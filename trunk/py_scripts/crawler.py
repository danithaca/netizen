# -*- coding: GBK -*-

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
TAG POS=1 TYPE=A ATTR=TXT:��һҳ"""
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
TAG POS=1 TYPE=A ATTR=TXT:��һҳ"""
  for i in range(45):
    pos = i + 1
    script += article_t.format(pos)
  script += list_next
  print script

if __name__ == '__main__':
  tianya_next_urls_simple()