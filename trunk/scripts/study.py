# coding: utf8
# this is the new script for study after 2011-2
# this file is encoded in UTF8.

import re, os, tempfile, codecs, traceback, collections, igraph, random, numpy
from mypytools import read_csv
from scipy.stats.stats import kendalltau


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


def compute_kendall(knnx, knny):
  #assert len(knnx) == len(knny)
  knnx = [r[0] for r in knnx]
  knny = [r[0] for r in knny]
  for x, y in zip(knnx, knny):
    print x, '\t', y
  totalsize = min(len(knnx), len(knny))
  knnx = knnx[:totalsize]
  knny = knny[:totalsize]
  dx, dy = {}, {}
  x, y = [], []
  for i,w in enumerate(knnx): dx[w] = i
  for i,w in enumerate(knny): dy[w] = i
  for w in set(dx.keys() + dy.keys()):
    x.append(dx.get(w, totalsize+1))
    y.append(dy.get(w, totalsize+1))
  ktau = kendalltau(x, y)
  return ktau[0]

class Node:
  # initialize synonyms for the class
  synonyms = generate_synonyms()

  def __init__(self, id):
    self.id = id
  def __cmp__(self, other):
    return cmp(self.id, other.id)

  # note: will take care of synonyms
  def construct_from_line(self, line):
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
  def toString(self):
    return "%s,%s,%s" % (self.node1.id, self.node2.id, self.weight)


class UndirectedEdge(Edge):
  def __init__(self, node1, node2):
    if node1>node2:
      node1, node2 = node2, node1
    Edge.__init__(self, node1, node2)


def no_skip_node(node): return False
def no_skip_edge(edge): return False

def skip_nonuserdict_node(node):
  if node.pos.startswith('zz'):
    #if re.match('\W+', self.term, re.UNICODE): return True
    return False
  else:
    return True

def skip_single_edge(edge):
    # only remove the 1-timers
    if edge.weight<2: return True
    else: return False


