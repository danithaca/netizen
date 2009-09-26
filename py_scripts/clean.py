# -*- coding: GBK -*-
from BeautifulSoup import BeautifulSoup, BeautifulStoneSoup
import re, codecs, sys, os

def filter_str(str):
  return str.replace('&nbsp;', ' ')

def test_bsoup():
  #f = open("143900.shtml", 'r')
  f = open("143953.shtml", 'r')
  doc = f.read().decode('gbk', 'ignore').encode('utf-8', 'ignore')
  start = doc.find('<!-- google_ad_section_start -->')
  if start == -1:
    start = 0
  end = doc.find('<!-- google_ad_section_end -->')
  if end == -1:
    end = None
  doc = doc[start:end]
  #print doc
  soup = BeautifulSoup(''.join(doc))
  
  
  title = soup.find('div', id='adsp_content_title_banner')
  print ''.join(map(lambda a: a.strip(), title.findAll(text=True)))
  
  tagline = soup.find('table', id='firstAuthor')
  print ''.join(map(lambda a: a.strip(), tagline.findAll(text=True)))
  #print soup.prettify().decode('utf-8')
  
  content = soup.find('div', id='pContentDiv').first()
  #print ''.join(map(lambda a: a.strip()+'\n', content.findAll(text=True)))
  for e in content.childGenerator():
    #print '######'
    if isinstance(e, unicode):
      s = e.strip()
      if s != '':
        print s
    else:
      s = ''.join([a.strip()+' ' for a in e.findAll(text=True)])
      s.strip()
      print '>>>===', s
  
  links = soup.findAll('a', attrs={'class':'page_numb'})
  print [a['href'] for a in links]


# transform html to xml
def tianya_news_to_xml(pathfrom, pathto):
  ffrom = open(pathfrom, 'r')
  out = ''
  
  # prepare data
  doc = ffrom.read().decode('gbk', 'ignore').encode('utf-8', 'ignore')
  start = doc.find('<!-- google_ad_section_start -->')
  if start == -1:
    start = 0
  end = doc.find('<!-- google_ad_section_end -->')
  if end == -1:
    end = None
  doc = doc[start:end]
  soup = BeautifulSoup(''.join(doc))
  
  # start of xml
  out = '<thread>'
  
  # title
  title = soup.find('div', id='adsp_content_title_banner')
  out += '<title>' + ''.join([filter_str(a.strip()) for a in title.findAll(text=True)]) + '</title>'
  
  # tagline
  tagline = soup.find('table', id='firstAuthor')
  s = ''.join([filter_str(a.strip()) for a in tagline.findAll(text=True)])
  m = re.search(u"作者：(.+?)提交日期：(.+?)访问：(.+?)回复：(.+)", s)
  firstauthor = filter_str(m.group(1)).strip()
  firsttime = filter_str(m.group(2)).strip()
  out += '<firstauthor>' + firstauthor + '</firstauthor>'
  out += '<firsttime>' + firsttime + '</firsttime>'
  out += '<visits>' + m.group(3).strip() + '</visits>'
  out += '<responses>' + m.group(4).strip() + '</responses>'
  
  # same links
  links = soup.findAll('a', attrs={'class':'page_numb'})
  ids = []
  for a in links:
    ids.append(re.match("http://www.tianya.cn/publicforum/content/news/1/(\\d+)[.]shtml", a['href']).group(1))
  out += '<samelinks>' + ','.join(ids) + '</samelinks>'
  
  # process post contents
  content = soup.find('div', id='pContentDiv').first()
  out += '<post><author>' + firstauthor + '</author><time>' + firsttime + '</time><content>'
  for e in content.childGenerator():
    if isinstance(e, unicode):
      s = filter_str(e).strip()
      if s != '':
        out += s
    else:
      s = ' '.join([filter_str(a.strip()) for a in e.findAll(text=True)])
      s = s.strip()
      if s == '':
        out += '#NEXTLINE\n'
      else:
        m = re.match(u"作者：(.+?)回复日期：(.+)".decode('gbk'), s)
        if m != None:
          # first close <post>, then start new post
          out += '</content></post><post><author>' + filter_str(m.group(1)).strip() + '</author><time>' + filter_str(m.group(2)).strip() + '</time><content>'
        else:
          out += s + '#NEXTLINE\n'
          
  # close xml
  out += '</content></post></thread>'
  
  # prettify and output
  outsoup = BeautifulStoneSoup(out)
  s = outsoup.prettify()
  fto = open(pathto, 'w')
  fto.write(s)
  ffrom.close()
  fto.close()
  
  
# transform shtml files in a direcotry to another directory.
def tianya_news_transform_folder():
  errfile = open("N:\\Download\\err.txt", 'w')
  basedir = "N:\\Download\\tianya-news3\\"
  dir = os.listdir("N:\\Download\\tianya-news3\\")
  count = 0
  for f in dir:
    count += 1
    num = re.match("(\\d+?).shtml", f)
    if num != None:
      print "Processing", f, "(%d / %d)" % (count, len(dir))
      num = num.group(1)
      try:
        tianya_news_to_xml(basedir+f, "N:\\Download\\output\\"+str(num)+".xml")
      except:
        print "!!! ERROR: ", f
        errfile.write(str(num)+'\n')
        errfile.flush()
  errfile.close()


if __name__ == '__main__':
  #sys.setdefaultencoding("utf-8")
  #test_bsoup()
  #tianya_news_to_xml("143900.shtml", 'out.xml')
  tianya_news_transform_folder()