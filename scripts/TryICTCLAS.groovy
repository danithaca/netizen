import java.io.File;

import magicstudio.netizen.util.SmartParser;

//println System.getProperties().getProperty('java.library.path')

def sp = new SmartParser()
//sp.loadUserDict()
def s = sp.splitTerms("你好, 北京人肉三聚氰胺是什么？周晓丹说阮安玲是好人21×13=buvidk")
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