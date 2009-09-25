# -*- coding: GBK -*-
from BeautifulSoup import BeautifulSoup
import re, codecs

def test_bsoup():
  f = codecs.open("139169.shtml", "r", "gbk", 'ignore')
  doc = f.readlines()
  soup = BeautifulSoup(''.join(doc), fromEncoding="gbk")
  print soup.prettify()
  

if __name__ == '__main__':
  test_bsoup()