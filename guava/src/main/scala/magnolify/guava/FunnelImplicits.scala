package magnolify.guava

import com.google.common.hash.{Funnel, Funnels, PrimitiveSink}

import scala.annotation.nowarn
trait FunnelImplicits {
  import FunnelImplicits.*

  private def funnel[T](f: (PrimitiveSink, T) => Any): Funnel[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit = f(into, from): @nowarn
  }

  implicit val intFunnel: Funnel[Int] = Funnels.integerFunnel().asInstanceOf[Funnel[Int]]
  implicit val longFunnel: Funnel[Long] = Funnels.longFunnel().asInstanceOf[Funnel[Long]]
  implicit val bytesFunnel: Funnel[Array[Byte]] = Funnels.byteArrayFunnel()
  implicit val booleanFunnel: Funnel[Boolean] = funnel[Boolean](_.putBoolean(_))
  implicit val byteFunnel: Funnel[Byte] = funnel[Byte](_.putByte(_))
  implicit val charFunnel: Funnel[Char] = funnel[Char](_.putChar(_))
  implicit val shortFunnel: Funnel[Short] = funnel[Short](_.putShort(_))

  implicit def charSequenceFunnel[T <: CharSequence]: Funnel[T] =
    Funnels.unencodedCharsFunnel().asInstanceOf[Funnel[T]]

  // There is an implicit Option[T] => Iterable[T]
  implicit def iterableFunnel[T, C[_]](implicit
    fnl: Funnel[T],
    ti: C[T] => Iterable[T]
  ): Funnel[C[T]] =
    funnel { (sink, from) =>
      var i = 0
      ti(from).foreach { x =>
        fnl.funnel(x, sink)
        i += 1
      }
      // inject size to distinguish `None`, `Some("")`, and `List("", "", ...)`
      sink.putInt(i)
    }

  def Funnel[T](implicit fnl: Funnel[T]): Funnel[T] = fnl

  implicit def funnelOps[T](fnl: Funnel[T]): FunnelOps[T] = new FunnelOps(fnl)
}

object FunnelImplicits extends FunnelImplicits {
  final class FunnelOps[T](val fnl: Funnel[T]) extends AnyVal {
    def contramap[U](f: U => T): Funnel[U] = new Funnel[U] {
      override def funnel(from: U, into: PrimitiveSink): Unit = fnl.funnel(f(from), into)
    }
  }
}
