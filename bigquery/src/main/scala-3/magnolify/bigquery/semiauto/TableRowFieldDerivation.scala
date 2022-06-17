package magnolify.bigquery.semiauto

import com.google.api.services.bigquery.model.{TableFieldSchema, TableRow}
import magnolia1.*
import magnolify.bigquery.TableRowField.Record
import magnolify.bigquery.{description, TableRowField}
import magnolify.shared.CaseMapper

import scala.annotation.implicitNotFound
import scala.deriving.Mirror
import scala.jdk.CollectionConverters._

object TableRowFieldDerivation extends ProductDerivation[TableRowField]:

  def join[T](caseClass: CaseClass[TableRowField, T]): TableRowField.Record[T] =
    new TableRowField.Record[T] {
      override protected def buildSchema(cm: CaseMapper): TableFieldSchema = {
        // do not use a scala wrapper in the schema, so clone() works
        val fields = new java.util.ArrayList[TableFieldSchema](caseClass.params.size)
        caseClass.params.foreach { p =>
          val f = p.typeclass
            .fieldSchema(cm)
            .clone()
            .setName(cm.map(p.label))
            .setDescription(getDescription(p.annotations, s"${caseClass.typeInfo.full}#${p.label}"))
          fields.add(f)
        }

        new TableFieldSchema()
          .setType("STRUCT")
          .setMode("REQUIRED")
          .setDescription(getDescription(caseClass.annotations, caseClass.typeInfo.full))
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
        caseClass.params.foldLeft(new TableRow) { (tr, p) =>
          val f = p.typeclass.to(p.deref(v))(cm)
          if (f != null) {
            tr.put(cm.map(p.label), f)
          }
          tr
        }

      private def getDescription(annotations: Seq[Any], name: String): String = {
        val descs = annotations.collect { case d: description => d.toString }
        require(descs.size <= 1, s"More than one @description annotation: $name")
        descs.headOption.orNull
      }
    }

  inline given apply[T](using Mirror.Of[T]): TableRowField.Record[T] =
    derived[T].asInstanceOf[TableRowField.Record[T]]
