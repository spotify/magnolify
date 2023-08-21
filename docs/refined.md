Refined
=======

[Refined](https://github.com/fthomas/refined) is a Scala library for refining types with type-level predicates which constrain the set of values described by the refined type.

```scala  mdoc
import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.auto._
import eu.timepit.refined.boolean._
import eu.timepit.refined.numeric._
import eu.timepit.refined.string._

case class Record(pos: Int Refined Positive, url: String Refined Url)

// Refinements are checked at compile time
Record(42, "https://www.spotify.com")
```

However, refinements only works with literals, so we have to use unsafe runtime check for variables.

```scala  mdoc
val x = -1
val url = "foo"
// Throws IllegalArgumentException
Record(refineV.unsafeFrom(x), refineV.unsafeFrom(url))
```

Magnolify works with Refined through some extra implicits.

```scala  mdoc
import magnolify.avro._
import magnolify.refined.avro._

val at = AvroType[Record]
at(Record(42, "https://www.spotify.com"))
```
