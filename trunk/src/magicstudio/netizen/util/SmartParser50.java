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

import ICTCLAS.I3S.AC.ICTCLAS50;

public class SmartParser50 {
    public static final int GBK = 2; // eCodeType
    public static final int TAGGED = 1; // POSTTagged

    static protected Charset defaultEncoding;
    static protected String configPath;
    // proxy design pattern
    protected ICTCLAS50 ictclasInstance;
    protected Map<Integer, String> posMapNumStr = new HashMap<Integer, String>();
    protected Map<String, Integer> posMapStrNum = new HashMap<String, Integer>();

    public SmartParser50() {
        if (defaultEncoding == null) {
            defaultEncoding = Charset.forName("GBK");
        }
        if (configPath == null) {
            configPath = System.getenv("ICTCLAS_HOME");
            //System.out.println(configPath);
        }

        ictclasInstance = new ICTCLAS50();
        if (!ictclasInstance.ICTCLAS_Init(configPath.getBytes(defaultEncoding))) {
            throw new RuntimeException("Cannot initialize ICTCLAS50");
        }
        setPosMapping(0, "pos_map.TXT");
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
            while ((line = reader.readLine()) != null) {
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

    public int posMap(String name) {
        Integer i = posMapStrNum.get(name.toLowerCase());
        if (i == null) throw new IllegalArgumentException("Can't find pos " + name);
        return i.intValue();
    }

    public String posMap(int index) {
        String name = posMapNumStr.get(index);
        if (name == null) throw new IllegalArgumentException("Can't find pos at " + index);
        return name;
    }

    public void loadUserDict(String dictName) {
        dictName = configPath + File.separator + dictName;
        if (!(new File(dictName)).exists()) {
            throw new RuntimeException("Can't load user dictionary at " + dictName);
        }
        // user dict file GBK ecode: 2
        int count = ictclasInstance.ICTCLAS_ImportUserDictFile(dictName.getBytes(defaultEncoding), GBK);
        System.err.println("User dict entries: " + count);
        ictclasInstance.ICTCLAS_SaveTheUsrDic();
    }

    public void loadUserDict() {
        loadUserDict("userdict.txt");
    }

    public void exit() {
        boolean ok = ictclasInstance.ICTCLAS_Exit();
        if (!ok) {
            throw new RuntimeException("Exit ICTCLAS50 error.");
        }
    }

    /**
     * Split a string, and return the splited string in a string format.
     */
    public String splitRaw(String input) {
        byte[] result = ictclasInstance.ICTCLAS_ParagraphProcess(input.getBytes(defaultEncoding), GBK, TAGGED);
        return new String(result, defaultEncoding);
    }

    public class Term {
        String term; // the term
        String pos; //word type，词性ID值，可以快速的获取词性表

        int start; //start position,词语在输入句子中的开始位置
        int length; //length,词语的长度

        int posId;//word type，词性ID值，可以快速的获取词性表
        int wordId; //如果是未登录词，设成0或者-1
        int word_type; //add by qp 2008.10.29 区分用户词典;1，是用户词典中的词；0，非用户词典中的词

        int weight;//add by qp 2008.11.17 word weight

        public Term() {}

        public Term(String term, String pos) {
            this.term = term;
            this.pos = pos;
        }

        public String getTerm() {
            return term;
        }

        public String getPos() {
            return pos;
        }

        public int getStart() {
            return start;
        }

        public int getWordId() {
            return wordId;
        }

        public int getWeight() {
            return weight;
        }

        public int getPosId() {
            if (posId != 0) {
                return posId;
            } else {
                posId = posMap(pos);
                return posId;
            }
        }

        @Override
        public String toString() {
            return "<" + term + ", " + pos + "|" + getPosId() + ">";
        }
    }

    /**
     * Split the string, and return a list of terms. Only the term and its type (pos) is returned.
     * TODO: add code that populate "start", "length".
     * We don't have the exact position, but we do have the order.
     */
    public List<Term> splitTerms(String input) {
        List<Term> list = new ArrayList<Term>();
        Pattern pattern = Pattern.compile("(.+?)/([a-zA-Z0-9]+?)\\s+?", Pattern.MULTILINE);
        String raw = splitRaw(input);
        Matcher matcher = pattern.matcher(raw);
        while (matcher.find()) {
            list.add(new Term(matcher.group(1), matcher.group(2)));
        }
        return list;
    }

    // internal class to be used with nativeProcAPara
    static public class stResult {
        int start; //start position,词语在输入句子中的开始位置
        int length; //length,词语的长度
        int iPOS; //POS,词性ID
        String sPOS;//word type词性
        int word_ID; //word_ID,词语ID
        int word_type; //Is the word of the user's dictionary?(0-no,1-yes)查看词语是否为用户字典中词语
        int weight;// word weight,词语权重

        public void setStart(int start) {
            this.start = start;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public void setiPOS(int iPOS) {
            this.iPOS = iPOS;
        }

        public void setsPOS(String sPOS) {
            this.sPOS = sPOS;
        }

        public void setWord_ID(int word_ID) {
            this.word_ID = word_ID;
        }

        public void setWord_type(int word_type) {
            this.word_type = word_type;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }


    /**
     * extract a list of keywords from the string
     * doesn't follow the orders of appearance (?)
     * including aggregated value such as "weight"
     * we don't know the pos, only know posId
     *
     * TODO: untested!
     */
    public List<stResult> extractTerms(String input) throws IOException {
        byte[] inputBytes = input.getBytes(defaultEncoding);
        ArrayList<stResult> al = new ArrayList<stResult>();

        //分词高级接口
        byte nativeBytes[];
        nativeBytes = ictclasInstance.nativeProcAPara(inputBytes, GBK, TAGGED);

        //处理结果转化
        for (int i = 0; i < nativeBytes.length; i++) {
            //获取词语在输入句子中的开始位置
            byte a[] = Arrays.copyOfRange(nativeBytes, i, i + 4);
            i += 4;
            int start = byteToInt2(a);
            start = Integer.reverseBytes(start);
            System.out.print(" " + start);
            //获取词语的长度
            byte b[] = Arrays.copyOfRange(nativeBytes, i, i + 4);
            i += 4;
            int length = byteToInt2(b);
            length = Integer.reverseBytes(length);
            System.out.print(" " + length);
            //获取词性ID
            byte c[] = Arrays.copyOfRange(nativeBytes, i, i + 4);
            i += 4;
            int iPOS = byteToInt2(c);
            iPOS = Integer.reverseBytes(iPOS);
            System.out.print(" " + iPOS);
            //获取词性
            byte s[] = Arrays.copyOfRange(nativeBytes, i, i + 8);
            i += 8;
            String sPOS = new String(s);
            System.out.print(" " + sPOS);
            //获取词语ID
            byte j[] = Arrays.copyOfRange(nativeBytes, i, i + 4);
            i += 4;
            int word_ID = byteToInt2(j);
            word_ID = Integer.reverseBytes(word_ID);
            System.out.print(" " + word_ID);
            //获取词语类型，查看是否是用户字典
            byte k[] = Arrays.copyOfRange(nativeBytes, i, i + 4);
            i += 4;
            int word_type = byteToInt2(k);
            word_type = Integer.reverseBytes(word_type);
            System.out.print(" " + word_type);
            //获取词语权重
            byte w[] = Arrays.copyOfRange(nativeBytes, i, i + 4);
            i += 4;
            int weight = byteToInt2(w);
            weight = Integer.reverseBytes(weight);
            System.out.print(" " + weight);
            //将处理结果赋值给结构体
            stResult stR = new stResult();
            stR.setStart(start);
            stR.setLength(length);
            stR.setiPOS(iPOS);
            stR.setsPOS(sPOS);
            stR.setWord_ID(word_ID);
            stR.setWord_type(word_type);
            stR.setWeight(weight);
            al.add(stR);
        }
        return al;
    }

    /**
     * 将byte数组转换为int数据
     *
     * @param b 字节数组
     * @return 生成的int数据
     */
    public static int byteToInt2(byte[] b) {
        return (((int) b[0]) << 24) + (((int) b[1]) << 16) + (((int) b[2]) << 8) + b[3];
    }

}
