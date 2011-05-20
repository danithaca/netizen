from java.io import *
from java.nio import *
from org.apache.commons.io import FileUtils
from magicstudio.netizen.util import SmartParser50, SmartParser
import sys, re, traceback, time, os, csv

term_file_header = ['threadid', 'position', 'term', 'pos']

# written for Jython. since jython2.5.2rc4 doesn't support gbk codec, we use Java files.
def output_term_pos(list_file, term_file, input_file_encoding, output_file_encoding):
  #analyzer = SmartParser50()
  # use the old parser
  #self.analyzer = SmartParser()
  #analyzer.loadUserDict()
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
    fc = FileUtils.readFileToString(File(f), input_file_encoding)
    analyzer = SmartParser50()
    terms = analyzer.splitTerms(fc)
    #print "Terms:", len(terms)
    analyzer.exit()

    position = 0
    for term in terms:
      t = term.getTerm()
      if t=='*': continue
      p = term.getPos()
      row = [threadid, str(position), t, str(p)]
      out.write('\t'.join(row)+'\n')
      position += 1
  out.close()
  fl.close()
  #analyzer.exit()


def test_output_term():
  src_txt_dir = r'/home/mrzhou/ChinaMedia/people-3-milk-clean'
  file_list = [src_txt_dir+'/'+f for f in os.listdir(src_txt_dir)]
  print "Total files:", len(file_list)
  def prep_fl(fn, rows):
    fl = open(fn, 'w')
    for f in rows:
      print >>fl, f
    fl.close()
  print "Generating first file"
  prep_fl('/tmp/1.fl', file_list)
  output_term_pos('/tmp/1.fl', '/tmp/1.t', 'utf8', 'utf8')
  print "Generating second file"
  file_list.reverse()
  prep_fl('/tmp/2.fl', file_list)
  output_term_pos('/tmp/2.fl', '/tmp/2.t', 'utf8', 'utf8')


#def main(opt):
#  if opt[0] == 'process':
#    file_list, term_file, input_file_encoding, output_file_encoding = opt[1:]
#    output_term_pos(file_list, term_file, input_file_encoding, output_file_encoding)
#  else:
#    assert False

def process_command(debug = True):
  if debug:
    starttime = time.time()
    assert len(sys.argv) == 2, "Please provide one line of python code to execute."
    py_stmt = sys.argv[1]
    print "Python statement to execute:", py_stmt
    eval(py_stmt)
    endtime = time.time()
    diff = endtime - starttime
    print int(diff//3600), 'hours', int((diff%3600)//60), 'minutes', diff%60, 'seconds'
    #print "Total execution hours:", (endtime-starttime)/3600,
  else:
    #command = sys.argv[1]
    #args = sys.argv[2:]
    #eval(command+'('+','.join(args)+')')
	assert len(sys.argv) == 2, "Please provide one line of python code to execute."
	eval(sys.argv[1])

if __name__ == '__main__':
#  if len(sys.argv) <= 1:
#    pass
#  else:
#    main(sys.argv[1:])
  process_command()

