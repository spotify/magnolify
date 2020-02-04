/*
 * Copyright 2020 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.protobuf


import java.nio.ByteBuffer
import java.{util => ju}

import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Message
import magnolia._
import magnolify.shared.Converter
import magnolify.shims.FactoryCompat
import magnolify.shims.JavaConverters._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.reflect.ClassTag


sealed trait ProtobufType[T, ParentMsgT <: Message] extends Converter[T, ParentMsgT,
  ParentMsgT] {
  val messageBuilder: Message.Builder
  def apply(r: ParentMsgT): T = from(r)
  def apply(t: T): ParentMsgT = to(t)
}

object ProtobufType {
  implicit def apply[T, ParentMsgT <: Message : ClassTag]
  (implicit f: ProtobufField.Record[T]): ProtobufType[T, ParentMsgT] =
    new ProtobufType[T, ParentMsgT] {
      override val messageBuilder: Message.Builder = implicitly[ClassTag[ParentMsgT]]
        .runtimeClass
        .getMethod("newBuilder")
        .invoke(null)
        .asInstanceOf[Message.Builder]

      override def from(v: ParentMsgT): T = f.from(v, messageBuilder)
      override def to(v: T): ParentMsgT = f.to(v, messageBuilder).asInstanceOf[ParentMsgT]
    }
}

sealed trait ProtobufField[T] extends Serializable { self =>
  type FromT
  type ToT

  def from(v: FromT, b: Message.Builder): T
  def to(v: T, b: Message.Builder): ToT

  def fromAny(v: Any, b: Message.Builder): T = from(v.asInstanceOf[FromT], b)
}

object ProtobufField {

  trait Aux[T, From, To] extends ProtobufField[T] {
    override type FromT = From
    override type ToT = To
  }

  trait Generic[T] extends Aux[T, Any, Any] // todo rm
  trait Record[T] extends Aux[T, Message, Message]

  //////////////////////////////////////////////////

  type Typeclass[T] = ProtobufField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {

    override def from(v: Message, b: Message.Builder): T =
      caseClass.construct(p => {
        val fieldDescriptor = b.getDescriptorForType.findFieldByName(p.label)

        if (fieldDescriptor.getType == FieldDescriptor.Type.MESSAGE) {
          // nested
          p.typeclass.fromAny(v.getField(fieldDescriptor), b.newBuilderForField(fieldDescriptor))
        } else {
          // non-nested
          p.typeclass.fromAny(v.getField(fieldDescriptor), b)
        }
      })

    override def to(v: T, bu: Message.Builder): Message = {
      // clear builder from previous runs before using it to construct a new instance
      caseClass.parameters.foldLeft(bu.clear()) { (b, p) =>
        val fieldDescriptor = b.getDescriptorForType.findFieldByName(p.label)

        if (fieldDescriptor.getType == FieldDescriptor.Type.MESSAGE) { // nested records
          val messageValue = p.typeclass.to(p.dereference(v), b.newBuilderForField(fieldDescriptor))

          if (messageValue == null) b else b.setField(fieldDescriptor, messageValue)
        } else {
          // non-nested
          val fieldValue = p.typeclass.to(p.dereference(v), b)
          if (fieldValue == null) b else b.setField(fieldDescriptor, fieldValue)
        }
      }
        .build()
    }
  }

  @implicitNotFound("Cannot derive ProtobufField for sealed trait")
  private sealed trait Dispatchable[T]

  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Record[T] = ???

  implicit def gen[T]: Record[T] = macro Magnolia.gen[T]

  //////////////////////////////////////////////////

  def apply[T](implicit f: ProtobufField[T]): ProtobufField[T] = f

  // TODO decide what to do with this
//  def from[T]: FromWord[T] = new FromWord[T]
//
//  class FromWord[T] {
//    def apply[U](f: T => U)(g: U => T)(implicit trf: ProtobufField[T]): ProtobufField[U] =
//      new ProtobufField[U] {
//        override type FromT = trf.FromT
//        override type ToT = trf.ToT
//
//        override def from(v: FromT): U = f(trf.from(v, descriptor))
//
//        override def to(v: U): ToT = trf.to(g(v), descriptor)
//      }
//  }

  //////////////////////////////////////////////////

  private def at[T, From, To](f: From => T)(g: T => To): ProtobufField[T] =
    new ProtobufField[T] {
      override type FromT = From
      override type ToT = To

      override def from(v: FromT, b: Message.Builder): T = f(v)

      override def to(v: T, b: Message.Builder): ToT = g(v)
    }

  def from[T, Repr](f: Repr => T)(g: T => Repr): ProtobufField[T] =
    at[T, Repr, Repr](f)(g)

  private def id[T]: ProtobufField[T] =
    at[T, T, T](identity)(identity)


  implicit val pfBoolean = id[Boolean]
  implicit val pfString = id[String]
  implicit val pfInt = id[Int]
  implicit val pfLong = id[Long]
  implicit val pfFloat = id[Float]
  implicit val pfDouble = id[Double]
  implicit val pfBytes = from[Array[Byte], ByteBuffer](bb =>
    ju.Arrays.copyOfRange(bb.array(), bb.position(), bb.limit())
  )(ByteBuffer.wrap)

  implicit def pfIterable[T, C[_]](implicit f: ProtobufField[T],
                                     ti: C[T] => Iterable[T],
                                     fc: FactoryCompat[T, C[T]]
                                   ): ProtobufField[C[T]] =
    new Aux[C[T], ju.List[f.FromT], ju.List[f.ToT]] {
      override def from(v: ju.List[f.FromT], b: Message.Builder): C[T] =
        if (v == null) {
          fc.newBuilder.result()
        } else {
          fc.build(v.asScala.iterator.map(f.from(_, b)))
        }
      override def to(v: C[T], b: Message.Builder): ju.List[f.ToT] =
        if (v.isEmpty) null else v.iterator.map(f.to(_, b)).toList.asJava
    }

  implicit def pfMap[T, U](implicit k: ProtobufField[T], v: ProtobufField[U])
  : ProtobufField[Map[T, U]] =
    new Aux[Map[T, U], ju.Map[k.FromT, v.FromT], ju.Map[k.ToT, v.ToT]] {
      override def from(kvalues: ju.Map[k.FromT, v.FromT], b: Message.Builder): Map[T, U] =
        if (kvalues == null) {
          Map.empty
        } else {
          kvalues.asScala.iterator.map(kv => (k.from(kv._1, b), v.from(kv._2, b))).toMap
        }

      override def to(kvalues: Map[T, U], b: Message.Builder): ju.Map[k.ToT, v.ToT] =
        if (kvalues.isEmpty) null else kvalues.iterator.map(kv => (k.to(kv._1, b), v.to(kv._2, b)))
          .toMap
          .asJava
    }

}
