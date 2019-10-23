package magnolia.bigquery.test

import magnolia.test.Simple._

object ScopeTest {
  object Auto {
    import magnolia.bigquery.auto._
    implicitly[TableRowType[Integers]]
  }

  object Semi {
    import magnolia.bigquery.semiauto._
    TableRowType[Integers]
  }
}
