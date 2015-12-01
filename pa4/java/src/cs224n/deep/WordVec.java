package cs224n.deep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ejml.simple.SimpleMatrix;

public class WordVec {

	private List<Datum> data;
	private static int windowSize, windowVecSize, wordSize;
	private List<Integer> labels;
	private SimpleMatrix L;

	private static final String NOT_MAPPED_WORD = "UUUNKKK";
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
//		List<double[]> windowVecList = new ArrayList<double[]>();
		List<List<Integer>> windowList = new ArrayList<List<Integer>>(); // list of 3-int array
		
		List<Datum> sentence = new ArrayList<Datum>();
		for (int i=0; i<data.size(); i++){
			Datum datum = data.get(i);
			sentence.add(datum);
			if (isSentenceEnd(datum)){				
				processSentence(sentence, windowList, labels);
				sentence = new ArrayList<Datum>();	
			}
		}
		
		int nWindow = windowList.size();
//		int[][] windowVecArr = new int[windowVecSize][nWindow];
		double[][] windowArr = new double[windowSize][nWindow];	
		for (int i = 0; i<nWindow; i++){
//			for (int j = 0; j<windowVecSize; j++){
//				windowVecArr[j][i] = windowVecList.get(i)[j];
//			}
			for (int j=0; j<windowSize; j++){
//				System.out.println(windowList.size()+" "+windowList.get(i).size());
				windowArr[j][i] = (double)windowList.get(i).get(j);
			}
		}

		L = new SimpleMatrix(windowArr);
//		System.out.println("L has numRows: "+L.numRows()+", numCols: "+L.numCols());
	}

	public void processSentence(List<Datum> sentence, List<List<Integer>> windowVecList, List<Integer> labels){
		if (sentence.size() < windowSize)
			return;
		
		for (int j=0; j<sentence.size()-(windowSize-1); j++){//per window
			List<Integer> window = new ArrayList<Integer>();
//			double[] windowVec = new double[windowVecSize];
			for (int k=j; k<windowSize+j; k++){
//				getWordVec(sentence.get(k), windowVec, k*wordSize);
				window.add(getWordIdx(sentence.get(k)));
			}
			windowVecList.add(window);
			labels.add(LABELMAP.get(sentence.get(j+windowSize/2).label));
		}
	}
	public int getWordIdx(Datum datum){
		Integer rowNum = FeatureFactory.wordToNum.get(datum.word);
		if (rowNum == null){
			rowNum = FeatureFactory.wordToNum.get(NOT_MAPPED_WORD);
		}
		return rowNum;
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


	
	public static SimpleMatrix getWordVec(SimpleMatrix rowNums){
//		Integer rowNum = FeatureFactory.wordToNum.get(datum.word);
//		if (rowNum == null){
//			rowNum = FeatureFactory.wordToNum.get(NOT_MAPPED_WORD);
//		}
		double[][] windowVec = new double[1][windowVecSize];
		int head = 0;
		for (int i=0; i<windowSize; i++){
			int rowNum = (int)rowNums.get(i,0);
			getRow(FeatureFactory.allVecs, rowNum, windowVec[0], head);
			head += wordSize;
		}
		return new SimpleMatrix(windowVec);
	}
	public static void getRow(SimpleMatrix matrix, int rowNum, double[] windowVec, int head){
		for (int col=0; col<matrix.numCols(); col++){
			windowVec[col+head] = matrix.get(rowNum, col);
		}
	}
	
	public static void updateWordVec(SimpleMatrix L, SimpleMatrix x){
		for (int i=0; i<L.numRows(); i++){
			for (int j=0; j<wordSize; j++){
				//System.out.println(FeatureFactory.allVecs.get((int)(L.get(i, 0)), j));
				//System.out.println(x.get(i*wordSize+j));
				FeatureFactory.allVecs.set((int)(L.get(i, 0)), j, x.get(i*wordSize+j));
				//System.out.println(FeatureFactory.allVecs.get((int)(L.get(i, 0)), j));
			}
		}
	}
}
