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
	private double lr;
	private double lambda;
	
	public WindowModel(int _windowSize, int _hiddenSize, double _lr, double lambda){
		//TODO
		windowSize = _windowSize;
		wordSize = FeatureFactory.VEC_LEN;
		hiddenSize = _hiddenSize;
		windowVecSize = windowSize * wordSize;	
		lr = _lr;
		this.lambda = lambda;
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
		b1 = new SimpleMatrix(hiddenSize, 1);//SimpleMatrix.random(hiddenSize, 1, -epsilon_W, epsilon_W, rand);
		U = SimpleMatrix.random(outputSize, hiddenSize, -epsilon_U, epsilon_U, rand);
		b2 = new SimpleMatrix(outputSize, 1);//SimpleMatrix.random(outputSize, 1, -epsilon_U, epsilon_U, rand);
	}

	/**
	 * Simplest SGD training 
	 */
	public void train(List<Datum> _trainData ){
		//	TODO
		N = _trainData.size();
		buildWordVec(_trainData);
		//		
		for (int iter=0; iter<3; iter++){

			double cost = 0;
			for (int j=0; j<L.numCols(); j++){
				Datum trainWord = _trainData.get(j);
				SimpleMatrix oneHot = buildOneHot(outputSize, j);
				SimpleMatrix X = (WordVec.getWordVec(L.extractVector(false, j))).transpose();
				SimpleMatrix p = forwardProp(X);

				double cur_cost = valueAt(oneHot, X);
//							System.out.println("at data "+j+", the cost is "+ cur_cost);		
							if (Double.isNaN(cur_cost)){
								System.out.println("U = "+U.toString());
								System.out.println("W = "+W.toString());
								System.out.println("b1 = "+b1.toString());
								System.out.println("b2 = "+b2.toString());
								System.out.println("p = "+p.toString());
								System.exit(3);
							}
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
			
			Datum testWord = testData.get(j);
			SimpleMatrix X = (WordVec.getWordVec(L.extractVector(false, j))).transpose();
			SimpleMatrix  p = forwardProp(X);
//			if ( 1.0 - p.elementSum() > 0.0000001){
//				System.err.println("at loop "+j+" sum(p) "+p.elementSum()+" is wrong");
//				System.exit(4);
//			}
			String prediction = LABEL[VecOp.argmax(p)];
			System.out.println(testWord.word + "\t" + testWord.label + "\t" + prediction);
		}
	}
	
	
	public SimpleMatrix forwardProp(SimpleMatrix X){
//		System.out.println("W has numRows: "+W.numRows()+", numCols: "+W.numCols());
//		System.out.println("X has numRows: "+X.numRows()+", numCols: "+X.numCols());
//		System.out.println("b1 has numRows: "+b1.numRows()+", numCols: "+b1.numCols());
		
		Z = (W.mult(X)).plus(b1); //SimpleMatrix Z = addBias(W.mult(L), b1);
//		System.out.println("Z: "+Z.toString());
		H = VecOp.tanhVec(Z);
//		System.out.println("H: "+H.toString());
		SimpleMatrix theta = (U.mult(H)).plus(b2);//SimpleMatrix theta = addBias(U.mult(H), b2);
//		System.out.println("theta: "+theta.toString());
		SimpleMatrix p = VecOp.softmax(theta);
		return p;
	}
	
	public void backwardProp(SimpleMatrix X, int j, SimpleMatrix p){

		SimpleMatrix y = buildOneHot(outputSize,j);
		SimpleMatrix dcost_dtheta = p.minus(y);		
		SimpleMatrix dtheta_dh = U.transpose();//dtheta_dh = W2.T
		SimpleMatrix gradb2 = dcost_dtheta;//.scale(1.0/(double)N);//avgColwize(dcost_dtheta) ; //gradb2 = np.sum(dcost_dtheta,0)/N 
		
		
		SimpleMatrix gradU = (dcost_dtheta.mult(H.transpose()));//.scale(1.0/(double)N);////gradW2 = np.dot(h.T, dcost_dtheta)/N 
		
		
		SimpleMatrix prod = dtheta_dh.mult(dcost_dtheta);//		prod = np.dot(dcost_dtheta, dtheta_dh)//		SimpleMatrix tmp = new SimpleMatrix(prod.numRows(), prod.numCols());//		tmp = np.zeros_like(prod)
		SimpleMatrix diag = VecOp.tanhGradVec(Z); //the jth col of H// a col vec
		SimpleMatrix jacobian = SimpleMatrix.diag(VecOp.get1DColVecArr(diag));//jacobian = np.diag(sigmoid_grad(h[i]))
		SimpleMatrix tmp = ((prod.transpose()).mult(jacobian)).transpose();//		for (int i=0; i<tmp.numRows(); i++){tmp.set(i,j,tmp_j.get(0,i));} //tmp[i] = np.matrix(prod[i]).dot(jacobian)
		
		SimpleMatrix gradb1 = tmp;//.scale(1.0/(double)N);//avgColwize(tmp);//gradb1 = np.sum(tmp,0) /N
		SimpleMatrix gradW = (tmp.mult(X.transpose()));//.scale(1.0/(double)N);//.scale(1.0/(double)N);//gradW1 = np.dot(x.T,tmp)/N

		SimpleMatrix gradX = (W.transpose().mult(tmp));//.scale(1.0/(double)N);//.scale(1.0/(double)N);//gradW1 = np.dot(x.T,tmp)/N

		
//		double error = 0.0;
//		SimpleMatrix analytic = new SimpleMatrix(X.numRows(), X.numCols());
//		for (int i=0; i<X.numRows(); i++){
//			for (int k=0; k<X.numCols(); k++){
//				double prior = X.get(i,k);
//				X.set(i,k, prior+GradientCheck.eps);
//				double higher = valueAt(y,X);
//				X.set(i,k, prior-GradientCheck.eps);
//				double lower = valueAt(y,X);
//				X.set(i,k, prior);
//				double analytic_deriv = (higher-lower)/ (2.0 * GradientCheck.eps);
//				double componentError = gradX.get(i,k) - analytic_deriv;
//				error += (componentError * componentError);
//				analytic.set(i,k, analytic_deriv);
//				System.out.println("higher in matrix loop: "+(higher ));
//                System.out.println("lower in matrix loop: "+(lower));
//			}
//		}
//		System.out.println("err on x: "+error);
//		System.out.println("analytic_deriv: "+analytic.toString());
//      System.out.println("deriv: "+gradU.toString());
		
		
		
		
//		boolean check = GradientCheck.check(y, 
//				new ArrayList<SimpleMatrix>(Arrays.asList(X)), 
//				new ArrayList<SimpleMatrix>(Arrays.asList(gradX)), 
//				this);
//		System.out.println("on col "+j+", check: "+check);	

		
		U = U.minus(gradU.scale(lr));
		W = W.minus(gradW.scale(lr));
		b1 = b1.minus(gradb1.scale(lr));
		b2 = b2.minus(gradb2.scale(lr));
//		VecOp.senCol(L, X.minus(gradX.scale(lr)), j);
		//System.out.println("before: X["+j+"] = "+WordVec.getWordVec(L.extractVector(false, j)).toString());
		WordVec.updateWordVec(L.extractVector(false, j), X.minus(gradX.scale(lr)));
		//System.out.println("after: X["+j+"] = "+WordVec.getWordVec(L.extractVector(false, j)).toString());
		
		
	}
	

//	public double valueAt(SimpleMatrix label, SimpleMatrix input, SimpleMatrix p) {
//		// TODO Auto-generated method stub
//		return -label.transpose().dot(VecOp.logVec(p));// + (lambda / 2.0) * W.normF();
//	}
	
	@Override
	public double valueAt(SimpleMatrix label, SimpleMatrix input) {
		// TODO Auto-generated method stub
		SimpleMatrix p = forwardProp(input);
		return -label.transpose().dot(VecOp.logVec(p));
	}
	
	public double cost(){
		
		double cost = 0.0;
		for (int j=0; j<L.numCols(); j++){
			SimpleMatrix y = buildOneHot(outputSize, labels.get(j));
			SimpleMatrix X = (WordVec.getWordVec(L.extractVector(false, j))).transpose();
			SimpleMatrix p = forwardProp(X);
			cost += -y.transpose().dot(VecOp.logVec(p));
		}
		return cost;//label.transpose().dot(VecOp.logVec(p));
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
		labels = wordVec.getLabels();
	}
	
	public void forwardPrint(){


		System.out.println("data = "+L.toString());
		System.out.println("W = "+W.toString());
		System.out.println("b1 = "+b1.toString());
		System.out.println("W.mult(L) = "+W.mult(L).toString());
//		System.out.println("Z = "+Z.toString());
		System.out.println("H = "+H.toString());
		System.out.println("U = "+U.toString());
		System.out.println("b2 = "+b2.toString());
		System.out.println("U.mult(H) = "+U.mult(H).toString());
//		System.out.println("theta = "+theta.toString());
//		System.out.println("p = "+p.toString());
		
		System.out.println("W has numRows: "+W.numRows()+", numCols: "+W.numCols());
		System.out.println("L has numRows: "+L.numRows()+", numCols: "+L.numCols());
		System.out.println("b1 has numRows: "+b1.numRows()+", numCols: "+b1.numCols());
		System.out.println("U has numRows: "+U.numRows()+", numCols: "+U.numCols());
//		System.out.println("Z has numRows: "+Z.numRows()+", numCols: "+Z.numCols());
		System.out.println("H has numRows: "+H.numRows()+", numCols: "+H.numCols());
//		System.out.println("theta has numRows: "+theta.numRows()+", numCols: "+theta.numCols());
//		System.out.println("p has numRows: "+p.numRows()+", numCols: "+p.numCols());
//		for (int i=0; i<p.numRows(); i++){
//			System.out.print(p.get(i,0)+"+");
//		}
//		System.out.println();
	}
	
	public void backwardPrint(){
//		System.out.println("p = "+p.toString());
//		System.out.println("y = "+y.toString());
//		System.out.println("dtheta_dh = "+dtheta_dh.toString());
//		System.out.println("gradb2 = "+ gradb2.toString());
//		System.out.println("gradU = "+ gradU.toString());
//		System.out.println("prod = "+ prod.toString());
//		System.out.println("tmp = "+ tmp.toString());
//		System.out.println("gradb1 = "+ gradb1.toString());
//		System.out.println("gradW = "+ gradW.toString());
//		System.out.println("gradX = "+ gradX.toString());
//		
//		
//		System.out.println("dcost_dtheta has numRows: "+dcost_dtheta.numRows()+", numCols: "+dcost_dtheta.numCols());
//		System.out.println("dtheta_dh has numRows: "+dtheta_dh.numRows()+", numCols: "+dtheta_dh.numCols());
//		System.out.println("gradb2 has numRows: "+gradb2.numRows()+", numCols: "+gradb2.numCols());
//		System.out.println("gradU has numRows: "+gradU.numRows()+", numCols: "+gradU.numCols());
//		System.out.println("prod has numRows: "+prod.numRows()+", numCols: "+prod.numCols());
//		System.out.println("tmp has numRows: "+tmp.numRows()+", numCols: "+tmp.numCols());
//		System.out.println("gradb1 has numRows: "+gradb1.numRows()+", numCols: "+gradb1.numCols());
//		System.out.println("gradW has numRows: "+gradW.numRows()+", numCols: "+gradW.numCols());
//		System.out.println("gradX has numRows: "+gradX.numRows()+", numCols: "+gradX.numCols());

	}
	
	public double epsilon(int fanIn, int fanOut){
		return Math.sqrt(6.0) / Math.sqrt((double)(fanIn + fanOut));
	}
}
