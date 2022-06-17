package magnolify.bigquery.auto

import scala.language.experimental.macros
import scala.reflect.macros._

object BigQueryMacros {
  def genTableRowFieldMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.bigquery.semiauto.TableRowFieldDerivation.apply[$wtt].asInstanceOf[TableRowField.Record[$wtt]]"""
  }
}
