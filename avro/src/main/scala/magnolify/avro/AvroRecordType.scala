package magnolify.avro

import magnolia._
import org.apache.avro.Schema
import org.apache.avro.Schema.Field
import org.apache.avro.generic.{GenericArray, GenericData, GenericRecord}

import scala.collection.JavaConverters._
import scala.language.experimental.macros
import java.util.{List => JList}

trait AvroType[T] {
  type FromAvroRepr
  type ToAvroRepr

  // Since GenericRecord.get only returns Any
  final def fromAny(r: Any): T = from(r.asInstanceOf[FromAvroRepr])
  def from(r: FromAvroRepr): T
  def to(t: T): ToAvroRepr
  def schema: Schema
}

object AvroType {
  trait Passthrough[T] extends Aux2[T, T]
  trait Aux2[T, AvroRepr] extends Aux3[T, AvroRepr, AvroRepr]
  trait Aux3[T, _FromAvroRepr, _ToAvroReprRepr] extends AvroType[T] {
    override type FromAvroRepr = _FromAvroRepr
    override type ToAvroRepr = _ToAvroReprRepr
  }

  object Aux {
    // Primitive/identity fn
    def apply[T](_schema: Schema): AvroType[T] = new AvroType.Passthrough[T] {
      override def from(r: T): T = r
      override def to(t: T): T = t
      override def schema: Schema = _schema
    }
  }

  object Aux2 {
    def apply[T, AvroRepr](
                            _schema: Schema,
                            _from: AvroRepr => T,
                            _to: T => AvroRepr
                          ): AvroType[T] = new AvroType.Aux2[T, AvroRepr] {
      override def from(r: AvroRepr): T = _from(r)
      override def to(t: T): AvroRepr = _to(t)
      override def schema: Schema = _schema
    }
  }

  object Aux3 {
    def apply[T, FromAvroRepr, ToAvroRepr](
                                            _schema: Schema,
                                            _from: FromAvroRepr => T,
                                            _to: T => ToAvroRepr
                                          ): AvroType[T] = new Aux3[T, FromAvroRepr, ToAvroRepr] {
      override def from(r: FromAvroRepr): T = _from(r)
      override def to(t: T): ToAvroRepr = _to(t)
      override def schema: Schema = _schema
    }
  }
}

// @Todo support nullable, repeated, map, bytes, etc.....
trait Implicits {
  implicit val stringType: AvroType[String] =
    AvroType.Aux3[String, CharSequence, String](Schema.create(Schema.Type.STRING), _.toString, identity)

  implicit val intType: AvroType[Int] =
    AvroType.Aux[Int](Schema.create(Schema.Type.INT))

  implicit def listType[T : AvroType]: AvroType[List[T]] = {
    val m = implicitly[AvroType[T]]

    new AvroType[List[T]] {
      override type FromAvroRepr = JList[m.FromAvroRepr]
      override type ToAvroRepr = GenericArray[m.ToAvroRepr]

      override def from(r: FromAvroRepr): List[T] =
        r.asScala.map(m.from).toList

      override def to(t: List[T]): ToAvroRepr =
        new GenericData.Array(schema, t.map(m.to).asJava)

      override def schema: Schema = Schema.createArray(m.schema)
    }
  }
}

object AvroRecordType extends Implicits {
  type Typeclass[T] = AvroType[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): AvroType.Aux2[T, GenericRecord] = new AvroType.Aux2[T, GenericRecord] {
    def schema: Schema = {
      val fields = caseClass.parameters.map { param =>
        val fieldSchema = param.typeclass.schema
        new Field(param.label, fieldSchema, "", null)
      }

      Schema.createRecord(
        caseClass.typeName.short,
        "",
        "magnolify.avro",
        false,
        fields.asJava)
    }

    override def from(r: GenericRecord): T = caseClass.construct { p =>
      p.typeclass.fromAny(r.get(p.label))
    }

    override def to(t: T): GenericRecord = {
      val gr = new GenericData.Record(schema)
      caseClass.parameters.foreach { p =>
        gr.put(p.label, p.typeclass.to(p.dereference(t)))
      }
      gr
    }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???
  implicit def gen[T]: AvroType.Aux2[T, GenericRecord] = macro Magnolia.gen[CC]
}
