package magnolify.bigtable.auto

import scala.language.experimental.macros
import scala.reflect.macros._

object BigtableMacros {
  def genBigtableFielddMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.bigtable.semiauto.BigtableFieldDerivation.apply[$wtt].asInstanceOf[BigtableField.Record[$wtt]]"""
  }
}
