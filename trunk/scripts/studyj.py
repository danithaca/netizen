from java.io import *
from java.nio import *
from magicstudio.netizen.util import SmartParser50, SmartParser
import sys, re, traceback
from mypytools import process_command

term_file_header = ['threadid', 'position', 'term', 'pos']

# written for Jython. since jython2.5.2rc4 doesn't support gbk codec, we use Java files.
def output_term_pos(list_file, term_file, input_file_encoding, output_file_encoding):
  analyzer = SmartParser50()
  # use the old parser
  #self.analyzer = SmartParser()
  analyzer.loadUserDict()
  fl = open(list_file, 'r')

  header = term_file_header
  out = OutputStreamWriter(FileOutputStream(term_file), output_file_encoding)
  out.write('\t'.join(header)+'\n')
  buf = CharBuffer.allocate(50000000) # 50M
  for f in fl:
    f = f.strip()
    #print "Processing file:", f
    try:
      threadid = re.search(r'(\d+)[.]txt', f).group(1)
    except:
      traceback.print_exc()
      print "Skip file:", f
      continue
    fi = InputStreamReader(FileInputStream(f), input_file_encoding)
    buf.clear()
    fi.read(buf)
    terms = analyzer.splitTerms(buf.toString())

    position = 0
    for term in terms:
      t = term.getTerm()
      if t=='*': continue
      p = term.getPos()
      row = [threadid, str(position), t, str(p)]
      out.write('\t'.join(row)+'\n')
      position += 1
  out.close()


#def main(opt):
#  if opt[0] == 'process':
#    file_list, term_file, input_file_encoding, output_file_encoding = opt[1:]
#    output_term_pos(file_list, term_file, input_file_encoding, output_file_encoding)
#  else:
#    assert False

if __name__ == '__main__':
#  if len(sys.argv) <= 1:
#    pass
#  else:
#    main(sys.argv[1:])
  process_command()

