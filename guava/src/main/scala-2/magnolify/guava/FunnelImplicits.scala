package magnolify.guava

import com.google.common.base.Charsets
import com.google.common.hash.{Funnel, Funnels, PrimitiveSink}

trait FunnelImplicits {
  private def funnel[T](f: (PrimitiveSink, T) => Unit): Funnel[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit = f(into, from)
  }

  implicit val intFunnel: Funnel[Int] = Funnels.integerFunnel().asInstanceOf[Funnel[Int]]
  implicit val longFunnel: Funnel[Long] = Funnels.longFunnel().asInstanceOf[Funnel[Long]]
  implicit val bytesFunnel: Funnel[Array[Byte]] = Funnels.byteArrayFunnel()
  implicit val charSequenceFunnel: Funnel[CharSequence] = Funnels.unencodedCharsFunnel()

  implicit val booleanFunnel: Funnel[Boolean] = funnel[Boolean](_.putBoolean(_))
  implicit val stringFunnel: Funnel[String] = funnel[String](_.putString(_, Charsets.UTF_8))
  implicit val byteFunnel: Funnel[Byte] = funnel[Byte](_.putByte(_))
  implicit val charFunnel: Funnel[Char] = funnel[Char](_.putChar(_))
  implicit val shortFunnel: Funnel[Short] = funnel[Short](_.putShort(_))

  // There is an implicit Option[T] => Iterable[T]
  implicit def iterableFunnel[T, C[_]](implicit
    fnl: Funnel[T],
    ti: C[T] => Iterable[T]
  ): Funnel[C[T]] =
    funnel { (sink, from) =>
      var i = 0
      from.foreach { x =>
        fnl.funnel(x, sink)
        i += 1
      }
      // inject size to distinguish `None`, `Some("")`, and `List("", "", ...)`
      sink.putInt(i)
    }
}
