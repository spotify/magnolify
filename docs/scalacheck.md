# Scalacheck

Type class derivation for Scalacheck can be performed both automatically and semi-automatically.

Automatic derivation are provided as implicits through `import magnolify.scalacheck.auto._`.

```scala mdoc:compile-only
case class Inner(int: Int, str: String)
case class Outer(inner: Inner)

// ScalaCheck Arbitrary
import magnolify.scalacheck.auto._
import org.scalacheck._ // implicit instances for Arbitrary[Int], etc.

val arb: Arbitrary[Outer] = implicitly[Arbitrary[Outer]]
arb.arbitrary.sample // = Some(Outer(Inter(12345, abcde)))
```

Semi-automatic derivation needs to be called explicitly.

```scala mdoc:compile-only
import magnolify.scalacheck.semiauto._
import org.scalacheck._

case class Inner(int: Int, str: String)
case class Outer(inner: Inner)

val arb: Arbitrary[Outer] = ArbitraryDerivation[Outer]
val cogen: Cogen[Outer] = CogenDerivation[Outer]
```
