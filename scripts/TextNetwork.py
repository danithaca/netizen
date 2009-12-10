# coding: utf8

# this is the super class for all text-based network.
class TextNetwork:
  
  # in: the unit to be processed
  # out: a list of relations between nodes
  def processUnit(self, unit): pass
  
  # in: the corpus to be processed
  # out: a list of relations between nodes
  def processCorpus(self, corpus): pass
  

class PeopleTextNetwork(TextNetwork):
  
  def processUnit(self, unit):
    pass