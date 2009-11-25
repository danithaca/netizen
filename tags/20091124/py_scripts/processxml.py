# -*- coding: GBK -*-
# temporaily not working
from xml.dom.minidom import parse
import os

def tianya_export_xml_to_raw(src_dir, dst_dir):
  thread_raw = open(dst_dir+'\\thread.raw', 'w')
  post_raw = open(dst_dir+'\\post.raw', 'w')
  for xml in os.listdir(src_dir):
    doc = parse(src_dir + '\\' + xml)
    thread = doc.getElementsByTagName('thread')
    
    break
    

if __name__ == '__main__':
  tianya_export_xml_to_raw('D:\\Download\\xml-news5', 'D:\\Download')
