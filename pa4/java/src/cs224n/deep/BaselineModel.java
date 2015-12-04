package cs224n.deep;

import java.util.HashMap;
import java.util.List;

import org.ejml.simple.SimpleMatrix;

public class BaselineModel {

	public BaselineModel() {

	}

	protected SimpleMatrix L, W, Wout;
	//
	public int windowSize,wordSize, hiddenSize;

	public BaselineModel(int _windowSize, int _hiddenSize, double _lr){

	}

	/**
	 * Initializes the weights randomly. 
	 */
	public void initWeights(){
		// Nothing to do here for baseline
	}


	/**
	 * Baseline - train by remembering all terms in train set
	 */
	private HashMap<String, String> trainData;
	public void train(List<Datum> _trainData ){

//		System.out.println("in baseline model");
		trainData = new HashMap<String, String>();
		for (Datum trainWord: _trainData){
			trainData.put(trainWord.word, trainWord.label);
		}
	}

	
	public void test(List<Datum> testData){
		// if word occurred in train set, mark the same. Else, mark as "O"
		for (Datum testWord: testData){
			String prediction = "O";
			if (trainData.containsKey(testWord.word)){
				prediction = trainData.get(testWord.word);
			}
			System.out.println(testWord.word + "\t" + testWord.label + "\t" + prediction);
		}
	}
}
