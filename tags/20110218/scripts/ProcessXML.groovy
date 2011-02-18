import magicstudio.netizen.util.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;
import org.apache.lucene.store.*;
import org.apache.lucene.analysis.cn.smart.*;

println "Running scripts"
//XmlToRawTxt('data/tianya-news-c56-xml', './', 'data4tech/tianya-news-c56-txt')
//ExtractKeywordsRawByThread('C:\\Download\\news5-sanlu-xml', 'C:\\Download')
//LuceneIndex('tianya-news-c56-xml', 'lucene-part')
OutputTermPosition('/home/mrzhou/data/tianya-news-c56-txt', '/home/mrzhou/data/data4tech/tianyaterms.txt')

// generate terms for people
//OutputTermPosition('/home/mrzhou/data/people-3-txt-clean', '/home/mrzhou/data/peopleterms.txt')

////////////////// functions //////////////

def LuceneIndex(xmlPath, lucenePath) {
	xmlDir = new File(xmlPath);
    Analyzer analyzer = new SmartChineseAnalyzer();
    IndexWriter indexWriter = new IndexWriter(
    		FSDirectory.open(new File(lucenePath)), 
    		analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
	
	parser = new XmlParser()
	parser.setFeature('http://xml.org/sax/features/unicode-normalization-checking', false)
	parser.setFeature('http://xml.org/sax/features/validation', false)
	parser.setTrimWhitespace(true)

	count = 0
	for (xmlInput in xmlDir.listFiles()) {
		def index = null
		try {
			try {
				index = ((xmlInput.getName() =~ /(\d+).xml$/)[0][1]).toInteger()
			} catch (Exception e) {
				println "Skip file ${xmlInput.getName()}"
				continue
			}
			if (count%100 == 0) println "Processing lucene: ${count}"
            count++
			def thread = parser.parse(xmlInput)
			
			title = stripXML(thread.'@title')
			firsttime = parseDate(thread.'@firsttime')

			// hack: skip time
			//starttime = new Date().parse("yyyy-M-d", "2007-10-1")
			endtime = new Date().parse("yyyy-M-d", "2009-3-1")
			//if (firsttime.before(starttime) || firsttime.after(endtime)) { continue }
			if (firsttime.before(endtime)) {continue}
			
			firstauthor = stripXML(thread.'@firstauthor')
			doc = GenerateLuceneDoc(index, firsttime, firstauthor, title, 1)
			indexWriter.addDocument(doc)
			
			for (post in thread.post) {
				def author = stripXML(post.'@author')
				def time = parseDate(post.'@time')
				def content = stripXML(post.text())
				doc = GenerateLuceneDoc(index, time, author, content, 0)
				indexWriter.addDocument(doc)
				// hack: only include the first post
				break
			}
		} catch (Exception e) {
			println "Error! -- "+index
			e.printStackTrace()
		}
	}
    indexWriter.optimize();
    indexWriter.close();
}


def GenerateLuceneDoc(threadId, time, author, text, isTitle) {
    Document doc = new Document();
    doc.add(new NumericField("threadid", Field.Store.YES, false).setIntValue(threadId))
	doc.add(new NumericField("time", Field.Store.YES, true).setLongValue(time.getTime()))
	doc.add(new Field("author", author, Field.Store.NO, Field.Index.NOT_ANALYZED))
	doc.add(new Field("text", text, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES))
	doc.add(new NumericField("istitle", Field.Store.YES, true).setIntValue(isTitle))
	return doc
}


def OutputTermRow(file, terms, title, thread, time) {
	terms.each { term ->
		t = term.getTerm()
		p = term.getPos().toInteger()
		w = term.getWeight()
        /* 18 - numbers
           52 - time/date
           93 - english word
        */
		//if (w>1 && t.size()>1 && !(p in [18, 52, 93])) {
        //if (t.size()>1) {
			row = ["\"${t}\"", p, w, title, thread, time ]
			file.append(row.join('\t')+'\n')
		//}
	}
}

def ExtractKeywordsRaw(xmlPath, rawPath) {
	rowFields = ['term', 'pos', 'weight', 'title', 'thread', 'time']
	xmlDir = new File(xmlPath)
	rawFile = new File("${rawPath}\\term.raw")
	rawFile.write(rowFields.join('\t')+'\n')
	
	parser = new XmlParser()
	parser.setFeature('http://xml.org/sax/features/unicode-normalization-checking', false)
	parser.setFeature('http://xml.org/sax/features/validation', false)
	parser.setTrimWhitespace(true)
	
	analyzer = new SmartParser()
	
	count = 0
	//errFile = new File("${rawPath}\\term.err")
	//errFile.write("")
	
	for (xmlInput in xmlDir.listFiles()) {
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
			
			def samelinks = (s = thread.'@samelinks') ? s.split(',').collect({it.toInteger()}) : null
			if (samelinks!=null && samelinks.min()<index) {
				// since we'll see the main doc, just skip adding the thread data.
				index = samelinks.min() // then we just use the smallest post
			} else {
				// this is the main data.
				title = stripXML(thread.'@title')
				terms = analyzer.extractTerms(title)
				OutputTermRow(rawFile, terms, 1, index, '.')
			}

			for (post in thread.post) {
				time = parseDate(post.'@time')
				content = stripXML(post.text())
				terms = analyzer.extractTerms(content)
				OutputTermRow(rawFile, terms, 0, index, "\"${time.format('yyyy-MM-dd HH:mm:ss')}\"")
			}
		} catch (Exception e) {
			println "Error! ${index}"
			e.printStackTrace()
			//errFile.append(index.toString()+'\n')
		}
	}
}


