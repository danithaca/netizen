package magicstudio.netizen.util;

import ICTCLAS.I3S.AC.ICTCLAS30;

import java.util.*;
import java.io.*;

import magicstudio.netizen.util.SmartParser;

class Result {
    int start; //start position,词语在输入句子中的开始位置
    int length; //length,词语的长度
    //char sPOS[8];//词性

    int posId;//word type，词性ID值，可以快速的获取词性表
    int wordId; //如果是未登录词，设成0或者-1
    int word_type; //add by qp 2008.10.29 区分用户词典;1，是用户词典中的词；0，非用户词典中的词

    int weight;//add by qp 2008.11.17 word weight
};

public class TestICTCLAS30 {

    public static void main(String[] args) throws Exception {
        String sInput = " ICTCLAS在国内973专家组组织的评测中活动获得了第一名，在第一届国际中文??处理研究机构SigHan组织的评测中都获得了多项第一名。 ";
    	//String sInput = "--______________________________________";
        //分词
        //Split(sInput);

        //关键词提取
        //KeyExtract(sInput);

        //指纹提取
        //FingerPrint(sInput);

        SmartParser smartParser = new SmartParser();
        System.out.println(smartParser.splitRaw(sInput));
        for (SmartParser.Term term : smartParser.splitTerms(sInput)) {
        	System.out.println(term.toString());
        }
        for (SmartParser.Term term : smartParser.extractTerms(sInput)) {
        	System.out.println(term.getTerm() + term.getWeight());
        }
    }

    public static void Split(String sInput) {
        try {
            ICTCLAS30 testICTCLAS30 = new ICTCLAS30();

            String argu = "D:\\Work\\ictclas2009\\windows_JNI_32\\api";
            if (testICTCLAS30.ICTCLAS_Init(argu.getBytes("GB2312")) == false) {
                System.out.println("Init Fail!");
                return;
            }

            /*
            * 设置词性标注集
                   ID		    代表词性集
                   1			计算所一级标注集
                   0			计算所二级标注集
                   2			北大二级标注集
                   3			北大一级标注集
           */
            testICTCLAS30.ICTCLAS_SetPOSmap(1);

            //导入用户词典前
            byte nativeBytes[] = testICTCLAS30.ICTCLAS_ParagraphProcess(sInput.getBytes("GB2312"), 1);
            String nativeStr = new String(nativeBytes, 0, nativeBytes.length, "GB2312");

            System.out.println("未导入用户词典： " + nativeStr);

            //导入用户词典
            String sUserDict = argu+'\\'+"userdic.txt";
            int nCount = testICTCLAS30.ICTCLAS_ImportUserDict(sUserDict.getBytes("GB2312"));
            testICTCLAS30.ICTCLAS_SaveTheUsrDic();//保存用户词典
            System.out.println("导入个用户词： " + nCount);

            nativeBytes = testICTCLAS30.ICTCLAS_ParagraphProcess(sInput.getBytes("GB2312"), 1);
            nativeStr = new String(nativeBytes, 0, nativeBytes.length, "GB2312");

            System.out.println("导入用户词典后： " + nativeStr);

            //动态添加用户词
            String sWordUser = "973专家组组织的评测	ict";
            testICTCLAS30.ICTCLAS_AddUserWord(sWordUser.getBytes("GB2312"));
            testICTCLAS30.ICTCLAS_SaveTheUsrDic();//保存用户词典

            nativeBytes = testICTCLAS30.ICTCLAS_ParagraphProcess(sInput.getBytes("GB2312"), 1);
            nativeStr = new String(nativeBytes, 0, nativeBytes.length, "GB2312");
            System.out.println("动态添加用户词后: " + nativeStr);

            //文件分词
            String argu1 = "Test.txt";
            String argu2 = "Test_result.txt";
            testICTCLAS30.ICTCLAS_FileProcess(argu1.getBytes("GB2312"), argu2.getBytes("GB2312"), 1);


            //释放分词组件资源
            testICTCLAS30.ICTCLAS_Exit();
        }
        catch (Exception ex) {
        }

    }

