import magicstudio.netizen.util.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;
import org.apache.lucene.store.*
import org.apache.lucene.analysis.cn.smart.*;

println "Running scripts"
//AddTxtToLucene('/data/data/ChinaMedia/people-3-txt-clean', '/data/data/ChinaMedia/people-3-lucene')
//OutputTermUsage('/data/data/ChinaMedia/people-3-txt-clean', '/data/data/ChinaMedia/people-3-network/allterms.raw')
//OutputTermUsageSeparate('/data/data/ChinaMedia/people-3-txt-clean', '/data/data/ChinaMedia/people-3-network/alltermss3.raw')
OutputTermPosition('/data/data/ChinaMedia/tianya-news-5-tiger-txt', '/data/data/ChinaMedia/tianya-news-5-network/termtiger.raw')

//////////////////////////////////////////////////


// stalling due to encoding problem
def AddTxtToLucene(srcPath, lucenePath) {
	srcDir = new File(srcPath);
	Analyzer analyzer = new SmartChineseAnalyzer();
	IndexWriter indexWriter = new IndexWriter(
			FSDirectory.open(new File(lucenePath)), 
			analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
	
	count = 0
	txtFileSet = srcDir.listFiles()
	println "Total file ${txtFileSet.size()}"
	for (txtFile in txtFileSet) {
		def threadid = null
		try {
			try {
				threadid = ((txtFile.getName() =~ /(\d+).txt$/)[0][1]).toInteger()
			} catch (Exception e) {
				println "Skip file ${txtFile.getName()}"
				continue
			}
			text = txtFile.getText('utf8')

			if (count%1000 == 0) println "Processing lucene: ${count}"
			count++
			
			Document doc = new Document();
			doc.add(new NumericField("threadid", Field.Store.YES, false).setIntValue(threadid))
			doc.add(new Field("text", text, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES))
			
			indexWriter.addDocument(doc)
			
		} catch (Exception e) {
			println "Error! -- "+index
			e.printStackTrace()
		}
	}
	indexWriter.optimize();
	indexWriter.close();
}

def OutputTermUsage(srcPath, outputFilename) {
	srcDir = new File(srcPath);
	output = new File(outputFilename)
	StringBuilder text = new StringBuilder()
	srcDir.eachFile { file ->
		s = file.getText('utf8')
		text.append(s)
	}
	println "Text size: ${text.size()}"
	analyzer = new SmartParser()
	terms = analyzer.extractTerms(text.toString())
	println "Terms: ${terms.size()}. Writing to file."
	terms.each {
		t = term.getTerm()
		p = term.getPos().toInteger()
		w = term.getWeight()
		row = [t, p, w]
		output.append(row.join('\t')+'\n')
	}
}

def OutputTermUsageSeparate(srcPath, outputFilename) {
	srcDir = new File(srcPath);
	output = new File(outputFilename)
	header = ['threadid', 'term', 'pos', 'weight']
	output.append(header.join('\t')+'\n')
	analyzer = new SmartParser()
	count = 0
	
	srcDir.eachFile { file ->
		try {
			threadid = ((file.getName() =~ /(\d+).txt$/)[0][1]).toInteger()
		} catch (Exception e) {
			println "Skip file ${txtFile.getName()}"
			return
		}
		s = file.getText('utf8')
		terms = analyzer.extractTerms(s)

		terms.each { term ->
			t = term.getTerm()
			p = term.getPos().toInteger()
			w = term.getWeight()
			row = [threadid, t, p, w]
			output.append(row.join('\t')+'\n')
		}
		if (count%1000 == 0) println "Processing articles: ${count}"
		count++
	}
}

// read files from folder, output analyzed terms to a file.
def OutputTermPosition(srcPath, outputFilename) {
	srcDir = new File(srcPath);
	output = new File(outputFilename)
	header = ['threadid', 'position', 'term', 'pos']
	output.append(header.join('\t')+'\n')
	analyzer = new SmartParser()
	count = 0
	
	srcDir.eachFile { file ->
		try {
			threadid = ((file.getName() =~ /(\d+).txt$/)[0][1]).toInteger()
		} catch (Exception e) {
			println "Skip file ${txtFile.getName()}"
			return
		}
		s = file.getText('gbk')
		terms = analyzer.splitTerms(s)

		position = 0
		for (term in terms) {
			t = term.getTerm()
			if (t=='*') continue;
			p = term.getPosId()
			row = [threadid, position, t, p]
			output.append(row.join('\t')+'\n')
			position++;
		}
		if (count%100 == 0) println "Processing articles: ${count}"
		count++
	}
}


// not finished
def CleanTxt(srcPath, dstPath) {
	srcDir = new File(srcPath)
	for (origTxt in srcDir.listFiles()) {
		
	}
}