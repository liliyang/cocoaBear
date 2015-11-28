package cs224n.deep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ejml.simple.SimpleMatrix;

public class WordVec {

	private List<Datum> data;
	private int windowSize, windowVecSize, wordSize;
	private List<Integer> labels;
	private SimpleMatrix L;

	private final String NOT_MAPPED_WORD = "UUUNKKK";
	public final String O = "O", LOC = "LOC", MISC = "MISC", ORG = "ORG", PER = "PER";
	private HashMap<String, Integer> LABELMAP;
	
	public WordVec(List<Datum> data, int windowSize, int windowVecSize, int wordSize) {
		// TODO Auto-generated constructor stub
		this.data = data;
		this.windowSize = windowSize;
		this.windowVecSize = windowVecSize;
		this.wordSize = wordSize;
		labels = new ArrayList<Integer>();	
		
		LABELMAP = new HashMap<String, Integer>();
		LABELMAP.put(O, 0);
		LABELMAP.put(LOC, 1);
		LABELMAP.put(MISC, 2);
		LABELMAP.put(ORG, 3);
		LABELMAP.put(PER, 4);
		buildWordVec();
	}
	

	public void buildWordVec(){
		List<double[]> windowVecList = new ArrayList<double[]>();
		
		List<Datum> sentence = new ArrayList<Datum>();
		for (int i=0; i<data.size(); i++){
			Datum datum = data.get(i);
			sentence.add(datum);
			if (isSentenceEnd(datum)){				
				processSentence(sentence, windowVecList, labels);
				sentence = new ArrayList<Datum>();	
			}
		}
		
		int nWindow = windowVecList.size();
		double[][] windowVecArr = new double[windowVecSize][nWindow];	
		for (int i = 0; i<nWindow; i++){
			for (int j = 0; j<windowVecSize; j++){
				windowVecArr[j][i] = windowVecList.get(i)[j];
			}
		}

		L = new SimpleMatrix(windowVecArr);
//		System.out.println("L has numRows: "+L.numRows()+", numCols: "+L.numCols());
	}

	public void processSentence(List<Datum> sentence, List<double[]> windowVecList, List<Integer> labels){
		if (sentence.size() < windowSize)
			return;
		
		for (int j=0; j<sentence.size()-(windowSize-1); j++){//per window
			double[] windowVec = new double[windowVecSize];
			for (int k=j; k<windowSize; k++){
				getWordVec(sentence.get(k), windowVec, k*wordSize);
			}
			windowVecList.add(windowVec);
			labels.add(LABELMAP.get(sentence.get(j+windowSize/2).label));
		}
	}
	public void getWordVec(Datum datum, double[] windowVec, int head){
		Integer rowNum = FeatureFactory.wordToNum.get(datum.word);
		if (rowNum == null){
			rowNum = FeatureFactory.wordToNum.get(NOT_MAPPED_WORD);
		}
		getRow(FeatureFactory.allVecs, rowNum, windowVec, head);
	}
	public void getRow(SimpleMatrix matrix, int rowNum, double[] windowVec, int head){
		for (int col=0; col<matrix.numCols(); col++){
			windowVec[col+head] = matrix.get(rowNum, col);
		}
	}
	public boolean isSentenceStart(Datum datum){
		return datum.word.equals("<s>");
	}
	public boolean isSentenceEnd(Datum datum){
		return datum.word.equals("</s>");
	}
	public List<Integer> getLabels(){
		return labels;
	}
	public SimpleMatrix getL(){
		return L;
	}


}
