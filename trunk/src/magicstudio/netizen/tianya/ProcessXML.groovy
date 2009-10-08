package magicstudio.netizen.tianya

XmlToRawTxt('C:\\Download\\', 'C:\\Download', 'C:\\Download\\')

////////////////// functions //////////////

def XmlToRawTxt(srcPath, rawPath, txtPath) {
	srcDir = new File(srcPath)
	threadFile = new File("${rawPath}\\thread.raw")
	threadFile.write(['index', 'title', 'author', 'time', 'visits', 'responses'].join('\t')+'\n')
	postFile = new File("${rawPath}\\post.raw")
	postFile.write(['index', 'author', 'time', 'size'].join('\t')+'\n')
	
	def parser = new XmlParser()
	// TagSoup parser doesn't work very well.
	//def parser = new XmlParser(new Parser())
	parser.setFeature('http://xml.org/sax/features/unicode-normalization-checking', false)
	parser.setFeature('http://xml.org/sax/features/validation', false)
	parser.setTrimWhitespace(true)
	
	count = 0
	errMsg = ''
	for (xmlInput in srcDir.listFiles()) {	
		def index = null
		try {
			try {
				index = ((xmlInput.getName() =~ /(\d+).xml$/)[0][1]).toInteger()
			} catch (Exception e) {
				println "Skip file ${xmlInput.getName()}"
				continue
			}
			println "Processing ${count++}: ${index}"
			def thread = parser.parse(xmlInput)
			parser.setTrimWhitespace(true)
			
			def title = stripXML(thread.'@title')
			def firsttime = parseDate(thread.'@firsttime')
			def firstauthor = stripXML(thread.'@firstauthor')
			def visits = (s = thread.'@visits') ? s.toInteger() : 0
			def responses = (s = thread.'@responses') ? s.toInteger() : 0
			def samelinks = (s = thread.'@samelinks') ? s.split(',').collect({it.toInteger()}) : null
			//if (samelinks) {println "Has links!!!"}
			if (samelinks!=null && samelinks.min()<index) {
				index = samelinks.min() // then we just use the smallest post
			} else {
				row = [index, '"'+title.replace('"', '\'')+'"', "\"${firstauthor}\"", "\"${firsttime.format('yyyy-MM-dd HH:mm:ss')}\"", visits, responses].join('\t')
				threadFile.append(row+'\n')
			}
			
			// create/reuse text file
			def txtFile = new File("${txtPath}\\${firsttime.format('yyyyMMdd')}-${title.replaceAll(~/\p{Punct}/, '')}.txt")
				
			for (post in thread.post) {
				def author = stripXML(post.'@author')
				def time = parseDate(post.'@time')
				def content = post.text()
				row = [index, "\"${author}\"", "\"${time.format('yyyy-MM-dd HH:mm:ss')}\"", content.size()]
				postFile.append(row.join('\t')+'\n')
				txtFile.append('>>>>>>>>>> '+row[1..2].join('\t')+'\n\n')
				txtFile.append(stripXML(content)+'\n\n')
			}
		} catch (Exception e) {
			println "Error!"
			e.printStackTrace()
			errMsg += "${index}\n"
		}
	}
	errFile = new File("${rawPath}\\_err.txt")
	errFile.write(errMsg)
}

def XmlToTxt(srcPath, dstPath) {
	srcDir = new File(srcPath)
	errFile = new File("${dstPath}\\_err.txt")
	errFile.write("Start logging.")
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
			errFile.append(index+'\n')
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
	pattern = ~/(\d{2,4}-\d{1,2}-\d{1,2}).+?(\d{1,2}:\d{1,2}:\d{1,2})/
	m = pattern.matcher(str)
	if (m.matches()) {
		return Date.parse('y-M-d H:m:s', "${m[0][1]} ${m[0][2]}")
	} else if (str ==~ /(\d{2,4}-\d{1,2}-\d{1,2})/) {
		return Date.parse('y-M-d', str)
	} else {
		throw new RuntimeException("Can't parse.")
	}
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
