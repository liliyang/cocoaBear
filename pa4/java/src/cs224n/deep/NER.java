package cs224n.deep;

import java.util.*;
import java.io.*;

import org.ejml.simple.SimpleMatrix;


public class NER {
    
    public static void main(String[] args) throws IOException {
	if (args.length < 2) {
	    System.out.println("USAGE: java -cp classes NER ../data/train ../data/dev");
	    return;
	}	    

	// this reads in the train and test datasets
	List<Datum> trainData = FeatureFactory.readTrainData(args[0]);
	List<Datum> testData = FeatureFactory.readTestData(args[1]);	
	
	//	read the train and test data
	//TODO: Implement this function (just reads in vocab and word vectors)
	FeatureFactory.initializeVocab("../data/vocab.txt");
	SimpleMatrix allVecs= FeatureFactory.readWordVectors("../data/wordVectors.txt");
	FeatureFactory.sanityCheck();

	// initialize model 
	WindowModel model = new WindowModel(3, 100,0.001, 0.1);
//	model.initWeights();
	model.train(trainData);
	model.test(testData);
	
//	BaselineModel base_model = new BaselineModel();
//	base_model.initWeights();
//
//	//TODO: Implement those two functions
//	base_model.train(trainData);
//	base_model.test(testData);
    }
}