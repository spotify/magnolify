package magnolia.bigquery

import scala.language.experimental.macros
import scala.reflect.macros._

package object auto {
  type TableRowType[T] = semiauto.TableRowType[T]
  val TableRowType = semiauto.TableRowType
  type TableRowMappable[T] = semiauto.TableRowMappable[T]
  val TableRowMappable = semiauto.TableRowMappable

  def genTableRowTypeMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.bigquery.semiauto.TableRowType.apply[$wtt]"""
  }

  implicit def genTableRowType[T]: TableRowType[T] = macro genTableRowTypeMacro[T]
}
