# coding: utf8

import igraph, os, sys

def output_centrality_csv(netfile, output):
  g = igraph.read(netfile)
  o = open(output, 'w')
  print g.summary()
  vs = g.vs
  print 'calc betweenness'
  betweenness = vs.betweenness(directed=False)
  print 'calc closeness'
  closeness = vs.closeness(mode=igraph.ALL)
  print 'calc degree'
  degree = vs.degree(type=igraph.ALL, loops=False)
  header = ['id', 'term', 'degree', 'betweeness', 'closeness']
  print >>o, ','.join(header)
  for v in vs:
    i = v.index
    print >>o, ','.join(map(str, [i, v['id'], degree[i], betweenness[i], closeness[i]]))
  o.close()

def output_term_knn(netfile, term, limit=0):
  g = graph_info(netfile)
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
    print "%s\t%d" % (g.vs[t[0]]['id'], t[1])
  return knnlist


# TODO: think about how to do this. comparing the rank difference between knn from 2 networks
def term_knn_diff(f1, f2, term, knn_cutoff=100):
  # knn1 is the list of knn from the first network
  knn1 = output_term_knn(f1, term)
  knn1 = [t[0] for t in knn1] # only take the term, not the weight
  knn1_len = len(knn1)
  knn2 = output_term_knn(f2, term)
  knn2 = [t[0] for t in knn2]
  knn2_len = len(knn2)

  diff = []
  visited = []
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
  for d in diff:
    print '\t'.join(map(str, d))


def graph_info(netfile):
  g = igraph.read(netfile)
  #print "edges attributes:", g.edge_attributes()
  #print "edges attributes:", g.vertex_attributes()
  if g.is_directed(): print "directed graph"
  else: print "indirected graph"

#  for v in g.vs:
#    print v
#  for e in g.es:
#    print e
#  v = g.vs.select(id_eq = ur'夏')
#  print v[0]['shape']
  return g


if __name__ == '__main__':
  #os.chdir('../data4tech')
  #print os.getcwd()
  #output_centrality_csv('data/tigerpeople.net', 'data/tiger-people.csv')
  #graph_info('test.net')
  #output_term_knn('peoplefull.net', ur'法律')
  #term_knn_diff('tianyafull.net', 'peoplefull.net', ur'法律')
  if len(sys.argv) <= 1:
    pass
  elif sys.argv[1] == 'info':
    graph_info(sys.argv[2])
  else:
    assert False

