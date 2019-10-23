package magnolia.bigquery.semiauto

import com.google.api.services.bigquery.model.TableRow
import com.google.common.io.BaseEncoding
import magnolia._
import magnolia.shims._
import org.joda.time._

import scala.collection.JavaConverters._
import scala.language.experimental.macros

trait TableRowType[T] extends Serializable {
  def empty: TableRow = new TableRow
  def apply(m: TableRow): T = ???
  def apply(v: T): TableRow = ???
}

object TableRowType {
  type Typeclass[T] = TableRowMappable[T]
  type M = TableRow

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def apply(m: M): T =
      caseClass.construct { p =>
        p.typeclass.get(m, p.label)
      }

    override def apply(v: T): M =
      caseClass.parameters.foldLeft(this.empty) { (m, p) =>
        p.typeclass.put(m, p.label, p.dereference(v))
        m
      }

    override def fromLeaf(v: Any): T = {
      // nested records
      val tr = new TableRow()
      tr.putAll(v.asInstanceOf[java.util.Map[String, Any]])
      this.apply(tr)
    }
    override def toLeaf(v: T): Any = this.apply(v)
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def apply[T]: TableRowType[T] = macro Magnolia.gen[T]

}

trait TableRowMappable[V] extends TableRowType[V] { self =>
  type M = TableRow
  def fromLeaf(v: Any): V
  def toLeaf(v: V): Any
  def get(m: M, k: String): V = fromLeaf(m.get(k))
  def put(m: M, k: String, v: V): Unit = m.put(k, toLeaf(v))

  def imap[U](f: V => U)(g: U => V): TableRowMappable[U] = new TableRowMappable[U] {
    override def fromLeaf(v: Any): U = f(self.fromLeaf(v))
    override def toLeaf(v: U): Any = self.toLeaf(g(v))
  }
}

object TableRowMappable {
  def apply[V](implicit m: TableRowMappable[V]): TableRowMappable[V] = m

  def at[V](fromFn: Any => V)(toFn: V => Any): TableRowMappable[V] = new TableRowMappable[V] {
    override def fromLeaf(v: Any): V = fromFn(v)
    override def toLeaf(v: V): Any = toFn(v)
  }

  implicit val trmBool = at[Boolean](_.toString.toBoolean)(identity)
  implicit val trmInt = at[Int](_.toString.toInt)(identity)
  implicit val trmLong = at[Long](_.toString.toLong)(identity)
  implicit val trmFloat = at[Float](_.toString.toFloat)(identity)
  implicit val trmDouble = at[Double](_.toString.toDouble)(identity)
  implicit val trmString = at[String](_.toString)(identity)
  implicit val trmByteArray = at[Array[Byte]](
    x => BaseEncoding.base64().decode(x.toString))(
    x => BaseEncoding.base64().encode(x))

  import TimestampConverter._
  implicit val timestampBigQueryMappableType = at(toInstant)(fromInstant)
  implicit val localDateBigQueryMappableType = at(toLocalDate)(fromLocalDate)
  implicit val localTimeBigQueryMappableType = at(toLocalTime)(fromLocalTime)
  implicit val localDateTimeBigQueryMappableType = at(toLocalDateTime)(fromLocalDateTime)

  implicit def optionalTableRowM[V](implicit trm: TableRowMappable[V])
  : TableRowMappable[Option[V]] =
    new TableRowMappable[Option[V]] {
      override def fromLeaf(v: Any): Option[V] = ???
      override def toLeaf(v: Option[V]): Any = ???
      override def get(m: M, k: String): Option[V] =
        Option(m.get(k)).map(trm.fromLeaf)
      override def put(m: M, k: String, v: Option[V]): Unit =
        v.foreach(x => m.put(k, trm.toLeaf(x)))
    }

  implicit def seqTableRowM[V, S[V]](implicit trm: TableRowMappable[V],
                                     toSeq: S[V] => Seq[V],
                                     fc: FactoryCompat[V, S[V]]): TableRowMappable[S[V]] =
    new TableRowMappable[S[V]] {
      override def fromLeaf(v: Any): S[V] = ???
      override def toLeaf(v: S[V]): Any = ???
      override def get(m: M, k: String): S[V] = m.get(k) match {
        case null => fc.newBuilder.result()
        case xs: java.util.List[Any] => fc.build(xs.asScala.iterator.map(trm.fromLeaf))
      }
      override def put(m: M, k: String, v: S[V]): Unit =
        m.put(k, toSeq(v).map(trm.toLeaf).asJava)
    }
}
