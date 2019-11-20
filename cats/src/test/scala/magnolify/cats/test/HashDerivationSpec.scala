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
  test[Int]
  test[Long]
  implicitly[Arbitrary[Integers]]
  implicitly[Cogen[Integers]]
  magnolify.cats.semiauto.HashDerivation[Integers]
//  implicitly[Hash[Integers]]
//  test[Integers]
}
