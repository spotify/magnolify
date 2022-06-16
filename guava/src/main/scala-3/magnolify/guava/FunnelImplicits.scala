package magnolify.guava

import com.google.common.base.Charsets
import com.google.common.hash.{Funnel, Funnels, PrimitiveSink}

trait FunnelImplicits:
  private def funnel[T](f: (PrimitiveSink, T) => Unit): Funnel[T] =
    (from: T, into: PrimitiveSink) => f(into, from)

  given Funnel[Int] = Funnels.integerFunnel().asInstanceOf[Funnel[Int]]
  given Funnel[Long] = Funnels.longFunnel().asInstanceOf[Funnel[Long]]
  given Funnel[Array[Byte]] = Funnels.byteArrayFunnel()
  given Funnel[CharSequence] = Funnels.unencodedCharsFunnel()

  given Funnel[Boolean] = funnel[Boolean](_.putBoolean(_))
  given Funnel[String] = funnel[String](_.putString(_, Charsets.UTF_8))
  given Funnel[Byte] = funnel[Byte](_.putByte(_))
  given Funnel[Char] = funnel[Char](_.putChar(_))
  given Funnel[Short] = funnel[Short](_.putShort(_))

//  // There is an implicit Option[T] => Iterable[T]
//  implicit def iterableFunnel[T, C[_]](implicit
//                                       fnl: Funnel[T],
//                                       ti: Conversion[C[_], Iterable[_]]
//                                      ): Funnel[C[T]] =
//    funnel { (sink, from) =>
//      var i = 0
//      ti(from).foreach { x =>
//        fnl.funnel(x, sink)
//        i += 1
//      }
//      // inject size to distinguish `None`, `Some("")`, and `List("", "", ...)`
//      sink.putInt(i)
//    }
