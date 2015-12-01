package cs224n.deep;
import java.lang.*;
import java.util.*;
import org.ejml.data.*;
import org.ejml.simple.*;
import java.text.*;

public class WindowModel implements ObjectiveFunction{

	protected SimpleMatrix L, W, U, b1, b2, H, Z; //Wout;
	public int windowSize,wordSize, hiddenSize, windowVecSize, N;
	
	public final int outputSize = 5; // K = 5, #class
	public final String[] LABEL = {"O","LOC",  "MISC","ORG", "PER"};
	private List<Integer> labels;
	private List<String> words;
	private double lr;
	private double lambda;
	
	public WindowModel(int _windowSize, int _hiddenSize, double _lr, double _lambda){
		//TODO
		windowSize = _windowSize;
		wordSize = FeatureFactory.VEC_LEN;
		hiddenSize = _hiddenSize;
		windowVecSize = windowSize * wordSize;	
		lr = _lr;
		lambda = _lambda;
		initWeights();
	}

	/**
	 * Initializes the weights randomly. 
	 */
	public void initWeights(){
		//TODO
		Random rand = new Random();
		double epsilon_W = epsilon(windowVecSize, hiddenSize);
		double epsilon_U = epsilon(hiddenSize, outputSize);

		// initialize with bias inside as the last column
	        // or separately
		W = SimpleMatrix.random(hiddenSize, windowVecSize, -epsilon_W, epsilon_W, rand);
		// U for the score
		b1 = SimpleMatrix.random(hiddenSize, 1, -epsilon_W, epsilon_W, rand);
		U = SimpleMatrix.random(outputSize, hiddenSize, -epsilon_U, epsilon_U, rand);
		b2 = SimpleMatrix.random(outputSize, 1, -epsilon_U, epsilon_U, rand);
	}

	/**
	 * Simplest SGD training 
	 */
	public void train(List<Datum> _trainData ){
		//	TODO
		N = _trainData.size();
		buildWordVec(_trainData);
		//		
		for (int iter=0; iter<20; iter++){

			double cost = 0;
			for (int j=0; j<L.numCols(); j++){
				Datum trainWord = _trainData.get(j);
				SimpleMatrix oneHot = buildOneHot(outputSize, j);
				//System.out.println("Word vec: " + L.extractVector(false, j).toString());
				SimpleMatrix X = (WordVec.getWordVec(L.extractVector(false, j))).transpose();

				SimpleMatrix p = forwardProp(X);

				double cur_cost = valueAt(oneHot, X);
//				System.out.println("at data "+j+", the cost is "+ cur_cost);		

				cost +=  cur_cost;
				backwardProp(X, j, p);
				
				//String prediction = LABEL[VecOp.argmax(p)];
				//System.out.println(trainWord.word + "\t" + trainWord.label + "\t" + prediction);
				
			}
			System.err.println("at iter "+iter+", the cost is "+ cost);		
		}
	}


	public void test(List<Datum> testData){
		// TODO
		N = testData.size();
		buildWordVec(testData);
		for (int j=0; j<L.numCols(); j++){
			SimpleMatrix X = (WordVec.getWordVec(L.extractVector(false, j))).transpose();
			SimpleMatrix  p = forwardProp(X);
			
			String prediction = LABEL[VecOp.argmax(p)];
			System.out.println(words.get(j) + "\t" + LABEL[labels.get(j)] + "\t" + prediction);
		}
	}
	
	
	public SimpleMatrix forwardProp(SimpleMatrix X){
		Z = (W.mult(X)).plus(b1); //SimpleMatrix Z = addBias(W.mult(L), b1);
		H = VecOp.tanhVec(Z);
		SimpleMatrix theta = (U.mult(H)).plus(b2);//SimpleMatrix theta = addBias(U.mult(H), b2);
		SimpleMatrix p = VecOp.softmax(theta);
		return p;
	}
	
