package magnolify.avro.semiauto

import magnolia1.*
import magnolify.avro.{doc, AvroField, AvroType}
import magnolify.shared.CaseMapper
import org.apache.avro.generic.*
import org.apache.avro.{JsonProperties, LogicalType, LogicalTypes, Schema}

import scala.collection.concurrent
import scala.jdk.CollectionConverters.*
import scala.deriving.Mirror
import java.util.UUID

object AvroFieldDerivation extends ProductDerivation[AvroField]:

  def join[T](caseClass: CaseClass[AvroField, T]): AvroField[T] = new AvroField.Record[T]:
    override protected def buildSchema(cm: CaseMapper): Schema = Schema
      .createRecord(
        caseClass.typeInfo.short,
        getDoc(caseClass.annotations, caseClass.typeInfo.full),
        caseClass.typeInfo.owner,
        false,
        caseClass.params
          .map { p =>
            new Schema.Field(
              cm.map(p.label),
              p.typeclass.schema(cm),
              getDoc(p.annotations, s"${caseClass.typeInfo.full}#${p.label}"),
              p.default
                .map(d => p.typeclass.makeDefault(d)(cm))
                .getOrElse(p.typeclass.fallbackDefault)
            )
          }
          .toList
          .asJava
      )

    // `JacksonUtils.toJson` expects `Map[String, AnyRef]` for `RECORD` defaults
    override def makeDefault(d: T)(cm: CaseMapper): java.util.Map[String, Any] =
      caseClass.params
        .map { p =>
          val name = cm.map(p.label)
          val value = p.typeclass.makeDefault(p.deref(d))(cm)
          name -> value
        }
        .toMap
        .asJava

    override def from(v: GenericRecord)(cm: CaseMapper): T =
      caseClass.construct { p =>
        p.typeclass.fromAny(v.get(p.index))(cm)
      }

    override def to(v: T)(cm: CaseMapper): GenericRecord =
      caseClass.params
        .foldLeft(new GenericData.Record(schema(cm))) { (r, p) =>
          r.put(p.index, p.typeclass.to(p.deref(v))(cm))
          r
        }

  private def getDoc(annotations: Seq[Any], name: String): String = {
    val docs = annotations.collect { case d: doc => d.toString }
    require(docs.size <= 1, s"More than one @doc annotation: $name")
    docs.headOption.orNull
  }

  // ProductDerivation can be specialized to an AvroField.Record
  inline given apply[T](using mirror: Mirror.Of[T]): AvroField.Record[T] =
    derived[T].asInstanceOf[AvroField.Record[T]]
