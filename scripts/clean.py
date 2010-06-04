# -*- coding: GBK -*-
from BeautifulSoup import BeautifulSoup, ICantBelieveItsBeautifulSoup
from xml.dom.minidom import Document, parse
import re, codecs, sys, os, traceback

def filter_str(str):
  if str == None:
    return ''
  RE_XML_ILLEGAL = u'([\u0000-\u0008\u000b-\u000c\u000e-\u001f\ufffe-\uffff])' + \
                 u'|' + \
                 u'([%s-%s][^%s-%s])|([^%s-%s][%s-%s])|([%s-%s]$)|(^[%s-%s])' % \
                  (unichr(0xd800),unichr(0xdbff),unichr(0xdc00),unichr(0xdfff),
                   unichr(0xd800),unichr(0xdbff),unichr(0xdc00),unichr(0xdfff),
                   unichr(0xd800),unichr(0xdbff),unichr(0xdc00),unichr(0xdfff))
  regex = re.compile(RE_XML_ILLEGAL)
  for match in regex.finditer(str):
    str = str[:match.start()] + " " + str[match.end():]
  # re.sub(regex, '', str) # don't know why this line doesn't work
  return str.replace('&nbsp;', ' ')


def parse_date(str):
  m = re.match('^(\d{2,4}-\d{1,2}-\d{1,2}).+?(\d{1,2}:\d{1,2}:\d{1,2})$', str)
  if m == None:
    raise RuntimeError("Invalid time format")
  else:
    str = m.groups()[0]+' '+m.groups()[1]
  return str

# transform html to xml
# the filefrom has to be like /(\\d+?).shtml/, and the number will serve as the ID
def tianya_news_to_xml(filefrom, pathto):
  # test file name
  m = re.search("(\\d+?).shtml$", filefrom)
  if m == None:
    raise RuntimeError('File format error.')
  index = int(m.group(1))
  ffrom = open(filefrom, 'r')
  out = Document()
  
  # prepare data
  doc = ffrom.read().decode('gbk', 'ignore').encode('utf-8', 'ignore')
  start = doc.find('<!-- google_ad_section_start -->')
  if start == -1:
    start = 0
  end = doc.find('<!-- google_ad_section_end -->')
  if end == -1:
    end = None
  doc = doc[start:end]
  
  # init the soup parser
  # TODO: note, it dosn't work with, say, 100002.shtml, where <img> has problems. and it doesn't throw error either.
  self_closing = BeautifulSoup.SELF_CLOSING_TAGS
  self_closing['b'] = None
  self_closing['font'] = None
  self_closing['color'] = None
  self_closing['img'] = None
  soup = BeautifulSoup(''.join(doc), convertEntities=BeautifulSoup.ALL_ENTITIES, selfClosingTags=self_closing)
  #soup = BeautifulSoup(''.join(doc))

  # start of xml
  out_thread = out.createElement("thread")
  out.appendChild(out_thread)
  
  # same links
  secondary_link = False
  # at first, we tried to get all <a class='page_numb>, but a better way is just to get <td id='pageDivTop'>
  #links = soup.findAll('a', attrs={'class':'page_numb'})
  pagelinks = soup.find('td', id='pageDivTop')
  if pagelinks:
    links = pagelinks.findAll('a')
  else:
    links = []
  ids = set([])
  # add links to ids
  for a in links:
    m = re.match("http://www.tianya.cn/publicforum/content/news/1/(\\d+)[.]shtml", a['href'])
    if m:
      ids.add(m.group(1))
  # the map() function will ensure each id is a number
  ids = set(map(int, ids))
  
  if len(ids)>0:
    # this block is to handle cases multiple pages for the same thread.
    # first, decide which id is the thread_id: obviously it's the min id.
    if index > min(ids):
      ids.add(index)
      index = min(ids) # we set the index to be the min value.
      secondary_link = True
      # now we know this file is not the first page, so that it should not set first author/time for the thread
    
    # now we look at if the thread file is already exists.
    if os.path.exists(pathto+"/"+str(index)+".xml"):
      # replace out as from the xml file
      out = parse(pathto+"/"+str(index)+".xml")
      out_thread = out.documentElement
    # we don't handle cases where we don't have an existing file
    
    if out_thread.hasAttribute('samelinks'):
      ids = ids.union(map(int, out_thread.getAttribute('samelinks').split(',')))
    out_thread.setAttribute('samelinks', ','.join(map(str, ids)))
  
  #after processing the "samelinks", now we have out as minidom Document, and out_thread as the root element.    
  
  # title and id
  if not secondary_link:
    title = soup.find('div', id='adsp_content_title_banner')
    out_thread.setAttribute('title', ''.join([filter_str(a.strip()) for a in title.findAll(text=True)]))
    out_thread.setAttribute('id', str(index))
  
  # tagline
  tagline = soup.find('table', id='firstAuthor')
  s = ''.join([filter_str(a.strip()) for a in tagline.findAll(text=True)])
  # can't use \d for numbers..., maybe due to unicode??
  # m = re.search(re.compile(u"作者：(.+?)提交日期：(.+?)访问：(\\d+?)回复：(\\d+?)", re.UNICODE), s)
  m = re.search(re.compile(u"作者：(.+?)提交日期：(.+?)访问：(.+?)回复：(.+)", re.UNICODE), s)
  if m == None:
    m = re.search(u"作者：(.+?)提交日期：(.+)", s)

  fields = m.groups()
  firstauthor = filter_str(fields[0]).strip()
  firsttime = filter_str(fields[1]).strip()
  # validate date 'cause we can't do it earlier.
  firsttime = parse_date(firsttime)
  # only first link need to set these attributes
  if not secondary_link:
    out_thread.setAttribute('firstauthor', firstauthor)
    out_thread.setAttribute('firsttime', firsttime)
    
  if (not secondary_link) and len(fields)>2:
    if re.match('^-?\d+$', fields[2].strip())==None:
      raise RuntimeError("Invalid visits number")
    out_thread.setAttribute('visits', fields[2].strip())
  if (not secondary_link) and len(fields)>3:
    if re.match('^-?\d+$', fields[3].strip())==None:
      raise RuntimeError("Invalid responses number")
    out_thread.setAttribute('responses', fields[3].strip())
  
  # processing post content. it's the same for both first link and secondary links.
  content = soup.find('div', id='pContentDiv').first()
  out_post = out.createElement("post")
  out_post.setAttribute('author', firstauthor)
  out_post.setAttribute('time', firsttime)
  content_str = ''
  
  # process the text in the post content.
  exc = None
  for e in content.childGenerator():
    try:
      if isinstance(e, unicode):
        s = filter_str(e).strip()
        if s != '':
          content_str += s
      else:
        s = ' '.join([filter_str(a.strip()) for a in e.findAll(text=True)])
        s = s.strip()
        if s == '':
          content_str += '#NL\n'
        else:
          m = re.match(u"作者：(.+?)回复日期：(.+)".decode('gbk'), s)
          if m != None:
            # first close <post>, then start new post
            out_content = out.createTextNode(content_str)
            out_post.appendChild(out_content)
            out_thread.appendChild(out_post)
            out_post = out.createElement("post")
            out_post.setAttribute('author', filter_str(m.group(1)).strip())
            posttime = parse_date(filter_str(m.group(2)).strip())
            out_post.setAttribute('time', posttime)
            content_str = ''
          else:
            content_str += s + '#NL\n'
    except:
      exc = PostException('single post exception')
      
  # close xml
  out_content = out.createTextNode(content_str)
  out_post.appendChild(out_content)
  out_thread.appendChild(out_post)
  
  # prettify and output
  # no matter whether we have the thread file or not, we'll just overwrite it.
  fto = open(pathto+"/"+str(index)+".xml", 'w')
  fto.write(out.toprettyxml(indent=" ", encoding='utf-8'))
  ffrom.close()
  fto.close()
  if exc != None:
    raise exc
  
  
