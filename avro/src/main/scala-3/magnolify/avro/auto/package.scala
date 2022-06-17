package magnolify.avro

import magnolify.avro.semiauto.AvroFieldDerivation

import scala.deriving.Mirror

package object auto extends AutoDerivation with AvroImplicits

trait AutoDerivation:
  inline given autoDerivedAvroField[T](using Mirror.Of[T]): AvroField.Record[T] =
    AvroFieldDerivation[T]
