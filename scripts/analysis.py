# coding: utf8

import igraph, os

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



if __name__ == '__main__':
  os.chdir('..')
  print os.getcwd()
  output_centrality_csv('data/tigerpeople.net', 'data/tiger-people.csv')