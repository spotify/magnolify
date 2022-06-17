package magnolify.avro.auto

import scala.language.experimental.macros
import scala.reflect.macros._

object AvroMacros {
  def genAvroFieldMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.avro.semiauto.AvroFieldDerivation.apply[$wtt].asInstanceOf[AvroField.Record[$wtt]]"""
  }
}
