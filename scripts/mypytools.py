# -*- coding: GBK -*-
from math import log
from os import listdir
from random import sample
from shutil import copy


# p and q are lists of discrete random variable distributions.
def kl_divergence(p, q):
  if len(p) != len(q): raise Exception('should pass in variables with the same number')
  if sum(p) != 1 or sum(q) != 1: raise Exception('a random variable distribution should sum up to 1')
  t = zip(p, q)
  return sum([ti[0]*log(ti[0]/ti[1],2) for ti in t])


# randomly copy $num of files from src to dst
# src folder can't have sub-folder
def random_sample_files(src, dst, num):
  p = listdir(src)
  s = sample(p, num)
  for f in s:
    copy(src+'/'+f, dst+'/'+f)
  

if __name__ == '__main__':
#  p = [0.5, 0.1, 0.4]
#  q = [0.2, 0.5, 0.3]
#  print kl_divergence(p, q)
  random_sample_files('c:/aa', 'c:/bb', 3)