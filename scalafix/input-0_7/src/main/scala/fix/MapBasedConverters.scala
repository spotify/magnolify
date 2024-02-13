/*
rule = MapBasedConverters
 */
package fix

import magnolify.tensorflow._
import org.tensorflow.proto.example.Example

object MapBasedConverters {

  final case class Person(name: String, age: Int)

  // ExampleType
  val etPerson: ExampleType[Person] = ???
  val e: Example = ???
  val p: Person = ???
  etPerson.from(e)
  etPerson.to(p)
}
