package magnolify.bigtable

import com.google.protobuf.ByteString
import magnolify.bigtable.BigtableField

import java.util.UUID

trait BigtableImplicits:
  given [T: BigtableField.Record]: BigtableType[T] = BigtableType[T]

  given BigtableField.Primitive[Byte] = BigtableField.btfByte
  given BigtableField.Primitive[Char] = BigtableField.btChar
  given BigtableField.Primitive[Short] = BigtableField.btfShort
  given BigtableField.Primitive[Int] = BigtableField.btfInt
  given BigtableField.Primitive[Long] = BigtableField.btfLong
  given BigtableField.Primitive[Float] = BigtableField.btfFloat
  given BigtableField.Primitive[Double] = BigtableField.btfDouble
  given BigtableField.Primitive[Boolean] = BigtableField.btfBoolean
  given BigtableField.Primitive[UUID] = BigtableField.btfUUID
  given BigtableField.Primitive[ByteString] = BigtableField.btfByteString
  given BigtableField.Primitive[Array[Byte]] = BigtableField.btfByteArray
  given BigtableField.Primitive[String] = BigtableField.btfString

//  implicit def btfEnum[T](implicit et: EnumType[T], lp: shapeless.LowPriority): Primitive[T] =
//  implicit def btfUnsafeEnum[T](implicit
//                                et: EnumType[T],
//                                lp: shapeless.LowPriority
//                               ): Primitive[UnsafeEnum[T]] =

  given BigtableField.Primitive[BigInt] = BigtableField.btfBigInt
  given BigtableField.Primitive[BigDecimal] = BigtableField.btfBigDecimal
  given [T: BigtableField]: BigtableField[Option[T]] = BigtableField.btfOption[T]
//  implicit def btfIterable[T, C[T]](implicit
//                                    btf: Primitive[T],
//                                    ti: C[T] => Iterable[T],
//                                    fc: FactoryCompat[T, C[T]]
//                                   ): Primitive[C[T]] =

object BigtableImplicits extends BigtableImplicits
