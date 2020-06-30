EntityType
==========

`EntityType[T]` provides conversion between Scala type `T` and Datastore `Entity`. Custom support for type `T` can be added with an implicit instance of `EntityField[T]`.

```scala
import java.net.URI
case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

import magnolify.datastore._
import com.google.datastore.v1.{Entiyt, Entity.Builder}

// Encode custom type URI as String
implicit val uriField = EntityField.from[String](URI.create)(_.toString)

val entityType = EntityType[Outer]
val entityBuilder: Entity.Builder = entityType.to(record)
val copy: Outer = entityType.from(entityBuilder.build)
```

Additional `EntityField[T]` instances for `Byte`, `Char`, `Short`, `Int`, and `Float` are available from `import magnolify.datastore.unsafe._`. These conversions are unsafe due to potential overflow.

To use a different field case format in target records, add an optional `CaseMapper` argument to `EntityType`. The following example maps `firstName` & `lastName` to `first_name` & `last_name`.

```scala
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert _
val entityType = EntityType[LowerCamel](CaseMapper(toSnakeCase))
entityType.to(LowerCamel("John", "Doe"))
```
