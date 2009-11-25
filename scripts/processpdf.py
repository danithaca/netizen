import os, subprocess

def people_export_pdf_to_txt(src_dir, dst_dir):
  d = os.listdir(src_dir)
  count = 0
  total = len(d)
  err = []
  for f in d:
    src = src_dir+"\\"+f
    dst = dst_dir+"\\"+f+".txt"
    ret = subprocess.call(["N:\\Download\\PDF2TXT3.0\\pdf2txt.exe ", src, dst])
    if ret!=1:
      print "error:", ret, src
      err.append(src)
    if count % 200 == 0: print "Processing %d/%d" % (count, total)
    count += 1
  print "Error files"
  for e in err: print e

if __name__ == '__main__':
  people_export_pdf_to_txt('N:\\Download\\people3', 'N:\\Download\\people3_txt')