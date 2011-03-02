# this is the new script for study after 2011-2

import re, os, tempfile
from java.io import *
from java.nio import *
from magicstudio.netizen.util import SmartParser50
import TextNetwork

class ChinaStudy(object):
  input_file_encoding = 'GBK'
  output_file_encoding = 'GBK'
  
  def config(self):
    assert False, "Please override"
    # self.src_txt_dir

  def __init__(self):
    #self.term_pos_file =
    self.config()
    self.analyzer = SmartParser50()
    
  def output_term_pos(self):
    file_list = os.listdir(self.src_txt_dir)
    n, term_file = tempfile.mkstemp(suffix='.t')
    print "Output terms to:", term_file
    self.term_file = term_file
    self._output_term_pos(file_list, term_file)

  # written for Jython. since jython2.5.2rc4 doesn't support gbk codec, we use Java files.
  def _output_term_pos(self, file_list, term_file):
    header = ['threadid', 'position', 'term', 'pos']
    out = OutputStreamWriter(FileOutputStream(term_file), self.output_file_encoding)
    out.write('\t'.join(header)+'\n')
    for f in file_list:
      try:
        threadid = re.search(r'(\d+)[.]txt').group(1)
      except:
        print "Skip file:", f
        continue
      fi = InputStreamReader(FileInputStream(f), self.input_file_encoding)
      content = CharBuffer()
      fi.read(content)
      terms = self.analyzer.splitTerms(content)
      
      position = 0
      for term in terms:
        t = term.getTerm()
        if t=='*': continue
        p = term.getPosId()
        row = [threadid, position, t, p]
        out.write('\t'.join(row)+'\n')
        position += 1
      

  def output_netfile_from_term(self):
    n, net_file = tempfile.mkstemp(suffix='.net')
    self.net_file = net_file
    print "Pajek file:", net_file
    network = TextNetwork.TextNetwork(self.term_file, self.net_file)

  def generate_term_knn(self):
    pass
  
  
class PeopleMilk(ChinaStudy):
  pass