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

package magnolify.guava.test

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Duration

import com.google.common.hash.{Funnel, PrimitiveSink}
import magnolify.guava.auto._
import magnolify.scalacheck.auto._
import magnolify.test.ADT._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

class FunnelDerivationSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag: Funnel]: Unit =
    test[T, T](identity)

  private def test[T: ClassTag, U](f: T => U)(implicit arb: Arbitrary[T], t: Funnel[T]): Unit = {
    val fnl = ensureSerializable(t)
    val name = className[T]
    val g = arb.arbitrary
    property(s"$name.uniqueness") {
      Prop.forAll(Gen.listOfN(10, g)) { xs =>
        xs.map(toBytes(_, fnl)).toSet.size == xs.map(f).toSet.size
      }
    }
    property(s"$name.consistency") {
      Prop.forAll { x: T => toBytes(x, fnl) == toBytes(x, fnl) }
    }
  }

  private def toBytes[T](x: T, fnl: Funnel[T]): List[Byte] = {
    val sink = new BytesSink
    fnl.funnel(x, sink)
    sink.toBytes.toList
  }

  test[Integers]
  test[Required]
  test[Nullable]
  test[FunnelTypes]

  {
    import Collections._
    test[Repeated]
    test((c: Collections) => (c.a.toList, c.l, c.v))
  }

  {
    import Custom._
    implicit val fUri: Funnel[URI] = FunnelDerivation.by(_.toString)
    implicit val fDuration: Funnel[Duration] = FunnelDerivation.by(_.toMillis)
    test[Custom]
  }

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
    oos.writeByte(b)
    this
  }

  override def putBytes(bytes: Array[Byte]): PrimitiveSink = {
    baos.write(bytes)
    this
  }

  override def putBytes(bytes: Array[Byte], off: Int, len: Int): PrimitiveSink = {
    baos.write(bytes, off, len)
    this
  }
  override def putBytes(bytes: ByteBuffer): PrimitiveSink = {
    baos.write(bytes.array(), bytes.position(), bytes.limit())
    this
  }

  override def putShort(s: Short): PrimitiveSink = {
    oos.writeShort(s)
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
    oos.writeChar(c)
    this
  }

  override def putUnencodedChars(charSequence: CharSequence): PrimitiveSink = {
    oos.writeChars(charSequence.toString)
    this
  }

  override def putString(charSequence: CharSequence, charset: Charset): PrimitiveSink =
    putBytes(charset.encode(charSequence.toString))
}
