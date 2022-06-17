package magnolify.avro.unsafe

import magnolify.avro.AvroField
import magnolify.avro.AvroImplicits.given

trait AvroUnsafeImplicits:
  given AvroField[Byte] = AvroField.from[Int](_.toByte)(_.toInt)
  given AvroField[Char] = AvroField.from[Int](_.toChar)(_.toInt)
  given AvroField[Short] = AvroField.from[Int](_.toShort)(_.toInt)
