package magnolify.guava

import com.google.common.hash.Funnel

import scala.reflect.macros.*

object GuavaMacros {

  def genFunnelMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.guava.FunnelDerivation.gen[$wtt]"""
  }

}

trait SemiAutoDerivations {
  def genFunnel[T]: Funnel[T] = macro GuavaMacros.genFunnelMacro[T]
}

trait AutoDerivations {
  implicit def genFunnel[T]: Funnel[T] = macro GuavaMacros.genFunnelMacro[T]
}