class ChinaStudy(object):
  input_file_encoding = 'utf8'
  output_file_encoding = 'utf8'
  nodeclass = Node
  edgeclass = Edge
  skip_node = no_skip_node
  skip_edge = no_skip_edge
  window_size = 50
  the_term = '法律'
  shuffle_percentage = 1.0
  shuffle_repeat = 100
  knn_toplist = 100

  def config(self):
    assert False, "Please override"
    # self.src_txt_dir

  def __init__(self):
    #self.term_pos_file =
    self.config()

  def reliable_test(self):
    knn_list, results = [], []
    n, self.tau_file = tempfile.mkstemp(prefix='', suffix='.tau')
    out = open(self.tau_file, 'w')
    for i in range(self.shuffle_repeat):
      print "Computing round:", i
      self.output_term_pos()
      edges = self.process_term_file()
      self.output_pajek(edges)
      knn = self.generate_term_knn(self.the_term, self.knn_toplist)
      for other in knn_list:
        tau = compute_kendall(other, knn)
        print >>out, tau
        results.append(tau)
      knn_list.append(knn)
    print "N, Mean, Std:", len(results), numpy.average(results), numpy.std(results)

  def manipulate_file_list(self, file_list):
    fl = list(file_list)
    random.shuffle(fl)
    return fl[:int(len(fl)*self.shuffle_percentage)]

  def output_term_pos(self):
    file_list = os.listdir(self.src_txt_dir)
    file_list = self.manipulate_file_list(file_list)
    n, list_file = tempfile.mkstemp(prefix='', suffix='.fl')
    fl = open(list_file, 'w')
    for f in file_list:
      print >>fl, self.src_txt_dir+'/'+f
    fl.close()
    n, term_file = tempfile.mkstemp(prefix='', suffix='.t')
    print "Output terms to:", term_file
    self.term_file = term_file
    cmd = "jython studyj.py \"output_term_pos('%s','%s','%s','%s')\"" % (list_file, term_file, self.input_file_encoding, self.output_file_encoding)
    #print cmd
    os.system(cmd)
    #print "finish"

  def process_term_file(self):
    term_file = self.term_file
    print "processing terms from term file:", term_file
    term_file = open(term_file, 'r')
    assert term_file.readline().strip().split() == term_file_header

    current_threadid = None
    window = []
    # using edgefile as temporary storage for the edges.
    n, self.edgefile = tempfile.mkstemp(prefix='', suffix='.ed')
    print "saving temp edge link to:", self.edgefile
    edgeout = open(self.edgefile, 'w')

    count = 0
    COUNTALERT=100000
    for line in term_file:
      if count%COUNTALERT == 0: print "processing term line", count
      count += 1

      try:
        node = (self.nodeclass)(None)
        node.construct_from_line(line.strip())
      except:
        traceback.print_exc()
        print "error line:", line
        continue
      if (self.skip_node)(node): continue

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
    alledges = collections.defaultdict(int)
    edges = {}
    for line in edgein:
      line = line.strip()
      alledges[line] += 1
    # TODO: weight calc is problematic here
    for k, v in alledges.items():
      n1, n2, w = k.split(',')
      edge = (self.edgeclass)((self.nodeclass)(n1), (self.nodeclass)(n2))
      edge.weight = v
      if (self.skip_edge)(edge) or edge.selfloop(): continue
      edges[edge.id] = edge
    return edges


  def output_pajek(self, inedges):
    n, self.net_file = tempfile.mkstemp(prefix='', suffix='.net')
    print "generating network file", self.net_file
    nodes = []
    nodesindex = {}
    edges = []
    for key, edge in inedges.items():
      if (self.skip_edge)(edge): continue
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
    output = open(self.net_file, 'w')
    print >>output, "*Vertices    ", len(nodes)
    for n in nodes:
      print >>output, count, '"'+n.id+'"'
      count += 1
    print >>output, "*Arcs"
    print >>output, "*Edges"
    for e in edges:
      print >>output, e[0], e[1], e[2]
    output.close()


  def generate_term_knn(self, term, limit=0):
    g = igraph.read(self.net_file)
    # find the node id or "vertex" object for the term
    v = g.vs.select(id_eq = term)
    if len(v) != 1:
      print "can't find term", term
    v = v[0]
    # find the adjacent verteces (neighbor)
    neighbors = g.neighbors(v.index)
    print "a total of", len(neighbors), "neighbors"
    # find the adjacent edges with weights
    adjedges = g.adjacent(v.index)
    assert len(neighbors) == len(adjedges)
    l = []
    knnlist = []
    for e in adjedges:
      if g.is_loop(e):
        print "contains self loop", e
        continue
      e = g.es[e]
      o = e.target if e.target!=v.index else e.source
      l.append((o, e['weight']))
    l.sort(cmp=lambda x,y: cmp(x[1],y[1]), reverse=True)
    for t in l:
      limit -= 1
      if limit == 0: break
      knnlist.append((g.vs[t[0]]['id'], t[1]))
      #print "%s\t%d" % (g.vs[t[0]]['id'], t[1])
    return knnlist




  def run(self):
    self.shuffle_percentage = 1.0 # we take all files.
    #self.output_term_pos()
    #edges = self.process_term_file()
    #self.output_pajek(edges)
    self.net_file = '/tmp/KX80md.net'
    knnlist = self.generate_term_knn(self.the_term)
    #print compute_kendall(knnlist, r)


class PeopleMilk(ChinaStudy):
  def config(self):
    #self.input_file_encoding = 'UTF-8'
    self.src_txt_dir = r'/home/mrzhou/ChinaMedia/people-3-milk-clean'
    self.edgeclass = UndirectedEdge
    self.skip_node = skip_nonuserdict_node
    self.skip_edge = skip_single_edge


if __name__ == '__main__':
  pm = PeopleMilk()
  #pm.run()
  pm.reliable_test()
