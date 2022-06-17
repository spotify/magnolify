package magnolify.avro

import magnolify.avro.AvroField.{aux, aux2, id}
import magnolify.shared.CaseMapper
import magnolify.shims.FactoryCompat
import org.apache.avro.{JsonProperties, LogicalType, LogicalTypes, Schema}
import org.apache.avro.generic.{GenericArray, GenericData}

import java.nio.ByteBuffer
import java.util.UUID
import java.time.LocalDate

trait AvroImplicits {

  implicit def avroType[T: AvroField.Record]: AvroType[T] = AvroType[T]

  implicit val afBoolean: AvroField[Boolean] = AvroField.afBoolean
  implicit val afInt: AvroField[Int] = AvroField.afInt
  implicit val afLong: AvroField[Long] = AvroField.afLong
  implicit val afFloat: AvroField[Float] = AvroField.afFloat
  implicit val afDouble: AvroField[Double] = AvroField.afDouble
  implicit val afString: AvroField[String] = AvroField.afString
  implicit val afUnit: AvroField[Unit] = AvroField.afUnit
  implicit val afBytes: AvroField[Array[Byte]] = AvroField.afBytes

//  implicit def afEnum[T](implicit et: EnumType[T]): AvroField[T] = AvroField.afEnum

  implicit def afOption[T](implicit f: AvroField[T]): AvroField[Option[T]] = AvroField.afOption
//  implicit def afIterable[T, C[_]](implicit
//    f: AvroField[T],
//    ti: C[T] => Iterable[T],
//    fc: FactoryCompat[T, C[T]]
//  ): AvroField[C[T]] = AvroField.afIterable

  implicit def afMap[T](implicit f: AvroField[T]): AvroField[Map[String, T]] = AvroField.afMap

  implicit val afUuid: AvroField[UUID] = AvroField.afUuid
  implicit val afDate: AvroField[LocalDate] = AvroField.afDate
}

object AvroImplicits extends AvroImplicits
