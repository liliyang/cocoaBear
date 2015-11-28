package cs224n.deep;

import java.util.HashMap;
import java.util.List;

import org.ejml.simple.SimpleMatrix;

public class BaselineModel {

	public BaselineModel() {
		// TODO Auto-generated constructor stub
	}

	protected SimpleMatrix L, W, Wout;
	//
	public int windowSize,wordSize, hiddenSize;

	public BaselineModel(int _windowSize, int _hiddenSize, double _lr){
		//TODO
	}

	/**
	 * Initializes the weights randomly. 
	 */
	public void initWeights(){
		//TODO
		// initialize with bias inside as the last column
	        // or separately
		// W = SimpleMatrix...
		// U for the score
		// U = SimpleMatrix...
	}


	/**
	 * Simplest SGD training 
	 */
	private HashMap<String, String> trainData;
	public void train(List<Datum> _trainData ){
		//	TODO
		trainData = new HashMap<String, String>();
		for (Datum trainWord: _trainData){
			trainData.put(trainWord.word, trainWord.label);
		}
	}

	
	public void test(List<Datum> testData){
		// TODO
//		EU      ORG     ORG
//		System.out.println("EU" + "\t" + "ORG" + "\t" + "ORG");
		for (Datum testWord: testData){
			String prediction = "O";
			if (trainData.containsKey(testWord.word)){
				prediction = trainData.get(testWord.word);
			}
			System.out.println(testWord.word + "\t" + testWord.label + "\t" + prediction);
		}
	}
}
