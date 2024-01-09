# Refined

[Refined](https://github.com/fthomas/refined) is a Scala library for refining types with type-level predicates which constrain the set of values described by the refined type.

```scala
import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import eu.timepit.refined.string._

case class Record(pos: Int Refined Positive, url: String Refined Url)

// Refinements are checked at compile time with literals
Record(42, "https://www.spotify.com")
// otherwise unsafe runtime check have to be used for variables.
val x = -1
val url = "foo"
// Throws IllegalArgumentException
Record(refineV.unsafeFrom(x), refineV.unsafeFrom(url))
```

Magnolify works with Refined through some extra implicits.

```scala
import eu.timepit.refined.api._
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import eu.timepit.refined.string._

import magnolify.avro._
import magnolify.refined.avro._

case class Record(pos: Int Refined Positive, url: String Refined Url)

val at = AvroType[Record]
at(Record(42, "https://www.spotify.com"))
```
