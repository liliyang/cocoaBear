package cs224n.deep;

import java.util.*;
import java.io.*;

import org.ejml.simple.SimpleMatrix;


public class NER {

	private static String BASELINE = "-baseline";
	public static void main(String[] args) throws IOException {
		// set parameters here
		int C = 5;
		int hiddenSize = 100;
		double learning = 0.005;	// alpha
		double regularize = 0.000025;	// lambda

		if (args.length < 2) {
			System.err.println("USAGE: java -cp classes NER ../data/train ../data/dev");
			return;
		}	    

		// this reads in the train and test datasets
		List<Datum> trainData = FeatureFactory.readTrainData(args[0], C);
		List<Datum> testData = FeatureFactory.readTestData(args[1], C);	

		//	read the train and test data
		//TODO: Implement this function (just reads in vocab and word vectors)
		FeatureFactory.initializeVocab("../data/vocab.txt");
		//SimpleMatrix allVecs= FeatureFactory.generateRandomWordVecs();
		SimpleMatrix allVecs= FeatureFactory.readWordVectors("../data/wordVectors.txt");
		FeatureFactory.sanityCheck();

		if ((args[args.length-1].toLowerCase()).equals(BASELINE)) {

			// Baseline here	
			BaselineModel base_model = new BaselineModel();
			base_model.train(trainData);
			base_model.test(testData);

		}	else{
			// initialize model 
			WindowModel model = new WindowModel(C, hiddenSize, learning, regularize);

			// train and test
			model.train(trainData);
			model.test(testData);
		}


	}
}