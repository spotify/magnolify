/*
 * Copyright 2019 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package magnolify.guava

import com.google.common.hash.{Funnel, PrimitiveSink}
import magnolify.scalacheck.TestArbitrary.*
import magnolify.test.*
import magnolify.test.ADT.*
import magnolify.test.Simple.*
import org.scalacheck.*

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Duration
import scala.reflect.*

class FunnelDerivationSuite
    extends MagnolifySuite
    with magnolify.scalacheck.AutoDerivations
    with magnolify.guava.FunnelImplicits
    with magnolify.guava.AutoDerivations {

  private def test[T: ClassTag](implicit arb: Arbitrary[T], t: Funnel[T]): Unit = {
    // TODO val fnl = ensureSerializable(t)
    val fnl = t
    val name = className[T]
    val g = arb.arbitrary
    property(s"$name.uniqueness") {
      Prop.forAll(Gen.listOfN(10, g)) { xs =>
        xs.map(toBytes(_, fnl)).toSet.size == xs.toSet.size
      }
    }
    property(s"$name.consistency") {
      Prop.forAll((x: T) => toBytes(x, fnl) == toBytes(x, fnl))
    }
  }

  private def toBytes[T](x: T, fnl: Funnel[T]): List[Byte] = {
    val sink = new BytesSink
    fnl.funnel(x, sink)
    sink.toBytes.toList
  }

  implicit val fUri: Funnel[URI] = Funnel[String].contramap(_.toString)
  implicit val fDuration: Funnel[Duration] = Funnel[Long].contramap(_.toMillis)

  test[Integers]
  test[Required]
  test[Nullable]
  test[FunnelTypes]

  test[Repeated]
  test[Collections]
  test[Custom]

  // test unwrapped
  test[Int]
  test[List[Int]]
  test[List[Required]]

  // TODO scala3 value-class are mirrorless and not supported by magnolia
  //  test("AnyVal") {
  //    implicit val f: Funnel[HasValueClass] = genFunnel[HasValueClass]
  //    test[HasValueClass]
  //
  //    val sink = new BytesSink()
  //    f.funnel(HasValueClass(ValueClass("String")), sink)
  //
  //    val ois = new ObjectInputStream(new ByteArrayInputStream(sink.toBytes))
  //    assert(ois.readInt() == 0)
  //    "String".foreach(c => assert(ois.readChar() == c))
  //    assert(ois.available() == 0)
  //  }

  // magnolia scala3 limitation:
  // For a recursive structures it is required to assign the derived value to an implicit variable
  implicit val funnelNode: Funnel[Node] = genFunnel
  implicit val funnelGNode: Funnel[GNode[Int]] = genFunnel
  test[Node]
  test[GNode[Int]]

  test[Shape]
  test[Color]
}

case class FunnelTypes(b: Byte, c: Char, s: Short)

class BytesSink extends PrimitiveSink {
  private val baos = new ByteArrayOutputStream()
  private val oos = new ObjectOutputStream(baos)

  def toBytes: Array[Byte] = {
    oos.close()
    baos.close()
    baos.toByteArray
  }

  override def putByte(b: Byte): PrimitiveSink = {
    oos.writeByte(b.toInt)
    this
  }

  override def putBytes(bytes: Array[Byte]): PrimitiveSink = {
    oos.write(bytes)
    this
  }

  override def putBytes(bytes: Array[Byte], off: Int, len: Int): PrimitiveSink = {
    oos.write(bytes, off, len)
    this
  }
  override def putBytes(bytes: ByteBuffer): PrimitiveSink = {
    oos.write(bytes.array(), bytes.position(), bytes.limit())
    this
  }

  override def putShort(s: Short): PrimitiveSink = {
    oos.writeShort(s.toInt)
    this
  }

  override def putInt(i: Int): PrimitiveSink = {
    oos.writeInt(i)
    this
  }

  override def putLong(l: Long): PrimitiveSink = {
    oos.writeLong(l)
    this
  }

  override def putFloat(f: Float): PrimitiveSink = {
    oos.writeFloat(f)
    this
  }

  override def putDouble(d: Double): PrimitiveSink = {
    oos.writeDouble(d)
    this
  }

  override def putBoolean(b: Boolean): PrimitiveSink = {
    oos.writeBoolean(b)
    this
  }

  override def putChar(c: Char): PrimitiveSink = {
    oos.writeChar(c.toInt)
    this
  }

  override def putUnencodedChars(charSequence: CharSequence): PrimitiveSink = {
    oos.writeChars(charSequence.toString)
    this
  }

  override def putString(charSequence: CharSequence, charset: Charset): PrimitiveSink =
    putBytes(charset.encode(charSequence.toString))
}
