# Guava

Type class derivation for Guava can be performed both automatically and semi-automatically.

Automatic derivation are provided as implicits through `import magnolify.guava.auto._`.

```scala mdoc:compile-only
case class Inner(int: Int, str: String)
case class Outer(inner: Inner)

// Guava Funnel
import magnolify.guava.auto._ // includes implicit instances for Funnel[Int], etc.
import com.google.common.hash._

val fnl: Funnel[Outer] = implicitly[Funnel[Outer]]
val bf: BloomFilter[Outer] = BloomFilter.create[Outer](fnl, 1000)
```

Semi-automatic derivation needs to be called explicitly.

```scala mdoc:compile-only
import magnolify.guava.semiauto._
import com.google.common.hash._

case class Inner(int: Int, str: String)
case class Outer(inner: Inner)

val fnl: Funnel[Outer] = FunnelDerivation[Outer]
```