def ExtractKeywordsRawByThread(xmlPath, rawPath) {
	rowFields = ['term', 'pos', 'weight', 'null1', 'thread', 'null2']
	xmlDir = new File(xmlPath)
	rawFile = new File("${rawPath}/term.raw")
    checkpoint = new Checkpoint()
    if (!checkpoint.isRecovered()) {
	    rawFile.write(rowFields.join('\t')+'\n')
    }

	parser = new XmlParser()
	parser.setFeature('http://xml.org/sax/features/unicode-normalization-checking', false)
	parser.setFeature('http://xml.org/sax/features/validation', false)
	parser.setTrimWhitespace(true)

	analyzer = new SmartParser()

	count = 0
	//errFile = new File("${rawPath}\\term.err")
	//errFile.write("")

    filesList = xmlDir.listFiles().sort()
	for (xmlInput in filesList) {
        // hack: restart ICTCLAS because experience shows it has problem after paring 2500 items.
        /*if (count%1000 == 0) {
          println "refreshing SmartParser at ${count}"
          analyzer.exit()
          analyzer = new SmartParser()
        }*/
        if (count%100 == 0) println "Processing extraction ${count}: ${xmlInput.getName()}"
        count++
      
		def index = null
		try {
			try {
				index = ((xmlInput.getName() =~ /(\d+).xml$/)[0][1]).toInteger()
                if (index.toString() in checkpoint.getItems()) {
                  //println "Skip processed file ${xmlInput.getName()}"
                  continue
                }
                // false file list, probably because of very long consecutive dashes
                /*if (index in [103231, 110814, 41266, 46042, 99207]) {
                    continue
                }*/
                if (index in [46042, 41266, 110814]) {
                    continue
                }
			} catch (Exception e) {
				println "Skip file ${xmlInput.getName()}"
				continue
			}
			def thread = parser.parse(xmlInput)
            content = ''

			def samelinks = (s = thread.'@samelinks') ? s.split(',').collect({it.toInteger()}) : null
			if (samelinks!=null && samelinks.min()<index) {
				// since we'll see the main doc, just skip adding the thread data.
				index = samelinks.min() // then we just use the smallest post
			} else {
				content = stripXML(thread.'@title')
			}
			for (post in thread.post) {
				content += stripXML(post.text())
			}
            // sometimes consequtive dashes will make the analyzer stop.
            content = content.replaceAll(~/-{2,}/, '--')
            //println content
            terms = analyzer.extractTerms(content)
			OutputTermRow(rawFile, terms, '', index, '')
            checkpoint.check(index)
		} catch (Exception e) {
			println "Error! -- ${index}"
			e.printStackTrace()
			//errFile.append(index.toString()+'\n')
		}
	}
}



