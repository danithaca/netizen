# coding: utf8
# this is the new script for study after 2011-2
# this file is encoded in UTF8.

import re, os, tempfile, codecs, traceback, collections, igraph, random, numpy, sys, time, csv
from mypytools import read_csv, save_list_to_file, slice_col, UnicodeReader
from scipy.stats.stats import kendalltau
from randomwalk import pagerank


term_file_header = ['threadid', 'position', 'term', 'pos']

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


def compute_kendall(knnx, knny):
  #assert len(knnx) == len(knny)
  knnx = [r[0] for r in knnx]
  knny = [r[0] for r in knny]
  #for x, y in zip(knnx, knny):
  #  print x, '\t', y
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
    #try:
      #return cmp(self.id, other.id)
    #except:
      ## very problematic!!
      #print 'WARNING: can not compare', type(self.id), type(other.id), self.id, other.id
      #return -1

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

def skip_w20_edge(edge):
  if edge.weight<20: return True
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
  shuffle_percentage = 0.5
  shuffle_repeat = 100
  shuffle_repeat2 = 1000
  knn_toplist = 100

  def config(self):
    assert False, "Please override"
    # self.src_txt_dir

  def __init__(self):
    #self.term_pos_file =
    self.config()


  # how it works:
  # run certain number of times, taking % of files, and compare them pair wise
  def reliable_test(self):
    knn_list, results = [], []
    n, self.tau_file = tempfile.mkstemp(prefix='', suffix='.tau')
    out = open(self.tau_file, 'w')
    for i in range(self.shuffle_repeat):
      print "Computing round:", i
      self.output_term_pos()
      edges = self.process_term_file()
      self.output_edges(edges)
      knn = self.generate_term_knn(self.the_term, self.knn_toplist)
      for other in knn_list:
        try:
          tau = compute_kendall(other, knn)
        except:
          traceback.print_exc()
          continue
        print >>out, tau
        results.append(tau)
      knn_list.append(knn)
    print "N, Mean, Std:", len(results), numpy.average(results), numpy.std(results)
    print "Tau file saved to:", self.tau_file



  # another version of reliability test
  # how it works:
  # for each run, cut the corpus into halves, and compare the two halves. save the k-tau scores.
  def reliable_test2(self):
    knn_list, results = [], []
    n, self.tau_file = tempfile.mkstemp(prefix='', suffix='.tau')
    out = open(self.tau_file, 'w')
    for i in range(self.shuffle_repeat2):
      print "Computing round:", i
      fl_all = os.listdir(self.src_txt_dir)
      random.shuffle(fl_all)
      cutpoint = int(len(fl_all)*0.5)
      fl1 = fl_all[:cutpoint]
      fl2 = fl_all[cutpoint:]
      knn1 = self.run_once(fl1)
      knn2 = self.run_once(fl2)
      try:
        tau = compute_kendall(knn1, knn2)
      except:
        traceback.print_exc()
        continue
      print >>out, tau
      results.append(tau)
    print "N, Mean, Std:", len(results), numpy.average(results), numpy.std(results)
    print "Tau file saved to:", self.tau_file


  # this is for the purpose of reliability test
  def run_once(self, file_list):
    self.output_term_pos(file_list)
    edges = self.process_term_file()
    self.output_edges(edges)
    knnlist = self.generate_term_knn(self.the_term, self.knn_toplist)
    return knnlist



  def manipulate_file_list(self, file_list):
    fl = list(file_list)
    random.shuffle(fl)
    ml = fl[:int(len(fl)*self.shuffle_percentage)]
    return ml



  def output_term_pos(self, file_list=None):
    if file_list == None:
      file_list = os.listdir(self.src_txt_dir)
      file_list = self.manipulate_file_list(file_list)
      print "Input dir:", self.src_txt_dir, "--", len(file_list)
    else:
      assert type(file_list) == type([])
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


  # note: whether the output edges are directed or undirected depends on "edgeclass", which is configured in the derived class.
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


  # this class is to be overriden
  def output_edges(self, edges):
    return self.output_pajek(edges)


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



  def output_pairs(self, inedges):
    n, self.pair_file = tempfile.mkstemp(prefix='', suffix='.p')
    output = open(self.pair_file, 'w')
    print "generating pair file", self.pair_file
    for edge in inedges.values():
      if (self.skip_edge)(edge): continue
      # note: we don't assert whether the edges are duplicate or not
      print >>output, ','.join([edge.node1.id, edge.node2.id, str(edge.weight)])
    output.close()


  def generate_term_knn(self, term, limit=-1):
    g = igraph.read(self.net_file)
    # find the node id or "vertex" object for the term
    v = g.vs.select(id_eq = term)
    if len(v) != 1:
      print "can't find term", term
    # v is the 'term' node in the graph g.
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
      if limit == -1: break
      # return this list of (term_name, weight) tuples.
      knnlist.append((g.vs[t[0]]['id'], t[1]))
      #print "%s\t%d" % (g.vs[t[0]]['id'], t[1])
    return knnlist

  # this is for general purpose running
  def run(self):
    self.shuffle_percentage = 1.0 # we take all files.
    self.output_term_pos()
    edges = self.process_term_file()
    self.output_edges(edges)
    knnlist = self.generate_term_knn(self.the_term, self.knn_toplist)
    return knnlist


