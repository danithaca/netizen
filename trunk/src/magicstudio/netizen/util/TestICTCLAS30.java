package magicstudio.netizen.util;

import ICTCLAS.I3S.AC.ICTCLAS30;

import java.util.*;
import java.io.*;

import magicstudio.netizen.util.SmartParser;

class Result {
    int start; //start position,��������������еĿ�ʼλ��
    int length; //length,����ĳ���
    //char sPOS[8];//����

    int posId;//word type������IDֵ�����Կ��ٵĻ�ȡ���Ա�
    int wordId; //�����δ��¼�ʣ����0����-1
    int word_type; //add by qp 2008.10.29 �����û��ʵ�;1�����û��ʵ��еĴʣ�0�����û��ʵ��еĴ�

    int weight;//add by qp 2008.11.17 word weight
};

public class TestICTCLAS30 {

    public static void main(String[] args) throws Exception {
        String sInput = " ICTCLAS�ڹ���973ר������֯�������л����˵�һ�����ڵ�һ���������??�����о�����SigHan��֯�������ж�����˶����һ���� ";
    	//String sInput = "--______________________________________";
        //�ִ�
        //Split(sInput);

        //�ؼ�����ȡ
        //KeyExtract(sInput);

        //ָ����ȡ
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
            * ���ô��Ա�ע��
                   ID		    ������Լ�
                   1			������һ����ע��
                   0			������������ע��
                   2			���������ע��
                   3			����һ����ע��
           */
            testICTCLAS30.ICTCLAS_SetPOSmap(1);

            //�����û��ʵ�ǰ
            byte nativeBytes[] = testICTCLAS30.ICTCLAS_ParagraphProcess(sInput.getBytes("GB2312"), 1);
            String nativeStr = new String(nativeBytes, 0, nativeBytes.length, "GB2312");

            System.out.println("δ�����û��ʵ䣺 " + nativeStr);

            //�����û��ʵ�
            String sUserDict = argu+'\\'+"userdic.txt";
            int nCount = testICTCLAS30.ICTCLAS_ImportUserDict(sUserDict.getBytes("GB2312"));
            testICTCLAS30.ICTCLAS_SaveTheUsrDic();//�����û��ʵ�
            System.out.println("������û��ʣ� " + nCount);

            nativeBytes = testICTCLAS30.ICTCLAS_ParagraphProcess(sInput.getBytes("GB2312"), 1);
            nativeStr = new String(nativeBytes, 0, nativeBytes.length, "GB2312");

            System.out.println("�����û��ʵ�� " + nativeStr);

            //��̬����û���
            String sWordUser = "973ר������֯������	ict";
            testICTCLAS30.ICTCLAS_AddUserWord(sWordUser.getBytes("GB2312"));
            testICTCLAS30.ICTCLAS_SaveTheUsrDic();//�����û��ʵ�

            nativeBytes = testICTCLAS30.ICTCLAS_ParagraphProcess(sInput.getBytes("GB2312"), 1);
            nativeStr = new String(nativeBytes, 0, nativeBytes.length, "GB2312");
            System.out.println("��̬����û��ʺ�: " + nativeStr);

            //�ļ��ִ�
            String argu1 = "Test.txt";
            String argu2 = "Test_result.txt";
            testICTCLAS30.ICTCLAS_FileProcess(argu1.getBytes("GB2312"), argu2.getBytes("GB2312"), 1);


            //�ͷŷִ������Դ
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

            //�ִʸ߼��ӿ�
            byte nativeBytes[];
            nativeBytes = testICTCLAS30.nativeProcAPara(sInput.getBytes("GB2312"));

            int nativeElementSize = testICTCLAS30.ICTCLAS_GetElemLength(0);//size of result_t in native code
            int nElement = nativeBytes.length / nativeElementSize;

            byte nativeBytesTmp[] = new byte[nativeBytes.length];

            //�ؼ�����ȡ
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

            //�ͷŷִ������Դ
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

            //�ִʸ߼��ӿ�
            testICTCLAS30.nativeProcAPara(sInput.getBytes("GB2312"));

            //ָ����ȡ
            long iFinger = testICTCLAS30.ICTCLAS_FingerPrint();
            System.out.println("finger: " + iFinger);

            //�ͷŷִ������Դ
            testICTCLAS30.ICTCLAS_Exit();
        }
        catch (Exception ex) {
        }

    }

}
 
