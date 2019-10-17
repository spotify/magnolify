package magnolia.test

object Simple {
  case class Required(b: Boolean, i: Int, s: String)
  case class Nullable(b: Option[Boolean], i: Option[Int], s: Option[String])
  case class Repeated(b: List[Boolean], i: List[Int], s: List[String])
  case class Nested(b: Boolean, i: Int, s: String, r: Required)
}
