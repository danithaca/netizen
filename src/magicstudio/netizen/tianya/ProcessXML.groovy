package magicstudio.netizen.tianya

import javax.xml.parsers.*
import org.ccil.cowan.tagsoup.Parser;


//XmlToTxt('D:\\Download\\xml-news5', 'D:\\Download\\txt-news5')
XmlToRaw('D:\\Download\\xml-news5', 'D:\\Download')

////////////////// functions //////////////

def XmlToRaw(srcPath, dstPath) {
	srcDir = new File(srcPath)
	threadFile = new File("${dstPath}\\thread.raw")
	postFile = new File("${dstPath}\\post.raw")
	//errFile = new File("${dstPath}\\_err.txt")
	
	def parser = new XmlParser(new Parser())
	// TagSoup parser doesn't work very well.
	//def parser = new XmlParser(new Parser())
	parser.setFeature('http://xml.org/sax/features/unicode-normalization-checking', false)
	parser.setFeature('http://xml.org/sax/features/validation', false)
	parser.setTrimWhitespace(true)
	
	count = 0
	for (xmlInput in srcDir.listFiles()) {	
		def index = null
		try {
			index = (xmlInput.getName() =~ /(\d+).xml$/)[0][1]
		} catch (Exception e) {
			println "Skip file ${xmlInput.getName()}"
			continue
		}
		println "Processing ${count++}: ${index}"
		def thread = parser.parse(xmlInput)
		
		def title = stripXML(thread.'@title')
		def firsttime = parseDate(thread.'@firsttime')
		def firstauthor = stripXML(thread.'@firstauthor')
		def visits = (s = thread.'@visits') ? s.toInteger() : 0
		def responses = (s = thread.'@responses') ? s.toInteger() : 0
		def samelinks = (s = thread.'@samelinks') ? s.split(',') : null
		if (samelinks) {println "Has links!!!"}
			
		for (post in thread.post) {
			def author = post.'@author'
			def time = post.'@time'
		}
	}
}

def XmlToTxt(srcPath, dstPath) {
	srcDir = new File(srcPath)
	//errFile = new File("${dstPath}\\_err.txt")
	count = 0
	for (xmlInput in srcDir.listFiles()) {
		println "Processing ${count++}"
		
		def index = null
		try {
			index = (xmlInput.getName() =~ /(\d+).xml$/)[0][1]
		} catch (Exception e) {
			println "Skip file ${xmlInput.getName()}"
			continue
		}
		
		try {
			def parser = new XmlParser()
			parser.setTrimWhitespace(true)
			def thread = parser.parse(xmlInput)
			
			def title = stripXML(thread.title.text())
			def firsttime = parseDate(thread.firsttime.text())
			
			txtOutput = new File("${dstPath}\\${firsttime.format('yyyyMMdd')}-${title.replaceAll(~/\p{Punct}/, '_')}.txt")
			txtOutput.withWriter { writer ->
				writer << "####### ${title}, ${index}" << '\n\n'
				
				for (post in thread.post) {
					writer << "####### ${stripXML(post.author.text())}, ${parseDate(post.time.text()).format('yyyy-MM-dd HH:mm:ss')}" << '\n'
					writer << stripXML(post.content.text()) << '\n\n'
				}
			}
		} catch (Exception e) {
			println "ERROR: ${index}"
			e.printStackTrace()
			//errFile.write(index+'\n')
		}
	}
}

def stripXML(str) {
	str = str.replaceAll(~/\s/, '')
	str = str.replaceAll("#NEXTLINE", "\n")
	str = str.replaceAll(~/[\n]+/, "\n")
	return str
}

def parseDate(str) {
	m = str =~ /(\d{2,4}-\d{1,2}-\d{1,2}).+?(\d{1,2}:\d{1,2}:\d{1,2})/
	return Date.parse('y-M-d H:m:s', "${m[0][1]} ${m[0][2]}")
}

/**
 * This method ensures that the output String has only
 * valid XML unicode characters as specified by the
 * XML 1.0 standard. For reference, please see
 * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
 * standard</a>. This method will return an empty
 * String if the input is null or empty.
 *
 * @param in The String whose non-valid characters we want to remove.
 * @return The in String, stripped of non-valid characters.
 */
String stripNonValidXMLCharacters(String input) {
    StringBuffer out = new StringBuffer(); // Used to hold the output.
    char current; // Used to reference the current character.

    if (input == null || ("".equals(input))) return ""; // vacancy test.
    for (int i = 0; i < input.length(); i++) {
        current = input.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
        if ((current == 0x9) ||
            (current == 0xA) ||
            (current == 0xD) ||
            ((current >= 0x20) && (current <= 0xD7FF)) ||
            ((current >= 0xE000) && (current <= 0xFFFD)) ||
            ((current >= 0x10000) && (current <= 0x10FFFF)))
            out.append(current);
    }
    return out.toString();
}
