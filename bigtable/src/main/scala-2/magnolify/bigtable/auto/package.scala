package magnolify.bigtable

import magnolify.bigtable.auto.BigtableMacros

package object auto extends AutoDerivation with BigtableImplicits

trait AutoDerivation {
  implicit def genBigtableField[T]: BigtableField.Record[T] =
    macro BigtableMacros.genBigtableFielddMacro[T]
}
