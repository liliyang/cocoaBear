package cs224n.deep;

import org.ejml.simple.SimpleMatrix;

public class VecOp {

	public static SimpleMatrix softmax(SimpleMatrix theta){
		// minus each entry by the max in its col			
		minus(theta, theta.elementMaxAbs());
		//exp on each entry
		expVec(theta);			
		theta = theta.divide(theta.elementSum());
		return theta;
	}

	public static void expVec(SimpleMatrix m){
		for (int i=0; i<m.numRows(); i++){//per row in each col
			m.set(i, Math.exp(m.get(i)));
		}
	}

	public static SimpleMatrix logVec(SimpleMatrix m){
		SimpleMatrix log = new SimpleMatrix(m.numRows(), m.numCols());
		for (int i=0; i<m.numRows(); i++){//per row in each col
			log.set(i,  Math.log(m.get(i)));
		}
		return log;
	}	
	public static void minus(SimpleMatrix v, double val){
		for (int i=0; i<v.numRows(); i++){//per row in each col
			v.set(i, 0, v.get(i,0)-val);
		}
	}	
	public static double tanh(double x){
		double e_x = Math.exp(x), e_negx = Math.exp(-x);
		return (e_x-e_negx) / (e_x+e_negx);
	}	
	public static double tanh_grad(double x){		
		return 1 - Math.pow(tanh(x), 2);
	}
	public static SimpleMatrix tanhVec(SimpleMatrix m){
		SimpleMatrix result = new SimpleMatrix(m.numRows(), m.numCols());
		for (int i = 0; i<m.numRows(); i++){
			result.set(i,  tanh(m.get(i)));
		}
		return result;
	}
	public static SimpleMatrix tanhGradVec(SimpleMatrix m){
		SimpleMatrix result = new SimpleMatrix(m.numRows(), m.numCols());
		for (int i = 0; i<m.numRows(); i++){
			result.set(i,  tanh_grad(m.get(i)));
		}
		return result;
	}
	
	public static double[] get1DColVecArr(SimpleMatrix m){
		double[] col = new double[m.numRows()];
		for (int i=0; i<col.length; i++){
			col[i] = m.get(i);
		}
		return col;
	}
	
	public static void senCol(SimpleMatrix m, SimpleMatrix col, int j){
		for (int i = 0; i<m.numRows(); i++){
			m.set(i,j, col.get(i));
		}
	}
	
	public static int argmax(SimpleMatrix m){
		int idx = 0;
		double cur = m.get(0);
		for (int i = 1; i<m.numRows(); i++){
			if (m.get(i)>cur){
				cur = m.get(i);
				idx = i;
			}
		}
		return idx;
	}
}
