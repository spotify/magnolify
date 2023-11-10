package magnolify.guava

import com.google.common.hash.Funnel

import scala.deriving.Mirror

trait SemiAutoDerivations:
  inline def genFunnel[T](using Mirror.Of[T]): Funnel[T] = FunnelDerivation.derivedMirror[T]

trait AutoDerivations:
  inline given genFunnel[T](using Mirror.Of[T]): Funnel[T] = FunnelDerivation.derivedMirror[T]
