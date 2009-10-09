package magicstudio.netizen.tianya
import magicstudio.netizen.util.*;
import org.apache.lucene.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*
import org.apache.lucene.analysis.cn.smart.*;


//XmlToRawTxt('C:\\Download\\news5-xml', 'C:\\Download', 'C:\\Download\\news5-txt')
//XmlToRawTxt('C:\\Download\\', 'C:\\Download', 'C:\\Download\\')
//ExtractKeywordsRaw('C:\\Download', 'C:\\Download')
//LuceneIndex('C:\\Download', 'C:\\Download\\lucene-news5')
LuceneSearch('C:\\Download\\lucene-news5', "tianya")

////////////////// functions //////////////

def LuceneSearch(lucenePath, termStr) {
	searcher = new IndexSearcher(FSDirectory.open(new File(lucenePath)), true)
	query = new TermQuery(new Term('text', termStr))
	hits = searcher.search(query, 1000000)
	//println hits.scoreDocs.length
	hits.scoreDocs.each { hit ->
		doc = searcher.doc(hit.doc)
		println doc.get('threadid')
		println DateTools.timeToString(doc.get('time').toLong(),  DateTools.Resolution.YEAR)
	}
}

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
			if (count%100 == 0) println "Processing ${count++}: ${index}"
			def thread = parser.parse(xmlInput)
			
			title = stripXML(thread.'@title')
			firsttime = parseDate(thread.'@firsttime')
			firstauthor = stripXML(thread.'@firstauthor')
			doc = GenerateLuceneDoc(index, firsttime, firstauthor, title, 1)
			indexWriter.addDocument(doc)
			
			for (post in thread.post) {
				def author = stripXML(post.'@author')
				def time = parseDate(post.'@time')
				def content = stripXML(post.text())
				doc = GenerateLuceneDoc(index, time, author, content, 0)
				indexWriter.addDocument(doc)
			}
		} catch (Exception e) {
			println "Error!"
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
		if (t.size()>1 && !(p in [18, 52])) {
			row = ["\"${t}\"", p, w, title, thread, time ]
			file.append(row.join('\t')+'\n')
		}
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
				def content = stripXML(post.text())
				row = [index, "\"${author}\"", "\"${time.format('yyyy-MM-dd HH:mm:ss')}\"", content.size()]
				postFile.append(row.join('\t')+'\n')
				txtFile.append('>>>>>>>>>> '+row[1..2].join('\t')+'\n\n')
				txtFile.append(content+'\n\n')
			}
		} catch (Exception e) {
			println "Error!"
			e.printStackTrace()
			errMsg += "${index}\n"
		}
	}
	if (errMsg.length>0) {
		errFile = new File("${rawPath}\\_err.txt")
		errFile.write(errMsg)
	}
}


def stripXML(str) {
	str = str.replaceAll(~/\s/, '')
	str = str.replace("#NEXTLINE", "\n")
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
