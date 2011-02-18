# -*- coding: GBK -*-
import re

# generate next urls for tianya-news
def tianya_next_urls():
  #textencoding = "GBK"
  script = """VERSION BUILD=6240709 RECORDER=FX
SET !EXTRACT_TEST_POPUP NO
TAB T=1
URL GOTO=http://www.tianya.cn/publicforum/articleslist/0/news.shtml"""
  article_t = """
TAG POS={0} TYPE=A ATTR=HREF:http://www.tianya.cn/publicforum/content/news/1/*.shtml EXTRACT=HREF
SAVEAS TYPE=EXTRACT FOLDER=D:\Download FILE=urls.txt"""
  list_next = """
TAG POS=1 TYPE=A ATTR=TXT:下一页"""
  for pages in range(800):
    for i in range(45):
      pos = i + 1
      script += article_t.format(pos)
    script += list_next
  f = open("D:\\Download\\script.iim", 'w')
  f.write(script)
  #print script

def tianya_next_urls_simple():
  #textencoding = "GBK"
  script = """VERSION BUILD=6240709 RECORDER=FX
SET !EXTRACT_TEST_POPUP NO
TAB T=1"""
  article_t = """
TAG POS={0} TYPE=A ATTR=HREF:http://www.tianya.cn/publicforum/content/news/1/*.shtml EXTRACT=HREF
SAVEAS TYPE=EXTRACT FOLDER=D:\Download FILE=urls.txt"""
  list_next = """
TAG POS=1 TYPE=A ATTR=TXT:下一页"""
  for i in range(45):
    pos = i + 1
    script += article_t.format(pos)
  script += list_next
  print script
  
def qiangguo_next_urls_simple():
  script = """VERSION BUILD=6240709 RECORDER=FX
SET !EXTRACT_TEST_POPUP NO
TAB T=1"""
  article_t = """
TAG POS={0} TYPE=A ATTR=HREF:http://bbs.people.com.cn/postDetail.do?view=1&id=*&bid=1 EXTRACT=HREF
SAVEAS TYPE=EXTRACT FOLDER=N:\Download FILE=qiangguo2008-urls.txt"""
  list_next = """
TAG POS=2 TYPE=A ATTR=TXT:下页"""
  for i in range(50):
    pos = i + 1
    script += article_t.format(pos)
  script += list_next
  print script
  
# remove the leading/trailing double quotes of the URL file  
def cleanup_url_file(infiles, outfile):
  count=1
  out = open(outfile, 'w')
  for infile in infiles:
    f = open(infile, 'r')
    for line in f:
      line = line.strip('"\n\r')
      if line == "#EANF#" or line=='':
        continue
      line = re.sub("^\W*", '', line)
      count += 1
      out.write(line)
      out.write('\n')
    f.close()
  out.close()
  print "Total lines:", count

# generate incremental url file based on the difference between the old and new files
def generate_incremental_url_file(newfile, oldfile, outfile):
  out = open(outfile, 'w')
  f1 = open(newfile, 'r')
  f2 = open(oldfile, 'r')
  n1 = set([])
  for line in f1:
    n1.add(line.strip())
  print "new file: ", len(n1)
  n2 = set([])
  for line in f2:
    n2.add(line.strip())
  print "old file: ", len(n2)
  n = n1 - n2
  print "difference", len(n)
  for line in n:
    print >>out, line
  f1.close()
  f2.close()
  out.close()
  
  
def split_url_file(infile, size):
  f = open(infile, 'r')
  count = 0
  for line in f:
    index = count // size
    mod = count % size
    if mod == 0:
      o = open(infile+str(index), 'w')
    o.write(line)
    count += 1



if __name__ == '__main__':
    #cleanup_url_file(["N:\\Download\\urls.txt"], "N:\\Download\\tianya-news6-clean-urls.txt")
    #split_url_file("D:\\Download\\people-clean-urls.txt", 20000)
    #tianya_next_urls_simple()
    generate_incremental_url_file("N:/Download/urls.txt", "N:/Download/tianya-news-urls-clean.txt", "N:/Download/tianya-news6-clean-urls.txt")
