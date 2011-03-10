import java.io.File;

import magicstudio.netizen.util.SmartParser50;

//println System.getProperties().getProperty('java.library.path')

def sp = new SmartParser50()
sp.loadUserDict()
def s = sp.splitTerms("液态奶垄断资本你好, 北京人肉三聚氰胺是什么？周晓丹说阮安玲是好人21×13=buvidk湖北省")
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