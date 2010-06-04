# coding: utf8
 
import re, sys

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
    # remove the |zz pos info
    term_synonyms = [t[0:t.rfind('|')] if t.rfind('|')!=-1 else t for t in term_synonyms]
    # remove the prefix '-' which preclude the term in the dictionary.
    term_synonyms = [t[1:] if t.startswith('-') else t for t in term_synonyms]

    key_term = term_synonyms[0]
    del term_synonyms[0]
    for other_term in term_synonyms:
      if other_term in terms:
        raise Exception("duplicate entry in dictionary. line: "+line)
      else:
        terms[other_term] = key_term
  input.close()
  return terms

# read the term file generated from groovy script
# return a list of term tuples
# check the terms to be in the format of [threadid  position  term  pos]
def read_term_file(file):
  header = ['threadid', 'position', 'term', 'pos']
  terms = []
  separator = '\t'
  infile = open(file, 'r')
  line = infile.readline().strip()
  assert header == line.split(separator)
  for line in infile:
    fields = line.strip().split(separator)
    # check for special cases
    if len(fields) != len(header):
      print line,
      continue
    terms.append(fields)
  infile.close()
  return terms
  

# aggregate terms data file and output some descriptive scrips
def aggregate_terms_usage(files, output):
  # term usage private class
  class TermUsage:
    def __init__(self):
      self.pos = set([])
      self.totaloccur = 0
      self.threadoccur = 0
    def __cmp__(self, other):
      a = cmp(self.threadoccur, other.threadoccur)
      if a != 0: return a
      b = cmp(self.totaloccur, other.totaloccur)
      if b != 0: return b
      c = cmp(len(self.term), len(other.term))
      if c != 0: return c
      return cmp(self.term, other.term)
    def __str__(self):
      pos = '/'.join(self.pos)
      return '\t'.join([self.term, pos, str(self.threadoccur), str(self.totaloccur)])
      
  term_usage_dict = {}
  curr_threadid = ''
  counted_terms = set([])
  for f in files:
    terms = read_term_file(f)
    for threadid, position, term, pos in terms:
      # if new thread, then do some stuff
      if threadid != curr_threadid:
        curr_threadid = threadid
        counted_terms = set([])
        
      if term not in term_usage_dict:
        usage = TermUsage()
        usage.term = term
      else:
        usage = term_usage_dict[term]
        assert term == usage.term
      usage.pos.add(pos)
      usage.totaloccur += 1
      # count thread occur
      if term not in counted_terms:
        usage.threadoccur += 1
        counted_terms.add(term)
      term_usage_dict[term] = usage

  # clean terms, based on the output from aggregate_terms_usage()
  def filter_terms(term):
    ######################## filter criteria ###################
    return term.threadoccur<=3 or term.totaloccur<=10

  header=['term', 'pos', 'threadoccur', 'totoaloccur']
  outfile = open(output, 'w')
  print >>outfile, '\t'.join(header)
  terms = term_usage_dict.values()
  terms.sort(reverse=True)
  for term in terms:
    if filter_terms(term): continue
    print >>outfile, term
  outfile.close()


def read_term_file_to_print(file):
  header = ['term','pos','threadoccur', 'totoaloccur']
  terms = []
  separator = '\t'
  infile = open(file, 'r')
  line = infile.readline().strip()
  assert header == line.split(separator)
  for line in infile:
    fields = line.strip().split(separator)
    print fields[0]
    terms.append(fields[0])
  infile.close()
  return terms




