package magnolify.cats.test

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.ADT._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object HashDerivationSpec extends MagnolifySpec("HashDerivation") {
  private def test[T: Arbitrary: ClassTag: Cogen: Hash]: Unit = {
    ensureSerializable(implicitly[Hash[T]])
    include(HashTests[T].hash.all, className[T] + ".")
  }

//  test[Integers]

  case class Integers(i: Int)

  val E = implicitly[Hash[Integers]]
  property("aaa") = Prop.forAll { (x: Integers, y: Integers) =>
    val r = (E.hash(x) == x.hashCode) &&
      (Hash.fromUniversalHashCode[Integers].hash(x) == x.hashCode()) &&
      (E.eqv(x, y) == Hash.fromUniversalHashCode[Integers].eqv(x, y))
    if (!r) {
      println("=" * 80)
      println(x)
      println(y)
      println(E.hash(x) == x.hashCode, E.hash(x), x.hashCode)
      println(Hash.fromUniversalHashCode[Integers].hash(x) == x.hashCode())
      println(E.eqv(x, y) == Hash.fromUniversalHashCode[Integers].eqv(x, y))
    }
    r
  }
}
