# this is a simple example of random walk.
# random walk usually follows this convention:
# F(t+1) = (1-alpha)*S*F(t) + alpha*Y

# library requirement: numpy/scipy, bidict

import numpy, itertools, scipy.sparse
from bidict import bidict

class BaseRandomWalk(object):
  # alpha is the teleport factor
  ALPHA = 0.15
  # tolerance to stop interation. this is the percentage of change.
  TOLERANCE = 0.05
  
  def prepareY(self):
    #return Y
    assert False, 'Please override'
    
  def prepareS(self):
    #return S
    assert False, 'Please override'
    
  def prepareF0(self):
    #return F0
    assert False, 'Please override'
    
  def setupNodeMap(self):
    assert False, 'Please override'


  def isFinished(self, Ft1, Ft0):
    assert Ft1.shape == Ft0.shape, 'Ft1 and Ft0 should have the same dimension.'
    diff = numpy.abs(Ft1 - Ft0)
    rowsize, colsize = diff.shape
    
    for i, j in itertools.product(range(rowsize), range(colsize)):
      if Ft0[i,j] == 0 and diff[i,j] == 0:
        continue # we want this condition to avoid divided by zero
      elif Ft0[i,j] == 0 and diff[i,j] != 0:
        return False
      elif diff[i,j]/Ft0[i,j] > self.TOLERANCE:
        return False
    else:
      return True
      
      
  # do the iteration.
  def iterate(self):
    Y = self.prepareY()
    S = self.prepareS()
    Ft0 = self.prepareF0()
    alpha = self.ALPHA

    count = 0
    while True:
      if count % 10 == 0:
        print "Iterating round:", count
      count += 1
      # the iteration
      Ft1 = (1-alpha) * S * Ft0 + alpha * Y
      if self.isFinished(Ft1, Ft0):
        print "Finish iteration in round:", count
        break
      else:
        Ft0 = Ft1
    return Ft1

  # rows is a list of tuples: (src, dst weight)
  def initFromRows(self, rows):
    # nodeMap is from node_index=>node_name
    self.nodeMap = bidict()
    converted_rows = []
    
    print 'Init graph from rows:', len(rows)
    for src, dst, weight in rows:
      src_index = self.registerNode(src)
      dst_index = self.registerNode(dst)
      converted_rows.append((src_index, dst_index, weight))
    dimension = len(self.nodeMap)
    
    # to have maximum compatibility w/ Matlab or R, we use lil_matrix rather than the dok_matrix, and maintain the index->node mapping ourselves.
    W = scipy.sparse.lil_matrix((dimension, dimension))
    for src_index, dst_index, weight in converted_rows:
      W[src_index, dst_index] = weight
      
    W = W.tocsr()
    #print W.todense()

    # compute D^-1
    dlist = numpy.asarray(W.sum(axis=1).transpose())[0,]
    assert len(dlist) == dimension
    dlist_neg = dlist ** -1
    D_neg = scipy.sparse.spdiags(data=dlist_neg, diags=array([0]), m=dimension, n=dimension)
    
    # compute S=D^-1*W
    S = D_neg * W
    S = S.tocsc()
    #print A.todense()
      
  
      
  def registerNode(self, node):
    if node not in ~(self.nodeMap):
      index = len(self.nodeMap)
      self.nodeMap[index] = node
    else:
      index = self.nodeMap[:node]
    return index


class PageRank(BaseRandomWalk):
  pass
  


class AbsorbingRandomWalk(BaseRandomWalk):
  pass
  


class LocalConsistencyGlobalConsistency(BaseRandomWalk):
  pass
  
