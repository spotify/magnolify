package magnolify.bigtable.semiauto

import com.google.bigtable.v2.Column
import com.google.bigtable.v2.Mutation.SetCell
import magnolia1._
import magnolify.bigtable.BigtableField
import magnolify.bigtable.BigtableField.Record
import magnolify.shared.{CaseMapper, Value}

import scala.annotation.implicitNotFound

object BigtableFieldDerivation {
  type Typeclass[T] = BigtableField[T]

  def join[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {
    private def key(prefix: String, label: String): String =
      if (prefix == null) label else s"$prefix.$label"

    override def get(xs: java.util.List[Column], k: String)(cm: CaseMapper): Value[T] = {
      var fallback = true
      val r = caseClass.construct { p =>
        val cq = key(k, cm.map(p.label))
        val v = p.typeclass.get(xs, cq)(cm)
        if (v.isSome) {
          fallback = false
        }
        v.getOrElse(p.default)
      }
      // result is default if all fields are default
      if (fallback) Value.Default(r) else Value.Some(r)
    }

    override def put(k: String, v: T)(cm: CaseMapper): Seq[SetCell.Builder] =
      caseClass.parameters.flatMap(p =>
        p.typeclass.put(key(k, cm.map(p.label)), p.dereference(v))(cm)
      )
  }

  @implicitNotFound("Cannot derive BigtableField for sealed trait")
  private sealed trait Dispatchable[T]
  def split[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Record[T] = ???

  implicit def apply[T]: Record[T] = macro Magnolia.gen[T]
}
