Type Class Derivation
=====================

Type class derivation for Cats, ScalaCheck, and Guava can be performed both automatically and semi-automatically.

Automatic derivation are provided as implicits through `import magnolify.$module.auto._`.  It works with context bounds i.e. `def plus[T: Semigroup](x: T, y: T)` and implicit parameters i.e. `def f[T](x: T, y: T)(implicit sg: Semigroup[T])`. The following examples summon them via `implicitly`.

```scala mdoc:reset
case class Inner(int: Int, str: String)
case class Outer(inner: Inner)

// Cats Semigroup
import magnolify.cats.auto._
import cats._

val sg: Semigroup[Outer] = implicitly[Semigroup[Outer]]
sg.combine(Outer(Inner(1, "hello, ")), Outer(Inner(100, "world!")))

// ScalaCheck Arbitrary
import magnolify.scalacheck.auto._
import org.scalacheck._

val arb: Arbitrary[Outer] = implicitly[Arbitrary[Outer]]
arb.arbitrary.sample

// Guava Funnel
import magnolify.guava.auto._
import com.google.common.hash._

val fnl: Funnel[Outer] = implicitly[Funnel[Outer]]
val bf: BloomFilter[Outer] = BloomFilter.create[Outer](fnl, 1000)
```

Some [Algebird](https://github.com/twitter/algebird) instances extend those from Cats and can be derived as well.

```scala mdoc
import com.twitter.{algebird => a}

case class Record(i: Int, o: Option[Int], l: List[Int], s: Set[Int], m: Map[String, Int])

// implicit conversion from cats.{Semigroup,Monoid} to com.twitter.algebird.{Semigroup,Monoid}
val algebirdSg: a.Semigroup[Record] = implicitly[a.Semigroup[Record]]
val algebirdMon: a.Monoid[Record] = implicitly[a.Monoid[Record]]
```

Semi-automatic derivation needs to be called explicitly.

```scala mdoc
import magnolify.cats.semiauto._

val eqOuter: Eq[Outer] = EqDerivation[Outer]
val hashOuter: Hash[Outer] = HashDerivation[Outer]
val monOuter: Monoid[Outer] = MonoidDerivation[Outer]

import magnolify.scalacheck.semiauto._

val arbOuter: Arbitrary[Outer] = ArbitraryDerivation[Outer]
val cogenOuter: Cogen[Outer] = CogenDerivation[Outer]

import magnolify.guava.semiauto.FunnelDerivation

val fnlOuter: Funnel[Outer] = FunnelDerivation[Outer]
```
