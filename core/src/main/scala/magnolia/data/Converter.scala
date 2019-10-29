package magnolia.data

object Converter {
  trait Record[T, Reader, Writer] extends Serializable {
    protected def empty: Writer
    def from(r: Reader): T
    def to(t: T): Writer
  }

  trait Field[V, Reader, Writer] extends Record[V, Reader, Writer] {
    def get(r: Reader, k: String): V
    def put(r: Writer, k: String, v: V): Writer
  }
}
