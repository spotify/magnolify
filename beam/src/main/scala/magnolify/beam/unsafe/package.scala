package magnolify.beam

import magnolify.shared._

package object unsafe {
  implicit def afUnsafeEnum[T: EnumType]: BeamSchemaField[UnsafeEnum[T]] =
    BeamSchemaField.from[String](UnsafeEnum.from[T])(UnsafeEnum.to[T])
}