# transform shtml files in a direcotry to another directory.
def tianya_news_transform_folder(pathfrom, pathto):
  errfile = open(pathto+"/_err.txt", 'w')
  dir = os.listdir(pathfrom)
  count = 0
  for f in dir:
    count += 1
    num = re.match("(\\d+?).shtml", f)
    if num != None:
      if (count%200)==1:
        print "Processing shtml-xml: ", f, "(%d / %d)" % (count, len(dir))
      num = num.group(1)
      try:
        tianya_news_to_xml(pathfrom+'/'+f, pathto)
      except PostException:
        print "partial error", f
        errfile.write('--'+str(num)+'\n')
        traceback.print_tb(sys.exc_info()[2], file=errfile)
        errfile.flush()
      except:
        print "!!! ERROR:", f
        errfile.write(str(num)+'\n')
        traceback.print_tb(sys.exc_info()[2], file=errfile)
        errfile.flush()
  errfile.close()
  

def tianya_news_transform_err():
  errfile = open("C:\\Download\\xml-news5\\err.txt", 'r')
  count = 0
  for num in errfile:
    num = num.strip()
    print num
    if num in ('100229'):
      continue
    if num != None:
      #try:
        tianya_news_to_xml("C:\\Download\\tianya-news5\\"+num+".shtml", "D:\\Download\\output_err\\"+num+".xml")
      #except:
      #  print num
  errfile.close()


def print_error_file():
  f = open("C:\\Download\\news5-xml-new\\_err.txt", 'r')
  count = 0
  for l in f:
    if l[0]!=' ':
      print l.strip()
      count += 1
  print "Total:", count


class PostException(Exception):
  def __init__(self,value):
    self.value = value
  def __str__(self):
    return repr(self.value)


if __name__ == '__main__':
  #sys.setdefaultencoding("utf-8")
  #test_bsoup()
  #tianya_news_to_xml("C:\\Download\\tianya-news5\\100002.shtml", 'C:\\Download\\')
  #tianya_news_transform_folder("/data/data/ChinaMedia/tianya-news-5", "/data/data/ChinaMedia/tianya-news-5-xml")
  tianya_news_transform_folder("../tianya-news-6", "../tianya-news-6-xml")
  #print_error_file()
  
