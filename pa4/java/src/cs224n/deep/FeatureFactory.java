package cs224n.deep;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.ejml.simple.*;


public class FeatureFactory {


	public static int VEC_LEN = 50;
	public static int NUM_VOCAB;//100232
	private FeatureFactory() {

	}

	 
	static List<Datum> trainData;
	/** Do not modify this method **/
	public static List<Datum> readTrainData(String filename) throws IOException {
        if (trainData==null) trainData= read(filename);
        return trainData;
	}
	
	static List<Datum> testData;
	/** Do not modify this method **/
	public static List<Datum> readTestData(String filename) throws IOException {
        if (testData==null) testData= read(filename);
        return testData;
	}
	
	private static List<Datum> read(String filename)
			throws FileNotFoundException, IOException {
	    // TODO: you'd want to handle sentence boundaries
		List<Datum> data = new ArrayList<Datum>();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		Datum start = new Datum("<s>", "O");
		Datum finish = new Datum("</s>", "O");
		data.add(start);
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (line.trim().length() == 0) {
				data.add(finish);
				data.add(start);
				continue;
			}
			String[] bits = line.split("\\s+");
			String word = bits[0];
			String label = bits[1];

			Datum datum = new Datum(word, label);
			data.add(datum);
		}
		data.remove(2);
		data.remove(1);
		data.remove(0);
		data.remove(data.size()-1);
		return data;
	}
 
 
	// Look up table matrix with all word vectors as defined in lecture with dimensionality n x |V|
	static SimpleMatrix allVecs; //access it directly in WindowModel
	public static SimpleMatrix readWordVectors(String vecFilename) throws IOException {
		if (allVecs!=null) return allVecs;		
//		return null;
		//TODO implement this
		//set allVecs from filename	
//		List<List<Double>> data = new ArrayList<List<Double>>();
		double[][] data = new double[NUM_VOCAB][VEC_LEN];
		BufferedReader in = new BufferedReader(new FileReader(vecFilename));
	
		int ctr = 0;
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (line.trim().length() == 0) {
				continue;
			}
//			List<Double> row = new ArrayList<Double>();
			
			String[] rowStr = line.split("\\s+");
			//sanity check:
			if (rowStr.length != VEC_LEN){
				System.err.println("Sanity check failed: word vector has length other than 50.");
				System.exit(1);
			}
			for (int i=0; i<rowStr.length; i++){
				data[ctr][i] = (Double.parseDouble(rowStr[i]));
			}
			
			ctr++;
		}
		in.close();
		allVecs = new SimpleMatrix(data);
//		System.out.println("word vector has numCols: "+allVecs.numCols()+", numRows: "+allVecs.numRows());
		return allVecs;
	}
	// might be useful for word to number lookups, just access them directly in WindowModel
	public static HashMap<String, Integer> wordToNum = new HashMap<String, Integer>(); 
	public static HashMap<Integer, String> numToWord = new HashMap<Integer, String>();

	public static HashMap<String, Integer> initializeVocab(String vocabFilename) throws IOException {
		//TODO: create this
		BufferedReader in = new BufferedReader(new FileReader(vocabFilename));
		int ctr = 0;
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (line.trim().length() == 0) {
				continue;
			}
			
			wordToNum.put(line, ctr);
			numToWord.put(ctr, line);
			ctr++;
		}		
		in.close();
		NUM_VOCAB = ctr;
		return wordToNum;
	}
 
	public static void sanityCheck(){		
		if (wordToNum.size() != allVecs.numRows()){
			System.err.println("wordToNum has size "+wordToNum.size()+" while allVecs has numRows "+allVecs.numRows());
			System.exit(2);
		}
	}

}