class ChinaStudyRandomWalk(ChinaStudy):
  #Override
  def output_edges(self, edges):
    return self.output_pairs(edges)

  #Override
  def generate_term_knn(self, term, limit=-1):
    # will use random walk to generate knn list
    # rw_file is the output of random walk
    n, self.rw_file = tempfile.mkstemp(prefix='', suffix='.rw')
    directed = self.edgeclass != UndirectedEdge # note: the dervide class of UndirectedEdge should be taken care of too.
    pagerank(self.pair_file, self.rw_file, [term], directed)

    rows = []
    reader = open(self.rw_file, 'r')
    for line in reader:
      row = line.strip().split(',')
      if len(row) == 0: continue
      elif len(row) != 2: assert False, str(row)
      rows.append((row[0], float(row[1])))

    print "Total neighbors for the term", term, ':', len(rows)
    rows.sort(cmp=lambda x,y: cmp(x[1],y[1]), reverse=True)
    assert rows[0][0] == term, 'Random walk returns false result: the top term should be the restart term'
    del(rows[0])
    if limit == -1:
      return rows
    else:
      return rows[:limit]




#####################################################################
##### command line processing


def compare_datasets(class1, class2):
  knn1 = class1().run()
  knn2 = class2().run()
  save_list_to_file(slice_col(knn1, 0), '/tmp/'+class1.__name__+'.knn')
  save_list_to_file(slice_col(knn2, 0), '/tmp/'+class2.__name__+'.knn')
  tau = compute_kendall(knn1, knn2)
  print "Kendall Tau:", tau, "--", class1.__name__, class2.__name__
  
def reliable_test(classname):
  c = classname()
  c.reliable_test()
  
def reliable_test2(classname):
  c = classname()
  c.reliable_test2()

def run(classname):
  c = classname()
  c.knn_toplist = 10
  knn = c.run()
  for t, w in knn:
    print t, w


# compare term rank diff in two networks
def compare_knn_diff(classname1, classname2, output, knn_cutoff=100):

  def generate_knn(classname):
    c = classname()
    # we take 2000, basically means we return all knn. knn_cutoff will control how many terms we care about.
    c.knn_toplist = 2000
    return c.run()

  # knn1 is the list of knn from the first network
  knn1 = generate_knn(classname1)
  knn1 = [t[0] for t in knn1] # only take the term, not the weight
  knn1_len = len(knn1)
  knn2 = generate_knn(classname2)
  knn2 = [t[0] for t in knn2]
  knn2_len = len(knn2)

  diff = []  # the rank diff
  visited = [] # when we compute knn2, we won't bother the terms already visited in knn1.

  for i in range(min(knn1_len, knn_cutoff)):
    term = knn1[i]
    if term in knn2:
      j = knn2.index(term)
      visited.append(term)
    else:
      j = knn2_len
    diff.append((term, i, j, abs(i-j)))

  for j in range(min(knn2_len, knn_cutoff)):
    term = knn2[j]
    if term in visited: continue
    if term in knn1:
      i = knn1.index(term)
    else:
      i = knn1_len
    diff.append((term, i, j, abs(i-j)))

  # sorting
  diff.sort(cmp=lambda x,y: cmp(x[3], y[3]), reverse=True)
  print "printing difference!"
  f = open(output, 'w')
  for d in diff:
    print '\t'.join(map(str, d))
    print >>f, '\t'.join(map(str, d))
  f.close()



def process_command(debug = True):
  if debug:
    starttime = time.time()
    assert len(sys.argv) == 2, "Please provide one line of python code to execute."
    py_stmt = sys.argv[1]
    print "Python statement to execute:", py_stmt
    eval(py_stmt)
    endtime = time.time()
    diff = endtime - starttime
    print int(diff//3600), 'hours', int((diff%3600)//60), 'minutes', diff%60, 'seconds'
    #print "Total execution hours:", (endtime-starttime)/3600,
  else:
    #command = sys.argv[1]
    #args = sys.argv[2:]
    #eval(command+'('+','.join(args)+')')
	assert len(sys.argv) == 2, "Please provide one line of python code to execute."
	eval(sys.argv[1])



#######################################################################
######### derived classes.

class PeopleMilk(ChinaStudy):
  def config(self):
    #self.input_file_encoding = 'UTF-8'
    self.src_txt_dir = r'/home/mrzhou/ChinaMedia/people-3-milk-clean'
    self.edgeclass = UndirectedEdge
    self.skip_node = skip_nonuserdict_node
    self.skip_edge = skip_single_edge
    
    
class PeopleTiger(ChinaStudy):
  def config(self):
    #self.input_file_encoding = 'UTF-8'
    self.src_txt_dir = r'/home/mrzhou/ChinaMedia/people-3-tiger-clean'
    self.edgeclass = UndirectedEdge
    self.skip_node = skip_nonuserdict_node
    self.skip_edge = skip_single_edge
    
    
class TianyaMilk(ChinaStudy):
  def config(self):
    self.input_file_encoding = 'GBK'
    self.src_txt_dir = r'/home/mrzhou/ChinaMedia/tianya-news-5-milk-txt'
    self.edgeclass = UndirectedEdge
    self.skip_node = skip_nonuserdict_node
    self.skip_edge = skip_w20_edge
  
  
class TianyaTiger(ChinaStudy):
  def config(self):
    self.input_file_encoding = 'GBK'
    self.src_txt_dir = r'/home/mrzhou/ChinaMedia/tianya-news-5-tiger-txt'
    self.edgeclass = UndirectedEdge
    self.skip_node = skip_nonuserdict_node
    self.skip_edge = skip_w20_edge


class PeopleMilkRW(PeopleMilk, ChinaStudyRandomWalk): pass
class PeopleTigerRW(PeopleTiger, ChinaStudyRandomWalk): pass
class TianyaMilkRW(TianyaMilk, ChinaStudyRandomWalk): pass
class TianyaTigerRW(TianyaTiger, ChinaStudyRandomWalk): pass


if __name__ == '__main__':
  #pm = TianyaMilk()
  #pm.run()
  #pm.reliable_test()
  process_command()
