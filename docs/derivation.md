Type Class Derivation
=====================

Type class derivation for Cats, ScalaCheck, and Guava can be performed both automatically and semi-automatically.

Automatic derivation are provided as implicits through `import magnolify.$module.auto._`.  It works with context bounds i.e. `def plus[T: Semigroup](x: T, y: T)` and implicit parameters i.e. `def f[T](x: T, y: T)(implicit sg: Semigroup[T])`. The following examples summon them via `implicitly`.

```scala
case class Inner(int: Int, str: String)
case class Outer(inner: Inner)

// Cats Semigroup
import magnolify.cats.auto._
import cats._

val sg: Semigroup[Outer] = implicitly[Semigroup[Outer]]
sg.combine(Outer(Inner(1, "hello, ")), Outer(Inner(100, "world!")))
// = Outer(Inner(101,hello, world!))

// ScalaCheck Arbitrary
import magnolify.scalacheck.auto._
import org.scalacheck._ // implicit instances for Arbitrary[Int], etc.

val arb: Arbitrary[Outer] = implicitly[Arbitrary[Outer]]
arb.arbitrary.sample
// = Some(Outer(Inter(12345, abcde)))

// Guava Funnel
import magnolify.guava.auto._ // includes implicit instances for Funnel[Int], etc.
import com.google.common.hash._
val fnl: Funnel[Outer] = implicitly[Funnel[Outer]]
val bf: BloomFilter[Outer] = BloomFilter.create[Outer](fnl, 1000)
```

Some [Algebird](https://github.com/twitter/algebird) instances extend those from Cats and can be derived as well.

```scala
import magnolify.cats.auto._
import com.twitter.{algebird => a}

case class Record(i: Int, o: Option[Int], l: List[Int], s: Set[Int], m: Map[String, Int])

// implicit conversion from cats.{Semigroup,Monoid} to com.twitter.algebird.{Semigroup,Monoid}
val sg: a.Semigroup[Record] = implicitly[a.Semigroup[Record]]
val mon: a.Monoid[Record] = implicitly[a.Monoid[Record]]
```

Semi-automatic derivation needs to be called explicitly.

```scala
import magnolify.cats.semiauto._
import cats._

val eq: Eq[Outer] = EqDerivation[Outer]
val hash: Hash[Outer] = HashDerivation[Outer]
val sg: Semigroup[Outer] = SemigroupDerivation[Outer]
val mon: Monoid[Outer] = MonoidDerivation[Outer]

// this fails due to missing `Group[String]` instance
val group: Group[Outer] = GroupDerivation[Outer]

import magnolify.scalacheck.semiauto._
import org.scalacheck._

val arb: Arbitrary[Outer] = ArbitraryDerivation[Outer]
val cogen: Cogen[Outer] = CogenDerivation[Outer]

import magnolify.guava.semiauto._

val fnl: Funnel[Outer] = FunnelDerivation[Outer]
```
