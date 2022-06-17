package magnolify.bigtable

import magnolify.bigtable.semiauto.BigtableFieldDerivation

import scala.deriving.Mirror

package object auto extends AutoDerivation with BigtableImplicits

trait AutoDerivation {
  inline given autoDerivedBigtableField[T](using Mirror.Of[T]): BigtableField.Record[T] =
    BigtableFieldDerivation[T]
}
