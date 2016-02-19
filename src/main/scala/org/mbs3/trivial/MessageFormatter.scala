package org.mbs3.trivial

import java.util.Properties

object MessageFormatter {
  def msg(key: String, args: Any*): String = {
    if(props.containsKey(key))
      return props.getProperty(key).format(args:_*)

    return "THIS IS AN ERROR. PLEASE REPORT ERROR: %s".format(key.toUpperCase())
  }

  lazy val props = loadProperties
  def loadProperties : Properties = {
    val p = new Properties()
    try {

      val stream = getClass.getClassLoader.getResourceAsStream("messages.properties")

      if(stream == null)
        throw new RuntimeException("Could not find json data file " + "messages.properties")

      val source = scala.io.Source.fromInputStream(stream)

      p.load(source.bufferedReader())
    }
    catch { case _: Exception => {} }
    p
  }
}