	public void backwardProp(SimpleMatrix X, int j, SimpleMatrix p){
	
		SimpleMatrix y = buildOneHot(outputSize,j);
		SimpleMatrix dcost_dtheta = p.minus(y);		
		SimpleMatrix dtheta_dh = U.transpose();//dtheta_dh = W2.T
		SimpleMatrix gradb2 = dcost_dtheta;//.scale(1.0/(double)N);//avgColwize(dcost_dtheta) ; //gradb2 = np.sum(dcost_dtheta,0)/N 
		//gradb2 = gradb2.plus(b2.scale(lambda)); // regularization
		
		SimpleMatrix gradU = (dcost_dtheta.mult(H.transpose()));//.scale(1.0/(double)N);////gradW2 = np.dot(h.T, dcost_dtheta)/N 
		//gradU = gradU.plus(gradU.scale(lambda));
		
		SimpleMatrix prod = dtheta_dh.mult(dcost_dtheta);//		prod = np.dot(dcost_dtheta, dtheta_dh)//		SimpleMatrix tmp = new SimpleMatrix(prod.numRows(), prod.numCols());//		tmp = np.zeros_like(prod)
		SimpleMatrix diag = VecOp.tanhGradVec(Z); //the jth col of H// a col vec
		SimpleMatrix jacobian = SimpleMatrix.diag(VecOp.get1DColVecArr(diag));//jacobian = np.diag(sigmoid_grad(h[i]))
		SimpleMatrix tmp = ((prod.transpose()).mult(jacobian)).transpose();//		for (int i=0; i<tmp.numRows(); i++){tmp.set(i,j,tmp_j.get(0,i));} //tmp[i] = np.matrix(prod[i]).dot(jacobian)
		
		SimpleMatrix gradb1 = tmp;//.scale(1.0/(double)N);//avgColwize(tmp);//gradb1 = np.sum(tmp,0) /N
		//gradb1 = gradb1.plus(b1.scale(lambda));
		SimpleMatrix gradW = (tmp.mult(X.transpose()));//.scale(1.0/(double)N);//.scale(1.0/(double)N);//gradW1 = np.dot(x.T,tmp)/N
		//gradW = gradW.plus(gradW.scale(lambda));
		
		SimpleMatrix gradX = (W.transpose().mult(tmp));//.scale(1.0/(double)N);//.scale(1.0/(double)N);//gradW1 = np.dot(x.T,tmp)/N
		
		/*
		SimpleMatrix matr = W;
		SimpleMatrix deriv = gradW;
		double eps = 1e-4;
		double error = 0;
    SimpleMatrix analytic = new SimpleMatrix(matr.numRows(), matr.numCols());
    for (int c = 0; c < matr.numCols(); c++){
        for (int r = 0; r < matr.numRows(); r++){

            double prior = matr.get(r, c);

            matr.set(r, c, prior + eps);
            double higher = valueAt(y, X);
            double reghigher = regCost();
            matr.set(r, c, prior - eps);
            double lower = valueAt(y, X);
            double reglower = regCost();
            matr.set(r, c, prior);

            double analytic_deriv = (higher - lower) / (2.0 * eps);
            double reg_analytic_deriv = (reghigher - reglower) / (2.0 * eps);

            double componentError = deriv.get(r, c) - analytic_deriv;

            analytic.set(r,c,analytic_deriv);
            System.out.println("higher in matrix loop: "+(higher ));
            System.out.println("lower in matrix loop: "+(lower));
            System.out.println("analytic_deriv in matrix loop: "+analytic_deriv);
            System.out.println("deriv in matrix loop: "+(deriv.get(r, c)));
            System.out.println("reghigher in matrix loop: "+(reghigher ));
            System.out.println("reglower in matrix loop: "+(reglower));
            System.out.println("regderiv in matrix loop: "+((deriv.scale(lambda)).get(r, c)));
            System.out.println("reg_analytic_deriv in matrix loop: "+reg_analytic_deriv);
            //System.out.println("analytic_deriv in matrix loop: "+analytic_deriv);
            error += (componentError * componentError);
        }
    }
    System.out.println("error: "+error);
//    System.out.println("analytic_deriv: "+analytic.toString());
//    System.out.println("deriv: "+deriv.toString());
*/
    
		//boolean check = GradientCheck.check(y, 
		//		new ArrayList<SimpleMatrix>(Arrays.asList(W,X)), 
		//		new ArrayList<SimpleMatrix>(Arrays.asList(W.scale(lambda),gradX.scale(0))), 
		//		this);
		//if (!check) {
		//  System.out.println("on col "+j+", check: "+check);
		//}

		U = U.minus(gradU.scale(lr));
		W = W.minus(gradW.scale(lr));
		b1 = b1.minus(gradb1.scale(lr));
		b2 = b2.minus(gradb2.scale(lr));

		WordVec.updateWordVec(L.extractVector(false, j), X.minus(gradX.scale(lr)));
		
	}
	

//	public double valueAt(SimpleMatrix label, SimpleMatrix input, SimpleMatrix p) {
//		// TODO Auto-generated method stub
//		return -label.transpose().dot(VecOp.logVec(p));// + (lambda / 2.0) * W.normF();
//	}
	
	@Override
	public double valueAt(SimpleMatrix label, SimpleMatrix input) {
		SimpleMatrix p = forwardProp(input);
		double normW = W.normF();
		double normU = U.normF();
		double normb1 = b1.normF();
		double normb2 = b2.normF();
		//return (lambda/2)*(normW*normW + normU*normU + normb1*normb1 + normb2*normb2);
		return -label.transpose().dot(VecOp.logVec(p));
	}
	
