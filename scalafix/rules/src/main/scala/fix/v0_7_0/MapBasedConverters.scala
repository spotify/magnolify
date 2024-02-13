package fix.v0_7_0

import scalafix.v1._

import scala.meta._

object MapBasedConverters {
  private val ExampleType: SymbolMatcher =
    SymbolMatcher.normalized("magnolify/tensorflow/ExampleType")

  private val ConverterFn: SymbolMatcher =
    SymbolMatcher.normalized("magnolify/shared/Converter#from") +
      SymbolMatcher.normalized("magnolify/shared/Converter#to")
}

class MapBasedConverters extends SemanticRule("MapBasedConverters") {
  import MapBasedConverters._

  def isExampleType(term: Term)(implicit doc: SemanticDocument): Boolean =
    term.symbol.info.map(_.signature) match {
      case Some(MethodSignature(_, _, TypeRef(_, sym, _))) => ExampleType.matches(sym)
      case _                                               => false
    }

  override def fix(implicit doc: SemanticDocument): Patch =
    doc.tree.collect {
      case t @ q"$qual.$fn(..$params)" if isExampleType(qual) && ConverterFn.matches(fn) =>
        Patch.replaceTree(t, q"$qual(..$params)".syntax)
    }.asPatch

}
