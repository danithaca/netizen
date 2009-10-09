package magicstudio.netizen.tianya

import org.apache.lucene.search.*
import org.apache.lucene.index.*
import org.apache.lucene.store.*

//ids = LuceneSearchPhrase('C:\\Download\\news5-lucene', "ศýยน")
//println ids.size()
SearchAndCopy('C:\\Download\\news5-xml', 'C:\\Download\\news5-sanlu-xml')


///////////////////////// functions /////////////////////

def SearchAndCopy(srcPath, dstPath) {
    ids = []
    ids += LuceneSearchTerm('C:\\Download\\news5-lucene', "ศýยน")
    ids += LuceneSearchPhrase('C:\\Download\\news5-lucene', "ศýยน")
    ids += LuceneSearchTerm('C:\\Download\\news5-lucene', "ฤฬทÛ")
    ids += LuceneSearchPhrase('C:\\Download\\news5-lucene', "ฤฬทÛ")
    ids = ids.unique()
    println "Copy ${ids.size()} files"
    ant = new AntBuilder ()
    ids.each { index ->
        ant.copy(file:"${srcPath}\\${index}.xml", tofile:"${dstPath}\\${index}.xml")
    }
}

def LuceneSearchPhrase(lucenePath, phraseStr) {
    threadIds = []
	searcher = new IndexSearcher(FSDirectory.open(new File(lucenePath)), true)
    query = new PhraseQuery()
    phraseStr.each {
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