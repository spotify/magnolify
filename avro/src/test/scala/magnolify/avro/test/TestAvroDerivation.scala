package magnolify.avro

import org.apache.avro.Schema
import org.apache.avro.Schema.Field
import org.apache.avro.generic.GenericData

import scala.collection.JavaConverters._

// @TODO convert to MagnolifySpec

case class NestedCC(f: String)

case class CC(
               strField: String,
               intField: Int,
               listField: List[String],
               nestedCCField: NestedCC
             )

object TestAvroDerivation extends Implicits {
  private val default: Any = null

  private val nestedCCSchema = Schema.createRecord(
    "NestedCC", "", "com.spotify.magnolia", false,
    List(
      new Field("f", Schema.create(Schema.Type.STRING), "", default)
    ).asJava
  )

  private val fields = List(
    new Field("strField", Schema.create(Schema.Type.STRING), "", default),
    new Field("intField", Schema.create(Schema.Type.INT), "", default),
    new Field("listField", Schema.createArray(Schema.create(Schema.Type.STRING)), "", default),
    new Field("nestedCCField", nestedCCSchema, "", default)
  ).asJava

  private val schema = Schema.createRecord(
    "CC", "", "magnolify.avro", false, fields)

  def main(args: Array[String]): Unit = {
    val m = AvroRecordType.gen[CC]

    val cc = CC("Mary", 20 , List("fluffy", "fido"), NestedCC("foo"))

    val nestedGR = new GenericData.Record(nestedCCSchema)
    nestedGR.put("f", cc.nestedCCField.f)

    val gr = new GenericData.Record(schema)
    gr.put("strField", cc.strField)
    gr.put("intField", cc.intField)
    gr.put("listField", cc.listField.asJava)
    gr.put("nestedCCField", nestedGR)

    print(m.schema)
    print("\nFrom:" + m.from(gr))
    print("\nTo: " + m.to(cc))
  }
}
