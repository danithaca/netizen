import org.apache.lucene.search.*
import org.apache.lucene.index.*
import org.apache.lucene.store.*

//ids = LuceneSearchTerm('tianya-news-c56-firstonly-lucene', "»ªÄÏ»¢")
//println ids.size()
SearchAndCopy('tianya-news-c56-xml', 'milk', 'lucene-part')


///////////////////////// functions /////////////////////

def SearchAndCopy(srcPath, dstPath, lucenePath) {
    ids = []
    ids += LuceneSearchTerm(lucenePath, "ÈýÂ¹")
    ids += LuceneSearchPhrase(lucenePath, "ÈýÂ¹")
    //ids += LuceneSearchTerm(lucenePath, "ÄÌ·Û")
    //ids += LuceneSearchPhrase(lucenePath, "ÄÌ·Û")
    ids = ids.unique()
    println "Copy ${ids.size()} files"
    ant = new AntBuilder ()
    ids.each { index ->
        try {
            ant.copy(file:"${srcPath}/${index}.xml", tofile:"${dstPath}/${index}.xml")
			//ant.copy(file:"${srcPath}/${index}.txt", tofile:"${dstPath}/${index}.txt")
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}

def LuceneSearchPhrase(lucenePath, phraseStr) {
    threadIds = []
	searcher = new IndexSearcher(FSDirectory.open(new File(lucenePath)), true)
    query = new PhraseQuery()
    phraseStr.each {
		println it
        query.add(new Term('text', it))
    }
	//query = new TermQuery(new Term('text', termStr))
	hits = searcher.search(query, 1000000)
	hits.scoreDocs.each { hit ->
		doc = searcher.doc(hit.doc)
		id = doc.get('threadid')
        if (!(id in threadIds)) {
            threadIds.push(id)
        }
	}
    println "For phrase ${phraseStr} -- hits ${hits.scoreDocs.length}, threads ${threadIds.size()}"
    return threadIds
}

def LuceneSearchTerm(lucenePath, termStr) {
    threadIds = []
	searcher = new IndexSearcher(FSDirectory.open(new File(lucenePath)), true)
	query = new TermQuery(new Term('text', termStr))
	hits = searcher.search(query, 1000000)
	hits.scoreDocs.each { hit ->
		doc = searcher.doc(hit.doc)
        id = doc.get('threadid')
        if (!(id in threadIds)) {
            threadIds.push(id)
        }
	}
    println "For term ${termStr} -- hits ${hits.scoreDocs.length}, threads ${threadIds.size()}"
    return threadIds
}