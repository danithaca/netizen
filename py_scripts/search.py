# import sys
# from org.apache.lucene.index import IndexReader
from org.apache.lucene.store import FSDirectory
from org.apache.lucene.search import IndexSearcher, TermQuery
from org.apache.lucene.index import Term

def test_lucene():
  searcher = IndexSearcher(FSDirectory(r'C:\Download\lucene-news5'), True)
  
def search(ipath, term_str):
  searcher = IndexSearcher(FSDirectory(ipath), True)
  query = TermQuery(Term('text', term_str))
  hits = searcher.search(query)
  print hits.length

if __name__ == '__main__':
  search(r'C:\Download\lucene-news5', u'风险')