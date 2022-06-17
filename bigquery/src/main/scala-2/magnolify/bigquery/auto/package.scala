package magnolify.bigquery

import magnolify.bigquery.auto.BigQueryMacros

package object auto extends AutoDerivation with BigQueryImplicits

trait AutoDerivation {
  implicit def genTableRowField[T]: TableRowField.Record[T] =
    macro BigQueryMacros.genTableRowFieldMacro[T]
}
