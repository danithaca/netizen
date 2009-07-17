def tianyaNews = ["http://www.tianya.cn/publicforum/content/news/1/%i.shtml", 75000, 135000]
new File("output.txt").withWriter { writer ->
  (tianyaNews[1]..tianyaNews[2]).each {
    //println tianyaNews[0].replace('%i', it.toString())
    writer << tianyaNews[0].replace('%i', it.toString()) << '\n';
    //writer.writeLine tianyaNews[0]
  }
}