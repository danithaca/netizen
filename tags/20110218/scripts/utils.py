import os

def get_dir_list(path):
  files = os.listdir(path)
  return [(f.split('.'))[0] for f in files]

def get_dir_diff(path1, path2):
  s1 = set(get_dir_list(path1))
  s2 = set(get_dir_list(path2))
  diff = s1 - s2
  return list(diff)



if __name__ == '__main__':
  path_orig = '/Users/danithaca/Desktop/tianya/tianya-news-5-milk'
  path_sele = '/Users/danithaca/Desktop/tianya/milk-selected'
  print len(get_dir_list(path_orig)), len(get_dir_list(path_sele)), len(get_dir_diff(path_orig, path_sele))