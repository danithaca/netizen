package magicstudio.netizen.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

import ICTCLAS.I3S.AC.ICTCLAS30;

public class SmartParser {
	
	static protected Charset defaultEncoding;
	static protected String configPath;
	// proxy design pattern
	protected ICTCLAS30 ictclasInstance;
	static protected Map<Integer, String> posMapNumStr = new HashMap<Integer, String>();
	static protected Map<String, Integer> posMapStrNum = new HashMap<String, Integer>();
	
	public SmartParser() {
		if (defaultEncoding == null) {
			defaultEncoding = Charset.forName("GBK");
		}
		if (configPath == null) {
			configPath = System.getenv("ICTCLAS_HOME");
			//System.out.println(configPath);
		}
		
		ictclasInstance = new ICTCLAS30();
		if (!ictclasInstance.ICTCLAS_Init(configPath.getBytes(defaultEncoding))) {
			throw new RuntimeException("Cannot initialize ICTCLAS30");
		}
		setPosMapping(1, "pos_map.TXT");
	}
	
	/*
     * 设置词性标注集
            ID		    代表词性集
            1			计算所一级标注集
            0			计算所二级标注集
            2			北大二级标注集
            3			北大一级标注集
    */
	public void setPosMapping(int mapping, String mapName) {
		if (ictclasInstance.ICTCLAS_SetPOSmap(mapping) == 0) {
			throw new RuntimeException("Cannot set POS map");
		}
		mapName = configPath + File.separator + mapName;
		File mapFile = new File(mapName);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(mapFile));
			String line;
			while ((line=reader.readLine()) != null) {
				StringTokenizer tokens = new StringTokenizer(line);
				if (!tokens.hasMoreElements()) continue;
				int index = Integer.parseInt(tokens.nextToken());
				String name = "";
				if (tokens.hasMoreTokens()) {
					name = tokens.nextToken().toLowerCase();
				}
				posMapNumStr.put(index, name);
				posMapStrNum.put(name, index);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static public int posMap(String name) {
		Integer i = posMapStrNum.get(name.toLowerCase());
		if (i == null) throw new IllegalArgumentException("Can't find pos " + name);
		return i.intValue();
	}
	
	static public String posMap(int index) {
		String name = posMapNumStr.get(index);
		if (name == null) throw new IllegalArgumentException("Can't find pos at " + index);
		return name;
	}
	
	public void loadUserDict(String dictName) {
		dictName = configPath + File.pathSeparator + dictName;
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
		String pos; //word type，词性ID值，可以快速的获取词性表
		
	    int start; //start position,词语在输入句子中的开始位置
	    int length; //length,词语的长度

	    int posId;//word type，词性ID值，可以快速的获取词性表
	    int wordId; //如果是未登录词，设成0或者-1
	    int word_type; //add by qp 2008.10.29 区分用户词典;1，是用户词典中的词；0，非用户词典中的词

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
		public int getPosId() {
			if (posId != 0) {return posId;}
			else {
				posId = SmartParser.posMap(pos);
				return posId;
			}
		}
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
		
		//分词高级接口
        byte nativeBytes[];
        nativeBytes = ictclasInstance.nativeProcAPara(inputBytes);

        int nativeElementSize = ictclasInstance.ICTCLAS_GetElemLength(0);//size of result_t in native code
        int nElement = nativeBytes.length / nativeElementSize;

        byte nativeBytesTmp[] = new byte[nativeBytes.length];

        //关键词提取
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
            resultArr[i].pos = posMap(resultArr[i].posId);
        }
        dis.close();
        return Arrays.asList(resultArr);
	}

}
