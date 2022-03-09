package magnolify.parquet

import magnolify.shared.CaseMapper
import org.apache.parquet.filter2.predicate.Operators.Column
import org.apache.parquet.filter2.predicate.{
  FilterApi,
  FilterPredicate,
  Statistics,
  UserDefinedPredicate
}
import org.apache.parquet.io.api.{Binary, PrimitiveConverter}
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName

object Predicate {

  def onField[ScalaFieldT](
    fieldName: String
  )(filterFn: ScalaFieldT => Boolean)(implicit
    pf: ParquetField.Primitive[ScalaFieldT]
  ): FilterPredicate = {
    val fieldType = pf.schema(CaseMapper.identity).asPrimitiveType().getPrimitiveTypeName

    val column = fieldType match {
      case PrimitiveTypeName.INT32                           => FilterApi.intColumn(fieldName)
      case PrimitiveTypeName.INT64 | PrimitiveTypeName.INT96 => FilterApi.longColumn(fieldName)
      case PrimitiveTypeName.BINARY                          => FilterApi.binaryColumn(fieldName)
      case PrimitiveTypeName.FLOAT                           => FilterApi.floatColumn(fieldName)
      case PrimitiveTypeName.DOUBLE                          => FilterApi.doubleColumn(fieldName)
      case PrimitiveTypeName.BOOLEAN                         => FilterApi.booleanColumn(fieldName)
      case _ => throw new UnsupportedOperationException(s"Unsupported column type $fieldType")
    }

    def wrap[T](addFn: (PrimitiveConverter, T) => Unit): T => ScalaFieldT = {
      val converter = pf.newConverter
      value => {
        addFn(converter.asPrimitiveConverter(), value)
        converter.get
      }
    }

    type PC = PrimitiveConverter
    val converter = (fieldType match {
      case PrimitiveTypeName.INT32 =>
        wrap((pc: PC, value: java.lang.Integer) => pc.addInt(value))
      case PrimitiveTypeName.INT64 | PrimitiveTypeName.INT96 =>
        wrap((pc: PC, value: java.lang.Long) => pc.addLong(value))
      case PrimitiveTypeName.BINARY =>
        wrap((pc: PC, value: Binary) => pc.addBinary(value))
      case PrimitiveTypeName.FLOAT =>
        wrap((pc: PC, value: java.lang.Float) => pc.addFloat(value))
      case PrimitiveTypeName.DOUBLE =>
        wrap((pc: PC, value: java.lang.Double) => pc.addDouble(value))
      case PrimitiveTypeName.BOOLEAN =>
        wrap((pc: PC, value: java.lang.Boolean) => pc.addBoolean(value))
      case _ => throw new UnsupportedOperationException(s"Unsupported column type $fieldType")
    }).asInstanceOf[pf.ParquetT => ScalaFieldT]

    FilterApi.userDefined[pf.ParquetT, UserDefinedPredicate[pf.ParquetT] with Serializable](
      column.asInstanceOf[Column[pf.ParquetT]],
      new UserDefinedPredicate[pf.ParquetT] with Serializable {
        override def keep(value: pf.ParquetT): Boolean =
          filterFn.apply(converter(value))
        override def canDrop(statistics: Statistics[pf.ParquetT]): Boolean = false
        override def inverseCanDrop(statistics: Statistics[pf.ParquetT]): Boolean = false
      }
    )
  }
}
