# this is the new script for study after 2011-2

import re, os, tempfile, codecs, traceback
from java.io import *
from java.nio import *
from magicstudio.netizen.util import SmartParser50, SmartParser
from mypytools import read_csv


term_file_header = ['threadid', 'position', 'term', 'pos']

def generate_synonyms():
  input = codecs.open('dictionary.txt', 'r', 'utf8')
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


class Node:
  # initialize synonyms for the class
  synonyms = generate_synonyms()

  def __init__(self, id):
    self.id = id
  def __cmp__(self, other):
    return cmp(self.id, other.id)

  # note: will take care of synonyms
  def extractLine(self, line):
    a1, a2, a3, a4 = line.split('\t')
    # sometimes a term leads by a space (GBK encoding), we got to remove those
    # note that only the term is used by Edge/Network now. position/pos/threadid are only used in the processTerms() function
    self.threadid, self.position, self.term, self.pos  = int(a1), int(a2), a3.strip().strip('\xa1\xa1'), a4
    # use the key_term if there's synonym available
    # TODO: verify there's no coding problem between UTF8 and GBK
    if self.term in self.synonyms:
      self.term = self.synonyms[self.term]
      # FIXME: this will arbitrarily set pos, which is not right.
      self.pos = 'zz'
    self.id = self.term


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



class ChinaStudy(object):
  input_file_encoding = 'utf8'
  output_file_encoding = 'utf8'
  nodeclass = Node
  edgeclass = Edge
  

  def config(self):
    assert False, "Please override"
    # self.src_txt_dir

  def __init__(self):
    #self.term_pos_file =
    self.config()
    self.analyzer = SmartParser50()
    # use the old parser
    #self.analyzer = SmartParser()
    self.analyzer.loadUserDict()

  def output_term_pos(self):
    file_list = os.listdir(self.src_txt_dir)
    file_list = [self.src_txt_dir+'/'+f for f in file_list]
    n, term_file = tempfile.mkstemp(prefix='', suffix='.t')
    print "Output terms to:", term_file
    self.term_file = term_file
    self._output_term_pos(file_list, term_file)

  # written for Jython. since jython2.5.2rc4 doesn't support gbk codec, we use Java files.
  def _output_term_pos(self, file_list, term_file):
    header = self.term_file_header
    out = OutputStreamWriter(FileOutputStream(term_file), self.output_file_encoding)
    out.write('\t'.join(header)+'\n')
    buf = CharBuffer.allocate(50000000) # 50M
    for f in file_list:
      #print "Processing file:", f
      try:
        threadid = re.search(r'(\d+)[.]txt', f).group(1)
      except:
        traceback.print_exc()
        print "Skip file:", f
        continue
      fi = InputStreamReader(FileInputStream(f), self.input_file_encoding)
      buf.clear()
      fi.read(buf)
      terms = self.analyzer.splitTerms(buf.toString())

      position = 0
      for term in terms:
        t = term.getTerm()
        if t=='*': continue
        p = term.getPosId()
        row = [threadid, str(position), t, str(p)]
        out.write('\t'.join(row)+'\n')
        position += 1
    out.close()


  def process_term_file(self, term_file):
    term_file = self.term_file
    print "processing terms from term file:", term_file
    term_file = codecs.open(term_file, 'r', self.output_file_encoding)
    header = term_file.readline().strip()
    
    current_threadid = None
    window = []
    # using edgefile as temporary storage for the edges.
    edgeout = open(self.edgefile, 'w')
    
    count = 0
    COUNTALERT=10000
    for line in term_file:
      if count%COUNTALERT == 0: print "processing line", count
      count += 1

      try:
        node = (self.nodeclass)(None)
        node.extractLine(line.strip())
      except:
        #traceback.print_exc()
        print "error line:", line
        continue
      if node.skippable(): continue

      if node.threadid != current_threadid:
        # start processing the new thread
        current_threadid = node.threadid
        del window # hope to trigger garbage collection
        window = []
        window.append(node)

      else:
        for i in xrange(len(window)-1, -1, -1):
          othernode = window[i]
          # window size is 50
          if node.position - othernode.position <= self.window_size:
            edge = (self.edgeclass)(node, othernode)
            if edge.selfloop(): continue
            print >>edgeout, edge.toString()
          else:
            # we won't iterate thru the earlier terms in the window
            break
        window.append(node)
    edgeout.close()
    
    # finish processing the lines in the term file. remove skippable edges
    print "FINISH generating the edges. Filtering skippable edges"
    edgein = open(self.edgefile, 'r')
    alledges = defaultdict(int)
    self.edges = {}
    for line in edgein:
      line = line.strip()
      alledges[line] += 1
    for k, v in alledges.items():
      n1, n2, w = k.split(',')
      edge = (self.edgeclass)((self.nodeclass)(n1), (self.nodeclass)(n2))
      edge.weight = v
      if edge.skippable() or edge.selfloop(): continue
      self.edges[edge.id] = edge
    


  def output_netfile_from_term(self):
    n, net_file = tempfile.mkstemp(prefix='', suffix='.net')
    self.net_file = net_file
    print "Pajek file:", net_file
    network = TextNetwork.TextNetwork().run(self.term_file, self.net_file)

  def generate_term_knn(self):
    pass
    
  def run(self):
    self.output_term_pos()
    self.output_netfile_from_term()




class PeopleMilk(ChinaStudy):
  def config(self):
    #self.input_file_encoding = 'UTF-8'
    self.src_txt_dir = r'/home/mrzhou/ChinaMedia/people-3-milk-clean'


if __name__ == '__main__':
  pm = PeopleMilk()
  pm.run()
