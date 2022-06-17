package magnolify.avro

import magnolify.avro.semiauto.AvroFieldDerivation
import magnolify.avro.auto.AvroMacros
import magnolify.avro.AvroImplicits

package object auto extends AutoDerivation with AvroImplicits

trait AutoDerivation {
  implicit def genAvroField[T]: AvroField.Record[T] = macro AvroMacros.genTableRowFieldMacro[T]
}
