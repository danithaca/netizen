# coding: utf8

import igraph, os

def output_centrality_csv(netfile, output):
  g = igraph.read(netfile)
  o = open(output, 'w')
  vs = g.vs
  print 'calc betweenness'
  betweenness = vs.betweenness(directed=False)
  print 'calc closeness'
  closeness = vs.closeness(mode=igraph.ALL)
  print 'calc degree'
  degree = vs.degree(type=igraph.ALL, loops=False)
  for v in vs:
    i = v.index
    print >>o, ','.join(map(str, [i, v['id'], degree[i], betweenness[i], closeness[i]]))
  o.close()



if __name__ == '__main__':
  os.chdir('..')
  print os.getcwd()
  output_centrality_csv('tiger.net', 'tiger.csv')