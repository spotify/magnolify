package magnolia.tensorflow

import com.google.protobuf.ByteString
import magnolia._
import magnolia.data.Converter
import magnolia.shims.FactoryCompat
import org.tensorflow.example._

import scala.collection.JavaConverters._
import scala.language.experimental.macros

sealed trait ExampleType[T] extends Converter.Record[T, FeaturesOrBuilder] {
  protected type R = FeaturesOrBuilder
  def apply(r: ExampleOrBuilder): T = from(r.getFeatures)
  def apply(t: T): Example = Example.newBuilder()
    .setFeatures(to(t).asInstanceOf[Features.Builder])
    .build()
  override protected def empty: R = Features.newBuilder()
  override protected def from(r: R): T = ???
  override protected def to(t: T): R = ???
}

object ExampleType {
  type Typeclass[T] = ExampleField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override protected def from(r: R): T =
      caseClass.construct(p => p.typeclass.get(r, p.label))

    override protected def to(t: T): R =
      caseClass.parameters.foldLeft(empty) { (r, p) =>
        p.typeclass.put(r, p.label, p.dereference(t))
        r
      }

    // FIXME: flatten nested fields
    override def fromField(v: Feature): T = ???
    override def toField(v: T): Feature.Builder = ???
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def apply[T]: ExampleType[T] = macro Magnolia.gen[T]
}

sealed trait ExampleField[V]
  extends ExampleType[V]
  with Converter.Field[V, FeaturesOrBuilder] { self =>
  override def get(r: FeaturesOrBuilder, k: String): V = fromField(r.getFeatureMap.get(k))
  override def put(r: FeaturesOrBuilder, k: String, v: V): Unit =
    r.asInstanceOf[Features.Builder].putFeature(k, toField(v).build())

  def fromField(v: Feature): V
  def toField(v: V): Feature.Builder

  def imap[U](f: V => U)(g: U => V): ExampleField[U] = new ExampleField[U] {
    override def fromField(v: Feature): U = f(self.fromField(v))
    override def toField(v: U): Feature.Builder = self.toField(g(v))
  }
}

object ExampleField extends LowPriorityExampleFieldAt {
  def apply[V](implicit f: ExampleField[V]): ExampleField[V] = f

  def at[V](f: Feature => V)(g: V => Feature.Builder): ExampleField[V] = new ExampleField[V] {
    override def fromField(v: Feature): V = f(v)
    override def toField(v: V): Feature.Builder = g(v)
  }

  sealed trait At[A, B] extends Serializable {
    def to(v: A): B
    def from(v: B): A
  }

  def atLong[V](f: Long => V)(g: V => Long): At[Long, V] = atField(f)(g)
  def atFloat[V](f: Float => V)(g: V => Float): At[Float, V] = atField(f)(g)
  def atByteString[V](f: ByteString => V)(g: V => ByteString): At[ByteString, V] = atField(f)(g)

  private def atField[A, B](f: A => B)(g: B => A): At[A, B] = new At[A, B] {
    override def to(v: A): B = f(v)
    override def from(v: B): A = g(v)
  }

  // Iterator is not Seq and avoids diverging implicit issues
  implicit val efLongIterator: ExampleField[Iterator[Long]] =
    at[Iterator[Long]](
      _.getInt64List.getValueList.asScala.iterator.asInstanceOf[Iterator[Long]])(
      x => Feature.newBuilder().setInt64List(Int64List.newBuilder().addAllValue(
        x.asInstanceOf[Iterator[java.lang.Long]].toIterable.asJava)))

  implicit val efFloatIterator: ExampleField[Iterator[Float]] =
    at[Iterator[Float]](
      _.getFloatList.getValueList.asScala.iterator.asInstanceOf[Iterator[Float]])(
      x => Feature.newBuilder().setFloatList(FloatList.newBuilder().addAllValue(
        x.asInstanceOf[Iterator[java.lang.Float]].toIterable.asJava)))

  implicit val efByteStringIterator: ExampleField[Iterator[ByteString]] =
    at[Iterator[ByteString]](
      _.getBytesList.getValueList.asScala.iterator)(
      x => Feature.newBuilder().setBytesList(BytesList.newBuilder().addAllValue(
        x.toIterable.asJava)))
}

trait LowPriorityExampleFieldAt extends LowPriorityExampleFieldSeq {
  import ExampleField.At

  // try to convert to V first
  implicit def efAtLong[V](implicit at: At[Long, V]): ExampleField[Iterator[V]] =
    ExampleField.efLongIterator.imap(_.map(at.to))(_.map(at.from))

  implicit def efAtFloat[V](implicit at: At[Float, V]): ExampleField[Iterator[V]] =
    ExampleField.efFloatIterator.imap(_.map(at.to))(_.map(at.from))

  implicit def efAtByteString[V](implicit at: At[ByteString, V]): ExampleField[Iterator[V]] =
    ExampleField.efByteStringIterator.imap(_.map(at.to))(_.map(at.from))
}

trait LowPriorityExampleFieldSeq extends LowPriorityExampleFieldOption {
  // upper bound instead of S[V] => Seq[V] to avoid diverging implicit issues with Array[V]
  implicit def efSeq[V, S[V] <: Seq[V]](implicit f: ExampleField[Iterator[V]],
                                        fc: FactoryCompat[V, S[V]]): ExampleField[S[V]] =
    f.imap(i => fc.build(i))(_.iterator)

  implicit def efArray[V](implicit f: ExampleField[Iterator[V]],
                          fc: FactoryCompat[V, Array[V]]): ExampleField[Array[V]] =
    f.imap(i => fc.build(i))(_.iterator)
}

trait LowPriorityExampleFieldOption extends LowPriorityExampleFieldSingle {
  implicit def efOption[V](implicit f: ExampleField[Iterator[V]]): ExampleField[Option[V]] =
    f.imap(v => if (v.hasNext) {
      val r = Some(v.next())
      require(!v.hasNext, "More than 1 value for Option field")
      r
    } else {
      None
    })(_.iterator)
}

trait LowPriorityExampleFieldSingle {
  implicit def efSingle[V](implicit f: ExampleField[Iterator[V]]): ExampleField[V] =
    f.imap(v => {
      require(v.hasNext, "Missing value for required field")
      val r = v.next()
      require(!v.hasNext, "More than 1 value for required field")
      r
    })(Iterator(_))
}
