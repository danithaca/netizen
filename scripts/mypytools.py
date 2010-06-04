# encoding: gbk
from math import log
from os import listdir
from random import sample
from shutil import copy
from os.path import isdir, basename


# p and q are lists of discrete random variable distributions.
def kl_divergence(p, q):
  if len(p) != len(q): raise Exception('should pass in variables with the same number')
  if sum(p) != 1 or sum(q) != 1: raise Exception('a random variable distribution should sum up to 1')
  t = zip(p, q)
  return sum([ti[0]*log(ti[0]/ti[1],2) for ti in t])


# randomly copy $num of files from src to dst
# src folder can't have sub-folder
def random_sample_files(src, dst, num):
  if type(src)==type([]):
    dirs = src  # multiple dirs
  else:
    dirs = [src]
  files = []
  for dir in dirs:
    for f in listdir(dir):
      files.append(dir+'/'+f)
  print 'Files pool:', len(files)
  s = sample(files, num)
  for f in s:
    copy(f, dst+'/'+basename(f))



# not working!!!
def gbk_to_utf8(dir):
  files = listdir(dir)
  for file in files:
    file = open(dir+'/'+file, 'rw')
    print file.read()


# filter string in a line, and then sort
def filter_sort(filename, filter_str, sort_field_index):
  f = open(filename, 'r')
  lst=[]
  for line in f:
    if line.find(filter_str) != -1:
      line = line.strip()
      fields = line.split(',')
      lst.append((int(fields[sort_field_index]), line))
  lst = sorted(lst, lambda x,y: cmp(x[0],y[0]), None, True)
  for i in lst: print i[1]


def rolling_sum(alist, step):
  results = []
  length = len(alist) - step + 1
  for i in range(length):
    results.append( sum(alist[i : (i+step)]) )
  return results


def remove_empty_lines(file):
  f = open(file, 'r')
  lines = f.readlines()
  f.close()
  f = open(file, 'w')
  for l in lines:
    if l.strip()=='': continue
    print >>f, l,
  f.close()


if __name__ == '__main__':
  #p = [0.5, 0.1, 0.4]
  #q = [0.2, 0.5, 0.3]
  #print kl_divergence(p, q)
  #print rolling_sum([1,2,3,4,5], 3)

  #r = '/Users/danithaca/Desktop/tianya/'
  #random_sample_files([r+'tiger-txt', r+'milk-txt'], r+'random70', 70)
  
  remove_empty_lines('../data/termsusage_v2.1.txt')
