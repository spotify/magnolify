# Cats

Type class derivation for Cats can be performed both automatically and semi-automatically.

Automatic derivation are provided as implicits through `import magnolify.cats.auto._`.

```scala
case class Inner(int: Int, str: String)
case class Outer(inner: Inner)

// Cats Semigroup
import magnolify.cats.auto._
import cats._

val sg: Semigroup[Outer] = implicitly[Semigroup[Outer]]
sg.combine(Outer(Inner(1, "hello, ")), Outer(Inner(100, "world!"))) // = Outer(Inner(101,hello, world!))
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

case class Inner(int: Int, str: String)
case class Outer(inner: Inner)

val eq: Eq[Outer] = EqDerivation[Outer]
val hash: Hash[Outer] = HashDerivation[Outer]
val sg: Semigroup[Outer] = SemigroupDerivation[Outer]
val mon: Monoid[Outer] = MonoidDerivation[Outer]

// this fails due to missing `Group[String]` instance
val group: Group[Outer] = GroupDerivation[Outer]
// error: sg is already defined as value sg
// val sg: a.Semigroup[Record] = implicitly[a.Semigroup[Record]]
//     ^^
// error: Inner is already defined as case class Inner
// case class Inner(int: Int, str: String)
//            ^^^^^
// error: Outer is already defined as case class Outer
// case class Outer(inner: Inner)
//            ^^^^^
// error: sg is already defined as value sg
// val sg: Semigroup[Outer] = SemigroupDerivation[Outer]
//     ^^
// error: mon is already defined as value mon
// val mon: Monoid[Outer] = MonoidDerivation[Outer]
//     ^^^
// error: magnolia: could not find Group.Typeclass for type String
//     in parameter 'str' of product type repl.MdocSession.MdocApp.Inner
//     in parameter 'inner' of product type repl.MdocSession.MdocApp.Outer
// 
// val group: Group[Outer] = GroupDerivation[Outer]
//                           ^^^^^^^^^^^^^^^^^^^^^^
```