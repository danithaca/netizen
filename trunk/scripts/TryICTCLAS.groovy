import java.io.File;

import magicstudio.netizen.util.SmartParser50;

//println System.getProperties().getProperty('java.library.path')

def sp = new SmartParser50()
sp.loadUserDict()
def s = sp.splitTerms("Һ̬��¢���ʱ����, �������������谷��ʲô��������˵����Ǻ���21��13=buvidk����ʡ")
//def s = sp.splitRaw("Hello Beijing")
println s
////sp.loadUserDict();
//def t = sp.splitTerms("Hello Beijing\nHello Jinan\n");
//t.each {
//	println it
//}
//
//t = sp.extractTerms("a b c d e a a b");
//t.each {
//	println it
//}

sp.exit()