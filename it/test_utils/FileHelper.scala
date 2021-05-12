package test_utils

import scala.io.{BufferedSource, Source}

object FilerHelper {

  def loadFile(name: String): String = {
    var source: BufferedSource = null
    try {
      source = Source.fromFile(name)
      source.mkString
    } finally {
      if(source != null)
        source.close()
    }
  }
}