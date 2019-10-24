package magnolia.test

import java.io._

import org.scalacheck._
import org.scalacheck.rng.Seed

import scala.reflect._

abstract class MagnoliaSpec(name: String) extends Properties(name) {
  def className[T: ClassTag]: String = classTag[T].runtimeClass.getSimpleName

  // workaround for stack overflow in recursive ADTs
  def includeWithSeed(ps: Properties, seed: Long): Unit =
    for((n,p) <- ps.properties) {
      propertyWithSeed(n, Some(Seed(seed).toBase64)) = p
    }

  private def serializeToByteArray(value: Serializable): Array[Byte] = {
    val buffer = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(buffer)
    oos.writeObject(value)
    buffer.toByteArray
  }

  private def deserializeFromByteArray(encodedValue: Array[Byte]): AnyRef = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(encodedValue))
    ois.readObject()
  }

  def ensureSerializable[T <: Serializable](value: T): T =
    deserializeFromByteArray(serializeToByteArray(value)).asInstanceOf[T]
}