	public double regCost() {
		double normW = W.normF();
		double normU = U.normF();
		double normb1 = b1.normF();
		double normb2 = b2.normF();
		return  (lambda/2)*(normW*normW + normU*normU + normb1*normb1 + normb2*normb2);
	}
	
	public double netCost(){
		double cost = 0.0;
		for (int j=0; j<L.numCols(); j++){
			SimpleMatrix y = buildOneHot(outputSize, labels.get(j));
			SimpleMatrix X = (WordVec.getWordVec(L.extractVector(false, j))).transpose();
			cost += valueAt(y,X);
		}
		return cost;
	}
	
	
	public SimpleMatrix buildOneHot(int outputSize, int j){
		SimpleMatrix oneHot = new SimpleMatrix(outputSize, 1);
		for (int i = 0; i< oneHot.numRows(); i++)   oneHot.set(i,0,0);
		oneHot.set(labels.get(j), 0, 1);
		return oneHot;
	}


	public void buildWordVec(List<Datum> data){
		WordVec wordVec = new WordVec(data, windowSize,windowVecSize, wordSize);
		L = wordVec.getL();
		//System.out.println("L = "+L.toString());
		labels = wordVec.getLabels();
		words = wordVec.getWords();
	}
	
	/*
	public void forwardPrint(){

		System.out.println("data = "+L.toString());
		System.out.println("W = "+W.toString());
		System.out.println("b1 = "+b1.toString());
		System.out.println("W.mult(L) = "+W.mult(L).toString());
		System.out.println("Z = "+Z.toString());
		System.out.println("H = "+H.toString());
		System.out.println("U = "+U.toString());
		System.out.println("b2 = "+b2.toString());
		System.out.println("U.mult(H) = "+U.mult(H).toString());
		System.out.println("theta = "+theta.toString());
		System.out.println("p = "+p.toString());
		
		System.out.println("W has numRows: "+W.numRows()+", numCols: "+W.numCols());
		System.out.println("L has numRows: "+L.numRows()+", numCols: "+L.numCols());
		System.out.println("b1 has numRows: "+b1.numRows()+", numCols: "+b1.numCols());
		System.out.println("U has numRows: "+U.numRows()+", numCols: "+U.numCols());
		System.out.println("Z has numRows: "+Z.numRows()+", numCols: "+Z.numCols());
		System.out.println("H has numRows: "+H.numRows()+", numCols: "+H.numCols());
		System.out.println("theta has numRows: "+theta.numRows()+", numCols: "+theta.numCols());
		System.out.println("p has numRows: "+p.numRows()+", numCols: "+p.numCols());
		for (int i=0; i<p.numRows(); i++){
			System.out.print(p.get(i,0)+"+");
		}
		System.out.println();
	}
	
	public void backwardPrint(){
		System.out.println("p = "+p.toString());
		System.out.println("y = "+y.toString());
		System.out.println("dtheta_dh = "+dtheta_dh.toString());
		System.out.println("gradb2 = "+ gradb2.toString());
		System.out.println("gradU = "+ gradU.toString());
		System.out.println("prod = "+ prod.toString());
		System.out.println("tmp = "+ tmp.toString());
		System.out.println("gradb1 = "+ gradb1.toString());
		System.out.println("gradW = "+ gradW.toString());
		System.out.println("gradX = "+ gradX.toString());
		
		
		System.out.println("dcost_dtheta has numRows: "+dcost_dtheta.numRows()+", numCols: "+dcost_dtheta.numCols());
		System.out.println("dtheta_dh has numRows: "+dtheta_dh.numRows()+", numCols: "+dtheta_dh.numCols());
		System.out.println("gradb2 has numRows: "+gradb2.numRows()+", numCols: "+gradb2.numCols());
		System.out.println("gradU has numRows: "+gradU.numRows()+", numCols: "+gradU.numCols());
		System.out.println("prod has numRows: "+prod.numRows()+", numCols: "+prod.numCols());
		System.out.println("tmp has numRows: "+tmp.numRows()+", numCols: "+tmp.numCols());
		System.out.println("gradb1 has numRows: "+gradb1.numRows()+", numCols: "+gradb1.numCols());
		System.out.println("gradW has numRows: "+gradW.numRows()+", numCols: "+gradW.numCols());
		System.out.println("gradX has numRows: "+gradX.numRows()+", numCols: "+gradX.numCols());

	}
	*/
	
	public double epsilon(int fanIn, int fanOut){
		return Math.sqrt(6.0) / Math.sqrt((double)(fanIn + fanOut));
	}
}
