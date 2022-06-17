package magnolify.bigquery

import magnolify.bigquery.semiauto.TableRowFieldDerivation

import scala.deriving.Mirror

package object auto extends AutoDerivation with BigQueryImplicits

trait AutoDerivation:
  inline given autoDerivedTableRowField[T](using Mirror.Of[T]): TableRowField.Record[T] =
    TableRowFieldDerivation[T]
