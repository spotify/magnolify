package magnolify.bigtable.test

import cats.Eq
import com.google.protobuf.ByteString
import magnolify.bigtable.BigtableType
import magnolify.test.MagnolifySuite
import org.scalacheck.{Arbitrary, Prop}

import scala.reflect.ClassTag

trait BigtableBaseSuite extends MagnolifySuite {
  private[magnolify] def test[T: Arbitrary: ClassTag](implicit t: BigtableType[T], eq: Eq[T]): Unit = {
    val tpe = ensureSerializable(t)
    property(className[T]) {
      Prop.forAll { t: T =>
        val mutations = tpe(t, "cf")
        val row = BigtableType.mutationsToRow(ByteString.EMPTY, mutations)
        val copy = tpe(row, "cf")
        val rowCopy =
          BigtableType.mutationsToRow(ByteString.EMPTY, BigtableType.rowToMutations(row))

        Prop.all(
          eq.eqv(t, copy),
          row == rowCopy
        )
      }
    }
  }

}
