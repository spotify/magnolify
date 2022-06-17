package magnolify.bigtable

import com.google.protobuf.ByteString
import magnolify.bigtable.BigtableField

import java.util.UUID

trait BigtableImplicits {
  implicit def bigtableType[T: BigtableField.Record]: BigtableType[T] = BigtableType[T]

  implicit val btfByte: BigtableField.Primitive[Byte] = BigtableField.btfByte
  implicit val btChar: BigtableField.Primitive[Char] = BigtableField.btChar
  implicit val btfShort: BigtableField.Primitive[Short] = BigtableField.btfShort
  implicit val btfInt: BigtableField.Primitive[Int] = BigtableField.btfInt
  implicit val btfLong: BigtableField.Primitive[Long] = BigtableField.btfLong
  implicit val btfFloat: BigtableField.Primitive[Float] = BigtableField.btfFloat
  implicit val btfDouble: BigtableField.Primitive[Double] = BigtableField.btfDouble
  implicit val btfBoolean: BigtableField.Primitive[Boolean] = BigtableField.btfBoolean
  implicit val btfUUID: BigtableField.Primitive[UUID] = BigtableField.btfUUID
  implicit val btfByteString: BigtableField.Primitive[ByteString] = BigtableField.btfByteString
  implicit val btfByteArray: BigtableField.Primitive[Array[Byte]] = BigtableField.btfByteArray
  implicit val btfString: BigtableField.Primitive[String] = BigtableField.btfString

//  implicit def btfEnum[T](implicit et: EnumType[T], lp: shapeless.LowPriority): BigtableField.Primitive[T] =
//  implicit def btfUnsafeEnum[T](implicit
//                                et: EnumType[T],
//                                lp: shapeless.LowPriority
//                               ): BigtableField.Primitive[UnsafeEnum[T]] =

  implicit val btfBigInt: BigtableField.Primitive[BigInt] = BigtableField.btfBigInt
  implicit val btfBigDecimal: BigtableField.Primitive[BigDecimal] = BigtableField.btfBigDecimal
  implicit def btfOption[T: BigtableField]: BigtableField[Option[T]] = BigtableField.btfOption
//  implicit def btfIterable[T, C[T]](implicit
//                                    btf: BigtableField.Primitive[T],
//                                    ti: C[T] => Iterable[T],
//                                    fc: FactoryCompat[T, C[T]]
//                                   ): BigtableField[C[T]] =
}

object BigtableImplicits extends BigtableImplicits
