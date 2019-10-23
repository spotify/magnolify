package magnolia.datastore

import java.time.{Duration, Instant}

import com.google.datastore.v1._
import com.google.datastore.v1.client.DatastoreHelper.makeValue
import com.google.protobuf.{ByteString, Timestamp}
import magnolia._
import magnolia.data.Converter
import magnolia.shims.FactoryCompat

import scala.collection.JavaConverters._
import scala.language.experimental.macros

trait EntityType[T] extends Converter.Record[T, EntityOrBuilder] {
  protected type R = EntityOrBuilder
  def apply(r: R): T = from(r)
  def apply(t: T): R = to(t)
  override protected def empty: R = Entity.newBuilder()
  override protected def from(r: R): T = ???
  override protected def to(v: T): R = ???
}

object EntityType {
  type Typeclass[T] = EntityField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override protected def from(r: R): T =
      caseClass.construct(p => p.typeclass.get(r, p.label))

    override protected def to(t: T): R =
      caseClass.parameters.foldLeft(this.empty) { (m, p) =>
        p.typeclass.put(m, p.label, p.dereference(t))
        m
      }

    override def fromField(v: ValueOrBuilder): T =
      this.from(v.getEntityValue)
    override def toField(v: T): ValueOrBuilder =
      makeValue(this.to(v).asInstanceOf[Entity.Builder])
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def apply[T]: EntityType[T] = macro Magnolia.gen[T]
}

trait EntityField[V]
  extends EntityType[V]
  with Converter.Field[V, EntityOrBuilder] { self =>
  override def get(r: R, k: String): V = fromField(r.getPropertiesMap.get(k))
  override def put(r: R, k: String, v: V): Unit =
    r.asInstanceOf[Entity.Builder].putProperties(
      k, toField(v).asInstanceOf[Value.Builder].build())

  def fromField(v: ValueOrBuilder): V
  def toField(v: V): ValueOrBuilder

  def imap[U](f: V => U)(g: U => V): EntityField[U] = new EntityField[U] {
    override def fromField(v: ValueOrBuilder): U = f(self.fromField(v))
    override def toField(v: U): ValueOrBuilder = self.toField(g(v))
  }
}

object EntityField {
  def apply[V](implicit f: EntityField[V]): EntityField[V] = f

  def at[V](f: ValueOrBuilder => V)(g: V => ValueOrBuilder): EntityField[V] = new EntityField[V] {
    override def fromField(v: ValueOrBuilder): V = f(v)
    override def toField(v: V): ValueOrBuilder = g(v)
  }

  implicit val efBool = at[Boolean](_.getBooleanValue)(makeValue)
  implicit val efLong = at[Long](_.getIntegerValue)(makeValue)
  implicit val efDouble = at[Double](_.getDoubleValue)(makeValue)
  implicit val efString = at[String](_.getStringValue)(makeValue)
  implicit val efByteString = at[ByteString](_.getBlobValue)(makeValue)
  implicit val efByteArray = at[Array[Byte]](_.getBlobValue.toByteArray)(
    v => makeValue(ByteString.copyFrom(v)))
  implicit val efTimestamp = at(toInstant)(fromInstant)

  private val millisPerSecond = Duration.ofSeconds(1).toMillis
  private def toInstant(v: ValueOrBuilder): Instant = {
    val t = v.getTimestampValue
    Instant.ofEpochMilli(t.getSeconds * millisPerSecond + t.getNanos / 1000000)
  }
  private def fromInstant(i: Instant): ValueOrBuilder = {
    val t = Timestamp.newBuilder()
      .setSeconds(i.toEpochMilli / millisPerSecond)
      .setNanos((i.toEpochMilli % 1000).toInt * 1000000)
    Value.newBuilder().setTimestampValue(t)
  }

  implicit def efOption[V](implicit f: EntityField[V]): EntityField[Option[V]] =
    new EntityField[Option[V]] {
      override def fromField(v: ValueOrBuilder): Option[V] = ???
      override def toField(v: Option[V]): ValueOrBuilder = ???
      override def get(r: R, k: String): Option[V] =
        Option(r.getPropertiesMap.get(k)).map(f.fromField)
      override def put(r: R, k: String, v: Option[V]): Unit =
        v.foreach(x => r.asInstanceOf[Entity.Builder].putProperties(
          k, f.toField(x).asInstanceOf[Value.Builder].build()))
    }

  implicit def efSeq[V, S[V]](implicit f: EntityField[V],
                              toSeq: S[V] => Seq[V],
                              fc: FactoryCompat[V, S[V]]): EntityField[S[V]] =
    new EntityField[S[V]] {
      override def fromField(v: ValueOrBuilder): S[V] = ???
      override def toField(v: S[V]): ValueOrBuilder = ???
      override def get(r: R, k: String): S[V] = r.getPropertiesMap.get(k) match {
        case null => fc.newBuilder.result()
        case xs => fc.build(xs.getArrayValue.getValuesList.asScala.iterator.map(f.fromField))
      }
      override def put(r: R, k: String, v: S[V]): Unit =
        r.asInstanceOf[Entity.Builder].putProperties(k, Value.newBuilder().setArrayValue(
          toSeq(v).foldLeft(ArrayValue.newBuilder()) { (b, x) =>
            b.addValues(f.toField(x).asInstanceOf[Value.Builder])
          }.build()).build())
    }
}
