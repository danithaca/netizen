import magicstudio.netizen.util.SmartParser;

//println System.getProperties().getProperty('java.library.path')

def sp = new SmartParser()
def s = sp.splitRaw("ÄãºÃ±±¾©")
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