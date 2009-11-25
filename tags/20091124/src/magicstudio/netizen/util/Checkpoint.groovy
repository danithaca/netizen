package magicstudio.netizen.util


public class Checkpoint {
  private items = [];
  private checkFile;
  private separator = '\t'
  private recovered = false

  public Checkpoint(file="C:\\Download\\checkpoint.txt") {
    checkFile = new File(file)
    if (!checkFile.exists()) {
      checkFile.createNewFile()
      recovered = false
    } else {
      def c = checkFile.getText()
      items = c.tokenize()
      recovered = true
    }
  }

  public isRecovered() {
    return recovered
  }

  public check(item) {
    checkFile.append(item+separator)
    items.push(item)
  }

  public startOver() {
    checkFile.write('')
  }

  public getItems() {
    return items;
  }
}