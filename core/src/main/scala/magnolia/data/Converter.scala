package magnolia.data

object Converter {
  trait Record[T, R] extends Serializable {
    protected def empty: R
    def from(r: R): T
    def to(t: T): R
  }

  trait Field[V, R] extends Record[V, R] {
    def get(r: R, k: String): V
    def put(r: R, k: String, v: V): Unit
  }
}