    public static void KeyExtract(String sInput) {
        try {


            ICTCLAS30 testICTCLAS30 = new ICTCLAS30();

            String argu = "D:\\Work\\ictclas2009\\windows_JNI_32\\api";
            if (testICTCLAS30.ICTCLAS_Init(argu.getBytes("GB2312")) == false) {
                System.out.println("Init Fail!");
                return;
            }

            //分词高级接口
            byte nativeBytes[];
            nativeBytes = testICTCLAS30.nativeProcAPara(sInput.getBytes("GB2312"));

            int nativeElementSize = testICTCLAS30.ICTCLAS_GetElemLength(0);//size of result_t in native code
            int nElement = nativeBytes.length / nativeElementSize;

            byte nativeBytesTmp[] = new byte[nativeBytes.length];

            //关键词提取
            int nCountKey = testICTCLAS30.ICTCLAS_KeyWord(nativeBytesTmp, nElement);

            Result[] resultArr = new Result[nCountKey];
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nativeBytesTmp));

            int iSkipNum;
            for (int i = 0; i < nCountKey; i++) {
                resultArr[i] = new Result();
                resultArr[i].start = Integer.reverseBytes(dis.readInt());
                iSkipNum = testICTCLAS30.ICTCLAS_GetElemLength(1) - 4;
                if (iSkipNum > 0) {
                    dis.skipBytes(iSkipNum);
                }

                resultArr[i].length = Integer.reverseBytes(dis.readInt());
                iSkipNum = testICTCLAS30.ICTCLAS_GetElemLength(2) - 4;
                if (iSkipNum > 0) {
                    dis.skipBytes(iSkipNum);
                }

                dis.skipBytes(testICTCLAS30.ICTCLAS_GetElemLength(3));

                resultArr[i].posId = Integer.reverseBytes(dis.readInt());
                iSkipNum = testICTCLAS30.ICTCLAS_GetElemLength(4) - 4;
                if (iSkipNum > 0) {
                    dis.skipBytes(iSkipNum);
                }

                resultArr[i].wordId = Integer.reverseBytes(dis.readInt());
                iSkipNum = testICTCLAS30.ICTCLAS_GetElemLength(5) - 4;
                if (iSkipNum > 0) {
                    dis.skipBytes(iSkipNum);
                }

                resultArr[i].word_type = Integer.reverseBytes(dis.readInt());
                iSkipNum = testICTCLAS30.ICTCLAS_GetElemLength(6) - 4;
                if (iSkipNum > 0) {
                    dis.skipBytes(iSkipNum);
                }
                resultArr[i].weight = Integer.reverseBytes(dis.readInt());
                iSkipNum = testICTCLAS30.ICTCLAS_GetElemLength(7) - 4;
                if (iSkipNum > 0) {
                    dis.skipBytes(iSkipNum);
                }

            }

            dis.close();

            for (int i = 0; i < resultArr.length; i++) {
                System.out.println("start=" + resultArr[i].start + ",length=" + resultArr[i].length + "pos=" + resultArr[i].posId + "word=" + resultArr[i].wordId + "  weight=" + resultArr[i].weight);
            }

            //释放分词组件资源
            testICTCLAS30.ICTCLAS_Exit();
        }
        catch (Exception ex) {
        }

    }

    public static void FingerPrint(String sInput) {
        try {
            ICTCLAS30 testICTCLAS30 = new ICTCLAS30();

            String argu = ".";
            if (testICTCLAS30.ICTCLAS_Init(argu.getBytes("GB2312")) == false) {
                System.out.println("Init Fail!");
                return;
            }

            //分词高级接口
            testICTCLAS30.nativeProcAPara(sInput.getBytes("GB2312"));

            //指纹提取
            long iFinger = testICTCLAS30.ICTCLAS_FingerPrint();
            System.out.println("finger: " + iFinger);

            //释放分词组件资源
            testICTCLAS30.ICTCLAS_Exit();
        }
        catch (Exception ex) {
        }

    }

}
 
