package magnolify.bigquery.semiauto

import com.google.api.services.bigquery.model.{TableFieldSchema, TableRow}
import magnolia1._
import magnolify.bigquery.TableRowField.Record
import magnolify.bigquery.{description, TableRowField}
import magnolify.shared.CaseMapper

import scala.annotation.implicitNotFound
import scala.jdk.CollectionConverters._

object TableRowFieldDerivation extends {

  type Typeclass[T] = TableRowField[T]

  def join[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {
    override protected def buildSchema(cm: CaseMapper): TableFieldSchema = {
      // do not use a scala wrapper in the schema, so clone() works
      val fields = new java.util.ArrayList[TableFieldSchema](caseClass.parameters.size)
      caseClass.parameters.foreach { p =>
        val f = p.typeclass
          .fieldSchema(cm)
          .clone()
          .setName(cm.map(p.label))
          .setDescription(getDescription(p.annotations, s"${caseClass.typeName.full}#${p.label}"))
        fields.add(f)
      }

      new TableFieldSchema()
        .setType("STRUCT")
        .setMode("REQUIRED")
        .setDescription(getDescription(caseClass.annotations, caseClass.typeName.full))
        .setFields(fields)
    }

    override def from(v: java.util.Map[String, AnyRef])(cm: CaseMapper): T =
      caseClass.construct { p =>
        val f = v.get(cm.map(p.label))
        if (f == null && p.default.isDefined) {
          p.default.get
        } else {
          p.typeclass.fromAny(f)(cm)
        }
      }

    override def to(v: T)(cm: CaseMapper): TableRow =
      caseClass.parameters.foldLeft(new TableRow) { (tr, p) =>
        val f = p.typeclass.to(p.dereference(v))(cm)
        if (f == null) tr else tr.set(cm.map(p.label), f)
      }

    private def getDescription(annotations: Seq[Any], name: String): String = {
      val descs = annotations.collect { case d: description => d.toString }
      require(descs.size <= 1, s"More than one @description annotation: $name")
      descs.headOption.orNull
    }
  }

  @implicitNotFound("Cannot derive TableRowField for sealed trait")
  private sealed trait Dispatchable[T]
  def split[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Record[T] = ???

  implicit def apply[T]: Record[T] = macro Magnolia.gen[T]

}