def XmlToRawTxt(srcPath, rawPath, txtPath) {
	srcDir = new File(srcPath)
	//threadFile = new File("${rawPath}/thread.raw")
	//threadFile.write(['index', 'title', 'author', 'time', 'visits', 'responses'].join('\t')+'\n')
	//postFile = new File("${rawPath}/post.raw")
	//postFile.write(['index', 'author', 'time', 'size'].join('\t')+'\n')
	
	def parser = new XmlParser()
	// TagSoup parser doesn't work very well.
	//def parser = new XmlParser(new Parser())
	parser.setFeature('http://xml.org/sax/features/unicode-normalization-checking', false)
	parser.setFeature('http://xml.org/sax/features/validation', false)
	parser.setTrimWhitespace(true)
	
	count = 0
	for (xmlInput in srcDir.listFiles()) {
        count++
		def index = null
		try {
			try {
				index = ((xmlInput.getName() =~ /(\d+).xml$/)[0][1]).toInteger()
			} catch (Exception e) {
				println "Skip file ${xmlInput.getName()}"
				continue
			}
			if (count%200 == 1)println "Processing text/raw ${count}: ${index}"
			def thread = parser.parse(xmlInput)
			
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
				//row = [index, '"'+title.replace('"', '\'')+'"', "\"${firstauthor}\"", "\"${firsttime.format('yyyy-MM-dd HH:mm:ss')}\"", visits, responses].join('\t')
				///threadFile.append(row+'\n')
			}
			
			// create/reuse text file
			//def txtFile = new File("${txtPath}/${firsttime.format('yyyyMMdd')}-${title.replaceAll(~/\p{Punct}/, '')}.txt")
			def txtFile = new File("${txtPath}/${index}.txt", )
			txtFile.append(title+'\n\n')
				
			for (post in thread.post) {
				def author = stripXML(post.'@author')
				def time = parseDate(post.'@time')
				def content = stripXML(post.text())
				row = [index, "\"${author}\"", "\"${time.format('yyyy-MM-dd HH:mm:ss')}\"", content.size()]
				//postFile.append(row.join('\t')+'\n')
				//txtFile.append('>>>>>>>>>> '+row[1..2].join('\t')+'\r\n\r\n')
				//txtFile.append('* '*50)
				//txtFile.append(title+'\n\n')
				txtFile.append(content+'\r\n\r\n')
				txtFile.append('* '*50+'\r\n')
                // hack: only include the first post
                //break;
			}
		} catch (Exception e) {
			println "Error! ${index}"
			e.printStackTrace()
		}
	}
}


def stripXML(str) {
	str = str.replaceAll(~/\s/, '')  // remove all the whitespaces.
	str = str.replace("#NL", "\n")  // TODO: change it to #NL
	str = str.replaceAll(~/\n+/, "\n")
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


//read files from folder, output analyzed terms to a file.
// for future network processing
def OutputTermPosition(srcPath, outputFilename) {
	srcDir = new File(srcPath);
	output = new File(outputFilename)
	header = ['threadid', 'position', 'term', 'pos']
	output.append(header.join('\t')+'\n')
	analyzer = new SmartParser()
	analyzer.loadUserDict()
	count = 0
	
	srcDir.eachFile { file ->
		try {
			threadid = ((file.getName() =~ /(\d+).txt$/)[0][1]).toInteger()
		} catch (Exception e) {
			println "Skip file ${txtFile.getName()}"
			return
		}
		// for Tianya's data, it's GBK. for People's Daily, it's UTF8
		s = file.getText('utf8')
		ss = new StringBuilder()
		s.eachLine { line ->
			// remove the title bar line
			if (line.startsWith("* * * * * * * * * * >>>>>>>>>>")) {
				// in order to add position of 50. i.e., new post will be 50 words away from the previous post
				//line = "+ - "*25
				line = ""
			}
			ss.append(line).append('\n')
		}
		//println ss.toString()
		terms = analyzer.splitTerms(ss.toString())

		position = 0
		for (term in terms) {
			t = term.getTerm()
			//if (t=='*' || t=='-') continue;
			p = term.getPos()
			row = [threadid, position, t, p]
			output.append(row.join('\t')+'\n')
			position++;
		}
		println "Processing articles: ${count}"
		count++
	}
}
