package magnolify.avro.unsafe

import magnolify.avro.AvroField
import magnolify.avro.AvroImplicits._

trait AvroUnsafeImplicits {
  implicit val afByte: AvroField[Byte] = AvroField.from[Int](_.toByte)(_.toInt)
  implicit val afChar: AvroField[Char] = AvroField.from[Int](_.toChar)(_.toInt)
  implicit val afShort: AvroField[Short] = AvroField.from[Int](_.toShort)(_.toInt)

//  implicit def afUnsafeEnum[T](implicit et: EnumType[T]): AvroField[UnsafeEnum[T]] =
//    AvroField.from[String](UnsafeEnum.from(_))(UnsafeEnum.to(_))
}
