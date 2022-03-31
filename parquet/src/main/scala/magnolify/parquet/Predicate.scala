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

  /**
   * Constructs a Parquet [[FilterPredicate]] based on a single primitive column and a user-defined
   * function. Predicates can be logically combined using [[FilterApi.and]] and [[FilterApi.or]].
   *
   * Note that Optional fields should use a predicate for their lifted type: that is, create a
   * predicate for an optional String field using [[Predicate.onField[String]], even if its
   * projected case class defines the field as `Option[String]`. When applying predicates, Parquet
   * will filter out optional fields with a value of `null` before applying the predicate filter.
   *
   * @param fieldName
   *   the name of the primitive column. Nested fields are separated by '.'s
   * @param filterFn
   *   the UDF representing the desired filter
   * @return
   *   a FilterPredicate for use with typed Parquet
   */
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
      lazy val converter = pf.newConverter
      value => {
        addFn(converter.asPrimitiveConverter(), value)
        converter.get
      }
    }

    type PC = PrimitiveConverter
    val toScalaT = (fieldType match {
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
          filterFn.apply(toScalaT(value))
        override def canDrop(statistics: Statistics[pf.ParquetT]): Boolean = false
        override def inverseCanDrop(statistics: Statistics[pf.ParquetT]): Boolean = false
      }
    )
  }
}
