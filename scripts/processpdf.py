# -*- coding: utf8 -*-
import os, subprocess

def people_export_pdf_to_txt(src_dir, dst_dir):
  d = os.listdir(src_dir)
  count = 0
  total = len(d)
  err = []
  for f in d:
    src = src_dir+"/"+f
    dst = dst_dir+"/"+f+".txt"
    ret = subprocess.call(["/usr/bin/pdftotext", src, dst])
    if ret!=0:
      print "error:", ret, src
      err.append(src)
    if count % 200 == 0: print "Processing %d/%d" % (count, total)
    count += 1
  if len(err)==0:
    print "No errors"
  else:
    print "Error files"
    for e in err: print e
    
# 1. change chinese name into number name because Java/Groovy doesn't handle chinese filenames on linux (w/o configuration)
# 2. remove stupid characters.
def people_clean_txt(src_dir, dst_dir):
  d = os.listdir(src_dir)
  index = 0
  for f in d:
    outfile = open(dst_dir+'/'+str(index)+'.txt', 'w')
    infile = open(src_dir+'/'+f, 'r')
    for line in infile:
      if line.startswith('http://www.cnki.net'): continue
      if line.startswith("© 1994-2009 China Academic Journal Electronic Publishing House. All rights reserved."): continue
      # remove whitespace in text
      line = line.strip().replace(' ', '')
      print >>outfile, line
    infile.close()
    outfile.close()
    index += 1
    if index%1000 == 0: print "processing %d" % index


if __name__ == '__main__':
  #people_export_pdf_to_txt('../../data/people-3', '../../data/people-3-txt')
  people_clean_txt('/data/data/ChinaMedia/people-3-txt', '/data/data/ChinaMedia/people-3-txt-clean')

