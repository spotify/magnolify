package magnolia.bigquery

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableRow
import com.google.api.services.bigquery.model.TableSchema
import com.google.common.io.BaseEncoding
import magnolia._
import magnolia.data.Converter
import magnolia.shims._

import scala.collection.JavaConverters._
import scala.language.experimental.macros

sealed trait TableRowType[T] extends Converter.Record[T, TableRow, TableRow] {
  def apply(r: TableRow): T = from(r)
  def apply(t: T): TableRow = to(t)
  def schema: TableSchema = ???
  override protected def empty: TableRow = new TableRow
  override def from(r: TableRow): T = ???
  override def to(t: T): TableRow = ???
}

object TableRowType {
  type Typeclass[T] = TableRowField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def schema: TableSchema = new TableSchema().setFields(
      caseClass.parameters.map(p => p.typeclass.fieldSchema.setName(p.label)).asJava)

    override def from(r: TableRow): T =
      caseClass.construct(p => p.typeclass.get(r, p.label))

    override def to(t: T): TableRow =
      caseClass.parameters.foldLeft(empty) { (r, p) =>
        p.typeclass.put(r, p.label, p.dereference(t))
      }

    override def fieldSchema: TableFieldSchema =
      new TableFieldSchema().setType("STRUCT").setMode("REQUIRED")
    override def fromField(v: Any): T = {
      val r = empty
      r.putAll(v.asInstanceOf[java.util.Map[String, Any]])
      this.from(r)
    }
    override def toField(v: T): Any = to(v)
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def apply[T]: TableRowType[T] = macro Magnolia.gen[T]
}

sealed trait TableRowField[V]
  extends TableRowType[V]
  with Converter.Field[V, TableRow, TableRow] { self =>
  override def get(r: TableRow, k: String): V = fromField(r.get(k))
  override def put(r: TableRow, k: String, v: V): TableRow = {
    r.put(k, toField(v))
    r
  }

  def fieldSchema: TableFieldSchema
  def fromField(v: Any): V
  def toField(v: V): Any

  def imap[U](f: V => U)(g: U => V): TableRowField[U] = new TableRowField[U] {
    override def fieldSchema: TableFieldSchema = self.fieldSchema
    override def fromField(v: Any): U = f(self.fromField(v))
    override def toField(v: U): Any = self.toField(g(v))
  }
}

object TableRowField {
  def apply[V](implicit f: TableRowField[V]): TableRowField[V] = f

  def at[V](tpe: String)(f: Any => V)(g: V => Any): TableRowField[V] = new TableRowField[V] {
    override def fieldSchema: TableFieldSchema =
      new TableFieldSchema().setType(tpe).setMode("REQUIRED")
    override def fromField(v: Any): V = f(v)
    override def toField(v: V): Any = g(v)
  }

  implicit val trfBool = at[Boolean]("BOOL")(_.toString.toBoolean)(identity)
  implicit val trfLong = at[Long]("INT64")(_.toString.toLong)(identity)
  implicit val trfDouble = at[Double]("FLOAT64")(_.toString.toDouble)(identity)
  implicit val trfString = at[String]("STRING")(_.toString)(identity)
  implicit val trfNumeric =
    at[BigDecimal]("NUMERIC")(NumericConverter.toBigDecimal)(NumericConverter.fromBigDecimal)

  implicit val trfByteArray = at[Array[Byte]]("BYTES")(
    x => BaseEncoding.base64().decode(x.toString))(
    x => BaseEncoding.base64().encode(x))

  import TimestampConverter._
  implicit val trfInstant = at("TIMESTAMP")(toInstant)(fromInstant)
  implicit val trfDate = at("DATE")(toLocalDate)(fromLocalDate)
  implicit val trfTime = at("TIME")(toLocalTime)(fromLocalTime)
  implicit val trfDateTime = at("DATETIME")(toLocalDateTime)(fromLocalDateTime)

  implicit def trfOption[V](implicit f: TableRowField[V]): TableRowField[Option[V]] =
    new TableRowField[Option[V]] {
      override def fieldSchema: TableFieldSchema = f.fieldSchema.setMode("NULLABLE")
      override def fromField(v: Any): Option[V] = ???
      override def toField(v: Option[V]): Any = ???
      override def get(r: TableRow, k: String): Option[V] =
        Option(r.get(k)).map(f.fromField)
      override def put(r: TableRow, k: String, v: Option[V]): TableRow = {
        v.foreach(x => r.put(k, f.toField(x)))
        r
      }
    }

  implicit def trfSeq[V, S[V]](implicit f: TableRowField[V],
                               ts: S[V] => Seq[V],
                               fc: FactoryCompat[V, S[V]]): TableRowField[S[V]] =
    new TableRowField[S[V]] {
      override def fieldSchema: TableFieldSchema = f.fieldSchema.setMode("REPEATED")
      override def fromField(v: Any): S[V] = ???
      override def toField(v: S[V]): Any = ???
      override def get(r: TableRow, k: String): S[V] = r.get(k) match {
        case null => fc.newBuilder.result()
        case xs: java.util.List[_] => fc.build(xs.asScala.iterator.map(f.fromField))
      }
      override def put(r: TableRow, k: String, v: S[V]): TableRow = {
        r.put(k, v.map(f.toField).asJava)
        r
      }
    }
}
