# coding: utf8

import re

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
  
# return the synonym dictionary: dic[word]='typical word'
def generate_synonyms():
  input = open('dictionary.txt', 'r')
  terms = {}
  for line in input:
    line = line.strip()
    if line.startswith('#') or line=='': continue
    term_synonyms = line.split(',')
    if len(term_synonyms)<2: continue
    term_synonyms = [term]
    
    key_term = term_synonyms[0]
    del term_synonyms[0]
    for other_term in term_synonyms:
      if other_term.startswith('-'): other_term = other_term[1:]
      if other_term in terms:
        raise Exception("duplicate entry in dictionary. line: "+line)
      else:
        terms[other_term] = key_term
  input.close()
  return terms
      
# default definition for a simple node
class Node:
  separator = '\t'
  header = ['threadid', 'position', 'term', 'pos']
  
  def __init__(self, id):
    self.id = id
  
  def __cmp__(self, other):
    if self.id < other.id:
      return -1
    elif self.id > other.id:
      return 1
    else:
      return 0
  
  @staticmethod
  def verifyHeader(line):
    h = line.split(Node.separator)
    return h == Node.header
  
  @staticmethod
  def extractNode(line):
    node = Node(None) # empty node
    a1, a2, a3, a4 = line.split(Node.separator)
    node.threadid, node.position, node.term, node.pos  = int(a1), int(a2), a3, int(a4)
    node.id = node.term
    return node
  
  # test whether a term should be skipped.
  # only not skip nouns and user defined words (>99)
  def skippable(self):
    if self.term in ['/', '*']: return True
    if self.pos in range(21,33) or self.pos>99:
      if re.match('\W+', self.term, re.UNICODE): return True
      return False
    else:
      return True
  

class Edge:
  
  def __init__(self, node1, node2):
    self.node1 = node1
    self.node2 = node2
    self.id = (self.node1.id, self.node2.id)
    self.weight = 0
  
  def __cmp__(self, other):
    return self.id.__cmp__(other.id)
  
  def selfloop(self):
    return self.node1 == self.node2
  
  def skippable(self):
    # only remove the 1-timers
    if self.weight<2: return True
    else: return False
  
  def toString(self):
    return "%s,%s,%s" % (self.node1.id, self.node2.id, self.weight)


class DirectedEdge(Edge): pass

class UndirectedEdge(Edge):
  def __init__(self, node1, node2):
    if node1>node2:
      node1, node2 = node2, node1
    Edge.__init__(self, node1, node2)


# this is the super class for all text-based network.
class TextNetwork:
  
  nodeclass = Node
  edgeclass = UndirectedEdge
    
  # output csv file
  def outputCSV(self, dst):
    dst = open(dst, 'w')
    for key, edge in self.edges.items():
      if edge.skippable(): continue
      print >> dst, edge.toString()
    dst.close()
  
      
  def run(self, input, output):
    self.processTerms(input)
    self.outputCSV(output)
  
  # process from terms file generated from the corpus
  def processTerms(self, terms_file):
    terms_file = open(terms_file, 'r')
    header = terms_file.readline().strip()
    assert self.nodeclass.verifyHeader(header)
    
    self.current_threadid = None
    # a list of the edges we are interested in.
    self.edges = {}
    
    for line in terms_file:
      try:
        node = self.nodeclass.extractNode(line.strip())
      except:
        print "error line:", line
        continue
      if node.skippable(): continue
      
      if node.threadid != self.current_threadid:
        # start processing the new thread
        self.current_threadid = node.threadid
        self.window = []
        self.window.append(node)
      
      else:
        for i in xrange(len(self.window)-1, -1, -1):
          othernode = self.window[i]
          # window size is 50
          if node.position - othernode.position <= 50:
            edge = (self.edgeclass)(node, othernode)
            if edge.selfloop(): continue
            if edge.id in self.edges:
              edge = self.edges[edge.id]
              edge.weight += 1
            else:
              edge.weight = 1
              self.edges[edge.id] = edge
          else:
            break
        self.window.append(node)


# inherited from the basic textnetwork
class PeopleTextNetwork(TextNetwork): pass

class TianyaTextNetwork(TextNetwork): pass


if __name__ == '__main__':
  #network = TianyaTextNetwork()
  #network.processTerms('/data/data/ChinaMedia/tianya-news-5-network/termflesh.raw')
  #network.outputRelation('/data/data/ChinaMedia/tianya-news-5-network/flesh.csv')
  #generate_userdict()
  network = TianyaTextNetwork()
  network.run('../milk-tianya-terms.txt', '../milk.csv')
  
