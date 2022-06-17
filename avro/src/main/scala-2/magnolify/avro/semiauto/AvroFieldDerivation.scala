package magnolify.avro.semiauto

import magnolia1._
import magnolify.avro.{doc, AvroField}
import magnolify.shared.CaseMapper
import magnolify.shims.FactoryCompat
import magnolify.shims.JavaConverters._
import org.apache.avro.generic._
import org.apache.avro.{JsonProperties, LogicalType, LogicalTypes, Schema}

import scala.annotation.implicitNotFound
import scala.collection.concurrent
import java.util.UUID

object AvroFieldDerivation {
  type Typeclass[T] = AvroField[T]

  def join[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new AvroField.Record[T] {
    override protected def buildSchema(cm: CaseMapper): Schema = Schema
      .createRecord(
        caseClass.typeName.short,
        getDoc(caseClass.annotations, caseClass.typeName.full),
        caseClass.typeName.owner,
        false,
        caseClass.parameters.map { p =>
          new Schema.Field(
            cm.map(p.label),
            p.typeclass.schema(cm),
            getDoc(p.annotations, s"${caseClass.typeName.full}#${p.label}"),
            p.default
              .map(d => p.typeclass.makeDefault(d)(cm))
              .getOrElse(p.typeclass.fallbackDefault)
          )
        }.asJava
      )

    // `JacksonUtils.toJson` expects `Map[String, Any]` for `RECORD` defaults
    override def makeDefault(d: T)(cm: CaseMapper): java.util.Map[String, Any] =
      caseClass.parameters
        .map { p =>
          val name = cm.map(p.label)
          val value = p.typeclass.makeDefault(p.dereference(d))(cm)
          name -> value
        }
        .toMap
        .asJava

    override def from(v: GenericRecord)(cm: CaseMapper): T =
      caseClass.construct { p =>
        p.typeclass.fromAny(v.get(p.index))(cm)
      }

    override def to(v: T)(cm: CaseMapper): GenericRecord =
      caseClass.parameters
        .foldLeft(new GenericData.Record(schema(cm))) { (r, p) =>
          r.put(p.index, p.typeclass.to(p.dereference(v))(cm))
          r
        }
  }

  private def getDoc(annotations: Seq[Any], name: String): String = {
    val docs = annotations.collect { case d: doc => d.toString }
    require(docs.size <= 1, s"More than one @doc annotation: $name")
    docs.headOption.orNull
  }

  @implicitNotFound("Cannot derive AvroField for sealed trait")
  private sealed trait Dispatchable[T]
  def split[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): AvroField.Record[T] = ???

  implicit def apply[T]: AvroField[T] = macro Magnolia.gen[T]
}
