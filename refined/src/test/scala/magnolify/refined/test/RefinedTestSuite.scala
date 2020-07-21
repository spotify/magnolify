/*
 * Copyright 2020 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.refined.test

import magnolify.avro.test._
import magnolify.bigquery.test._
import cats.implicits._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.refined._
import eu.timepit.refined.scalacheck.string._
import eu.timepit.refined.scalacheck.numeric._

object TestUtils {
  import eu.timepit.refined._
  import eu.timepit.refined.api._
  import eu.timepit.refined.boolean._
  import eu.timepit.refined.numeric._
  import eu.timepit.refined.string._

  type LessThanHundred = Positive And Not[Greater[W.`100`.T]]
  type Age = Int Refined LessThanHundred
  type StartWithS = String Refined StartsWith[W.`"S"`.T]

  type Count = Long Refined Positive
  type Word = String Refined Trimmed

  case class Person(name: StartWithS, age: Age)
  case class WordCount(string: Word, count: Count)
}

class RefinedAvroSuite extends AvroBaseSuite {
  import TestUtils._
  test[Person]
}

class RefinedTableRowSuite extends TableRowBaseSuite {
  import TestUtils._
  test[WordCount]
}
