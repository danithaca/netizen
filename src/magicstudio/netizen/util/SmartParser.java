package magicstudio.netizen.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

import ICTCLAS.I3S.AC.ICTCLAS30;

public class SmartParser {
	
	static protected Charset defaultEncoding = Charset.forName("GBK");
	static protected String configPath = "D:\\Work\\ictclas2009\\windows_JNI_32\\api";
	// proxy design pattern
	protected ICTCLAS30 ictclasInstance;
	
	public SmartParser() {
		if (defaultEncoding == null) {
			// TODO: add reading system parameters.
			defaultEncoding = Charset.forName("GBK");
		}
		if (configPath == null) {
			// TODO: add reading system parameters.
			configPath = "D:\\Work\\ictclas2009\\windows_JNI_32\\api";
		}
		
		ictclasInstance = new ICTCLAS30();
		if (!ictclasInstance.ICTCLAS_Init(configPath.getBytes(defaultEncoding))) {
			throw new RuntimeException("Cannot initialize ICTCLAS30");
		}
		setPosMapping(1);
	}
	
	/*
     * ���ô��Ա�ע��
            ID		    ������Լ�
            1			������һ����ע��
            0			������������ע��
            2			���������ע��
            3			����һ����ע��
    */
	public void setPosMapping(int mapping) {
		if (ictclasInstance.ICTCLAS_SetPOSmap(mapping) == 0) {
			throw new RuntimeException("Cannot set POS map");
		}
	}
	
	public void loadUserDict(String dictName) {
		dictName = configPath + "\\" + dictName; // TODO: should be system dependent
		if (!(new File(dictName)).exists()) {
			throw new RuntimeException("Can't load user dictionary at "+dictName);
		}
		int count = ictclasInstance.ICTCLAS_ImportUserDict(dictName.getBytes(defaultEncoding));
		System.err.println("User dict entries: "+count);
		ictclasInstance.ICTCLAS_SaveTheUsrDic();
	}
	
	public void loadUserDict() {
		loadUserDict("userdict.txt");
	}
	
	public void exit() {
		boolean ok = ictclasInstance.ICTCLAS_Exit();
		if (!ok) {
			throw new RuntimeException("Exit ICTCLAS30 error.");
		}
	}
	
	/**
	 * Split a string, and return the splited string in a string format.
	 */
	public String splitRaw(String input) {
		byte[] result = ictclasInstance.ICTCLAS_ParagraphProcess(input.getBytes(defaultEncoding), 1);
		return new String(result, defaultEncoding);
	}
	
	static public class Term {
		String term; // the term
		String pos; //word type������IDֵ�����Կ��ٵĻ�ȡ���Ա�
		
	    int start; //start position,��������������еĿ�ʼλ��
	    int length; //length,����ĳ���

	    int posId;//word type������IDֵ�����Կ��ٵĻ�ȡ���Ա�
	    int wordId; //�����δ��¼�ʣ����0����-1
	    int word_type; //add by qp 2008.10.29 �����û��ʵ�;1�����û��ʵ��еĴʣ�0�����û��ʵ��еĴ�

	    int weight;//add by qp 2008.11.17 word weight
		
	    public Term() {};
	    
		public Term(String term, String pos) {
			this.term = term;
			this.pos = pos;
		}
		public String getTerm() { return term; }
		public String getPos() { return pos; }
		public int getStart() { return start; }
		public int getWordId() { return wordId; }
		public int getWeight() { return weight; }
		@Override
		public String toString() {
			return "<"+term+", "+pos+">";
		}
	}
	
	/**
	 * Split the string, and return a list of terms. Only the term and its type (pos) is returned.
	 * TODO: add code that populate "start", "length".
	 * We don't have the exact position, but we do have the order.
	 */
	public List<Term> splitTerms(String input) {
		List<Term> list = new ArrayList<Term>();
		Pattern pattern = Pattern.compile("(.+?)/([a-zA-Z]+?)\\s+?", Pattern.MULTILINE);
		String raw = splitRaw(input);
		Matcher matcher = pattern.matcher(raw);
		while (matcher.find()) {
			list.add(new Term(matcher.group(1), matcher.group(2)));
		}
		return list;
	}
	
	/**
	 * extract a list of keywords from the string
	 * doesn't follow the orders of appearance (?)
	 * including aggregated value such as "weight"
	 * we don't know the pos, only know posId
	 */
	public List<Term> extractTerms(String input) throws IOException {
		byte[] inputBytes = input.getBytes(defaultEncoding);
		
		//�ִʸ߼��ӿ�
        byte nativeBytes[];
        nativeBytes = ictclasInstance.nativeProcAPara(inputBytes);

        int nativeElementSize = ictclasInstance.ICTCLAS_GetElemLength(0);//size of result_t in native code
        int nElement = nativeBytes.length / nativeElementSize;

        byte nativeBytesTmp[] = new byte[nativeBytes.length];

        //�ؼ�����ȡ
        int nCountKey = ictclasInstance.ICTCLAS_KeyWord(nativeBytesTmp, nElement);

        Term[] resultArr = new Term[nCountKey];
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nativeBytesTmp));
        int iSkipNum;
        for (int i = 0; i < nCountKey; i++) {
            resultArr[i] = new Term();
            resultArr[i].start = Integer.reverseBytes(dis.readInt());
            iSkipNum = ictclasInstance.ICTCLAS_GetElemLength(1) - 4;
            if (iSkipNum > 0) {
                dis.skipBytes(iSkipNum);
            }

            resultArr[i].length = Integer.reverseBytes(dis.readInt());
            iSkipNum = ictclasInstance.ICTCLAS_GetElemLength(2) - 4;
            if (iSkipNum > 0) {
                dis.skipBytes(iSkipNum);
            }

            dis.skipBytes(ictclasInstance.ICTCLAS_GetElemLength(3));

            resultArr[i].posId = Integer.reverseBytes(dis.readInt());
            iSkipNum = ictclasInstance.ICTCLAS_GetElemLength(4) - 4;
            if (iSkipNum > 0) {
                dis.skipBytes(iSkipNum);
            }

            resultArr[i].wordId = Integer.reverseBytes(dis.readInt());
            iSkipNum = ictclasInstance.ICTCLAS_GetElemLength(5) - 4;
            if (iSkipNum > 0) {
                dis.skipBytes(iSkipNum);
            }

            resultArr[i].word_type = Integer.reverseBytes(dis.readInt());
            iSkipNum = ictclasInstance.ICTCLAS_GetElemLength(6) - 4;
            if (iSkipNum > 0) {
                dis.skipBytes(iSkipNum);
            }
            resultArr[i].weight = Integer.reverseBytes(dis.readInt());
            iSkipNum = ictclasInstance.ICTCLAS_GetElemLength(7) - 4;
            if (iSkipNum > 0) {
                dis.skipBytes(iSkipNum);
            }
            
            // add the term
            resultArr[i].term = new String(inputBytes, resultArr[i].start, resultArr[i].length, defaultEncoding);
            // TODO: add pos, now we only have posId.
            resultArr[i].pos = ""+resultArr[i].posId;
        }
        dis.close();
        return Arrays.asList(resultArr);
	}

}
