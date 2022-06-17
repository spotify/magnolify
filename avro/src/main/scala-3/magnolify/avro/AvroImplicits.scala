package magnolify.avro

import magnolify.avro.AvroField.{aux, aux2, id}
import magnolify.shared.CaseMapper
import org.apache.avro.{JsonProperties, LogicalType, LogicalTypes, Schema}
import org.apache.avro.generic.{GenericArray, GenericData}

import java.nio.ByteBuffer
import java.util.UUID
import java.time.LocalDate
import scala.collection.Factory
import scala.jdk.CollectionConverters.*

trait AvroImplicits:

  given [T](using AvroField.Record[T]): AvroType[T] = AvroType[T]

  given AvroField[Boolean] = AvroField.afBoolean
  given AvroField[Int] = AvroField.afInt
  given AvroField[Long] = AvroField.afLong
  given AvroField[Float] = AvroField.afFloat
  given AvroField[Double] = AvroField.afDouble
  given AvroField[String] = AvroField.afString
  given AvroField[Unit] = AvroField.afUnit
  given AvroField[Array[Byte]] = AvroField.afBytes
  given [T](using f: AvroField[T]): AvroField[Option[T]] = AvroField.afOption
//  given [T, C[_] <: Iterable[_]](using f: AvroField[T], fc: Factory[T, C[T]]): AvroField[C[T]] = AvroField.afIterable
  given [T](using f: AvroField[T]): AvroField[Map[String, T]] = AvroField.afMap

  given AvroField[UUID] = AvroField.afUuid
  given AvroField[LocalDate] = AvroField.afDate

object AvroImplicits extends AvroImplicits
