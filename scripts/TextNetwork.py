# coding: utf8

# from the dictionary file, generate the userdict.txt file
def generate_userdict():
  input = open('dictionary.txt', 'r')
  output = open('userdict.txt', 'w')
  terms = set([])
  for line in input:
    line = line.strip()
    if line.startswith('#') or line=='': continue
    term_synonyms = line.split(',')
    for term_str in term_synonyms:
      if term_str.startswith('-'): continue
      term_part = term_str.split('|')
      if len(term_part)==1:
        term = (term_part[0], 'zz')
      elif len(term_part)==2:
        term = (term_part[0], term_part[1])
      else:
        raise Exception("dictionary format error")
      terms.add(term)
  for term, pos in terms:
    print >>output, term+'\t'+pos
  input.close()
  output.close()
      
    

# this is the super class for all text-based network.
class TextNetwork:
  
  def extractFileds(self, line):
    fields = line.strip().split('\t')
    return (int(fields[0]), int(fields[1]), fields[2], int(fields[3]))
  
  # test whether a term should be skipped.
  # only not skip nouns.
  def skippableTerm(self, term, pos):
    if term in ['/', '*']: return True
    
    if pos in range(21,33): return False
    else: return True
    
  def skippableRelation(self, value):
    if value<10: return True
    else: return False
    
  # update the relations between node
  def updateRelation(self, node1, node2, value):
    # maintain order for undirectional graph
    if node1>node2:
      key = (node2, node1)
    else:
      key = (node1, node2)
    self.relations[key] = self.relations.get(key, 0) + value
  
  # output csv file
  def outputRelation(self, dst):
    dst = open(dst, 'w')
    for key, value in self.relations.items():
      if self.skippableRelation(value): continue
      row = list(key)
      #row.append(str(value/2.0))
      row.append(str(value))
      print >> dst, ','.join(row)
      
    
  # process from terms file generated from the corpus
  def processTerms(self, terms_file):
    terms_file = open(terms_file, 'r')
    header = terms_file.readline()
    self.threadid = None
    # relations is a dic, key is (term, term), value is the strength
    self.relations = {}
    
    for line in terms_file:
      threadid, position, term, pos = self.extractFileds(line)
      node = (term, position)
      if self.skippableTerm(term, pos): continue
      
      if threadid != self.threadid:
        # start the new thread
        self.threadid = threadid
        self.window = []
        self.window.append(node)
      
      else:
        for i in xrange(len(self.window)-1, -1, -1):
          othernode = self.window[i]
          # window size is 50
          if node[1] - othernode[1] <= 50 and node[0] != othernode[0]:
            self.updateRelation(node[0], othernode[0], 1)
          else:
            break
        self.window.append(node)


# inherited from the basic textnetwork
class PeopleTextNetwork(TextNetwork): pass

class TianyaTextNetwork(TextNetwork):
  def skippableRelation(self, value):
    if value<15: return True
    else: return False


if __name__ == '__main__':
  #network = PeopleTextNetwork()
  #network.processTerms('/data/data/ChinaMedia/people-3-network/termtiger.raw')
  #network.outputRelation('/data/data/ChinaMedia/people-3-network/tiger.csv')
  #network = TianyaTextNetwork()
  #network.processTerms('/data/data/ChinaMedia/tianya-news-5-network/termflesh.raw')
  #network.outputRelation('/data/data/ChinaMedia/tianya-news-5-network/flesh.csv')
  generate_userdict()
  
  
