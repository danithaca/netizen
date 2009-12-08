# -*- coding: GBK -*-

# ranges:
# [21,33): noun
# [68,78): verbs
# [2,7): adjectives

def tianya_term(line):
  fields = line.strip().split('\t')
  term = fields[0].strip('"')
  pos = int(fields[1])
  weight = int(fields[2])
  thread = long(fields[4])
  return (term, pos, weight, thread)

def term_output_relations(src, dst):
  #ignore_pos = reduce(list.__add__, [[18], range(78, 96), range(40, 51)])
  allow_pos = reduce(list.__add__, [range(21,33)])
  th_dic = {}
  src = open(src, 'r')
  tag = src.readline()
  print "reading files"
  for line in src:
    (term, pos, weight, thread) = tianya_term(line)
    if weight<5 or pos not in allow_pos: continue
    th_terms = th_dic.get(thread, [])
    th_terms.append((term, weight))
    th_dic[thread] = th_terms
  
  print "total thread:", len(th_dic)
  results={}
  count = 0
  for thread, th_terms in th_dic.items():
    print "("+str(count)+"/"+str(len(th_dic))+")", "processing thread", thread, "with", len(th_terms), "terms", 
    pairs = [(x,y) for x in th_terms for y in th_terms if x[0]<y[0]]
    for t1, t2 in pairs:
      p = (t1[0], t2[0])
      #results[p] = results.get(p, 0) + t1[1] + t2[1]
      results[p] = results.get(p, 0) + 1
    print "-- results:", len(results) 
    count += 1
  
  print "output to file"
  dst = open(dst, 'w')
  for key, value in results.items():
    if value<10: continue
    row = list(key)
    #row.append(str(value/2.0))
    row.append(str(value))
    print >> dst, ','.join(row)
  #print tag
  #print term, pos, weight, thread

if __name__ == '__main__':
  term_output_relations("C:\\Download\\term.raw", "C:\\Download\\term-net.csv")