# default definition for a simple node
class Node:
  separator = '\t'
  header = ['threadid', 'position', 'term', 'pos']
  # initialize synonyms for the class
  synonyms = generate_synonyms()

  def __init__(self, id):
    self.id = id
  def __cmp__(self, other):
    return cmp(self.id, other.id)

  @staticmethod
  def verifyHeader(line):
    h = line.split(Node.separator)
    return h == Node.header

  # note: will take care of synonyms
  @staticmethod
  def extractNode(line):
    node = Node(None) # empty node
    fields = line.split(Node.separator)
    assert len(fields) == len(Node.header)
    a1, a2, a3, a4 = fields
    # sometimes a term leads by a space (GBK encoding), we got to remove those
    # note that only the term is used by Edge/Network now. position/pos/threadid are only used in the processTerms() function
    node.threadid, node.position, node.term, node.pos  = int(a1), int(a2), a3.strip().strip(ur"　"), a4
    # use the key_term if there's synonym available
    # TODO: verify there's no coding problem between UTF8 and GBK
    if node.term in Node.synonyms:
      node.term = Node.synonyms[node.term]
      # FIXME: this will arbitrarily set pos, which is not right.
      node.pos = 'zz'
    node.id = node.term
    return node

  def skippable(self):
    # don't skip anything
    return False


class UserDictNode(Node):
  # test whether a term should be skipped.
  # only not skip nouns and user defined words (>99)
  def skippable(self):
    if self.pos.startswith('zz'):
      #if re.match('\W+', self.term, re.UNICODE): return True
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

  nodeclass = UserDictNode
  edgeclass = UndirectedEdge

  # output csv file
  def outputCSV(self, dst):
    dst = open(dst, 'w')
    for key, edge in self.edges.items():
      if edge.skippable(): continue
      print >> dst, edge.toString()
    dst.close()

  def outputPajek(self, output):
    print "generating network file"
    nodes = []
    nodesindex = {}
    edges = []
    for key, edge in self.edges.items():
      if edge.skippable(): continue
      if edge.node1.id not in nodesindex:
        nodes.append(edge.node1)
        n1 = len(nodes)
        nodesindex[edge.node1.id] = n1
      else:
        n1 = nodesindex[edge.node1.id]
      if edge.node2.id not in nodesindex:
        nodes.append(edge.node2)
        n2 = len(nodes)
        nodesindex[edge.node2.id] = n2
      else:
        n2 = nodesindex[edge.node2.id]
      edges.append((n1, n2, edge.weight))

    count = 1
    output = open(output, 'w')
    print >>output, "*Vertices    ", len(nodes)
    for n in nodes:
      print >>output, count, '"'+n.term+'"'
      count += 1
    print >>output, "*Arcs"
    print >>output, "*Edges"
    for e in edges:
      print >>output, e[0], e[1], e[2]
    output.close()



  def run(self, input, output):
    self.processTerms(input)
    #self.outputCSV(output)
    self.outputPajek(output)

  # process from terms file generated from the corpus
  # this is to use the 50-words window method. override if needed.
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
            # we won't iterate thru the earlier terms in the window
            break
        self.window.append(node)
    
    # finish processing the lines in the term file. remove skippable edges
    removable = []
    for edge_id in self.edges:
      if self.edges[edge_id].skippable():
        removable.append(edge_id)
    for id in removable:
      del self.edges[id]


# inherited from the basic textnetwork
# undirected edge, remove edges w/ weight<2, only using UserDictionary.
class PeopleTextNetwork(TextNetwork): pass

class TianyaTextNetwork(TextNetwork):
  class SimplifiedUndirectedEdge(UndirectedEdge):
    def skippable(self):
      if self.weight<=5: return True
      else: return False
  edgeclass = SimplifiedUndirectedEdge


class TianyaFullNetwork(TextNetwork):
  class SimplifiedUndirectedEdge(UndirectedEdge):
    def skippable(self):
      if self.weight<=10: return True
      else: return False
  edgeclass = SimplifiedUndirectedEdge


if __name__ == '__main__':
  #network = PeopleTextNetwork()
  #network.run('../data/milkpeopleterms.txt', '../data/milkpeople.net')
  #generate_userdict()
  #network = TianyaTextNetwork()
  ##network.run('../tiger-tianya-terms.txt', '../tiger.net')
  l = generate_synonyms()
  for k,v in l.items(): print k,':',v
  

  # v2 is the simplified terms
  #aggregate_terms_usage(['../data/tigerpeopleterms.txt', '../data/milkpeopleterms.txt'], '../data/peopletermsusage.txt')
  #read_term_file_to_print('../data/termsusage_v3_together.txt')

  