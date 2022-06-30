package magnolify.scalacheck.test

import magnolify.scalacheck.semiauto.CogenDerivation
import magnolify.test.ADT._
import magnolify.test.JavaEnums
import magnolify.test.Simple._
import org.scalacheck.Cogen

import java.net.URI

object TestCogenImplicits {
  // other
  implicit lazy val coUri: Cogen[URI] =
    Cogen(_.hashCode().toLong)

  // enum
  implicit lazy val coJavaEnum: Cogen[JavaEnums.Color] = Cogen(_.ordinal().toLong)
  implicit lazy val coScalaEnums: Cogen[ScalaEnums.Color.Type] = Cogen(_.id.toLong)

  // ADT
  implicit lazy val coNode: Cogen[Node] = CogenDerivation[Node]
  implicit lazy val coGNode: Cogen[GNode[Int]] = CogenDerivation[GNode[Int]]
  implicit lazy val coShape: Cogen[Shape] = CogenDerivation[Shape]
  implicit lazy val coColor: Cogen[Color] = CogenDerivation[Color]
  implicit lazy val coPerson: Cogen[Person] = CogenDerivation[Person]

  // simple
  implicit lazy val coIntegers: Cogen[Integers] = CogenDerivation[Integers]
  implicit lazy val coFloats: Cogen[Floats] = CogenDerivation[Floats]
  implicit lazy val coNumbers: Cogen[Numbers] = CogenDerivation[Numbers]
  implicit lazy val coRequired: Cogen[Required] = CogenDerivation[Required]
  implicit lazy val coNullable: Cogen[Nullable] = CogenDerivation[Nullable]
  implicit lazy val coRepeated: Cogen[Repeated] = CogenDerivation[Repeated]
  implicit lazy val coNested: Cogen[Nested] = CogenDerivation[Nested]
  implicit lazy val coCollections: Cogen[Collections] = CogenDerivation[Collections]
//  implicit lazy val coMoreCollections: Cogen[MoreCollections] = CogenDerivation[[]
  implicit lazy val coEnums: Cogen[Enums] = CogenDerivation[Enums]
//  implicit lazy val coUnsafeEnums: Cogen[UnsafeEnums] = CogenDerivation[[]
  implicit lazy val coCustom: Cogen[Custom] = CogenDerivation[Custom]
  implicit lazy val coLowerCamel: Cogen[LowerCamel] = CogenDerivation[LowerCamel]
  implicit lazy val coLowerCamelInner: Cogen[LowerCamelInner] = CogenDerivation[LowerCamelInner]
}
