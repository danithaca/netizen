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
  
  
# remove the leading/trailing double quotes of the URL file  
def cleanup_url_file():
  f = open("D:\\Download\\urls.txt", 'r')
  out = open("D:\\Download\\urls-clean.txt", 'w')
  for line in f:
    line = line.strip('"\n')
    if line == "#EANF#":
      continue
    print line
    out.write(line)
    out.write('\n')
  f.close()
  out.close()



if __name__ == '__main__':
  cleanup_url_file()
  