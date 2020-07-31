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
package magnolify.codegen

import java.io.File
import java.util.Locale
import java.util.regex.Pattern

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.services.bigquery.model.{
  Job,
  JobConfiguration,
  JobConfigurationQuery,
  TableReference,
  TableSchema
}
import com.google.api.services.bigquery.{Bigquery, BigqueryScopes}
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.bigquery.storage.v1.{
  BigQueryReadClient,
  BigQueryReadSettings,
  CreateReadSessionRequest,
  DataFormat,
  ReadSession
}
import org.apache.avro.Schema
import magnolify.shims.JavaConverters._

object BigQueryClient {
  private val PROJECT_ID_REGEXP = "[a-z][-a-z0-9:.]{4,61}[a-z0-9]"
  private val DATASET_REGEXP = "[-\\w.]{1,1024}"
  private val TABLE_REGEXP = "[-\\w$@]{1,1024}"
  private val DATASET_TABLE_REGEXP =
    String.format(
      "((?<PROJECT>%s):)?(?<DATASET>%s)\\.(?<TABLE>%s)",
      PROJECT_ID_REGEXP,
      DATASET_REGEXP,
      TABLE_REGEXP
    )
  private val TABLE_SPEC = Pattern.compile(DATASET_TABLE_REGEXP)

  private val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

  private val defaultProject: String = {
    def isWindows = sys.props("os.name").toLowerCase(Locale.ENGLISH).contains("windows")

    val configFile = if (sys.env.contains("CLOUDSDK_CONFIG")) {
      new File(sys.env("CLOUDSDK_CONFIG"), "properties")
    } else if (isWindows && sys.env.contains("APPDATA")) {
      new File(sys.env("APPDATA"), "gcloud/properties")
    } else {
      val home = sys.props("user.home")
      val f = new File(home, ".config/gcloud/configurations/config_default")
      if (f.exists()) f else new File(home, ".config/gcloud/properties")
    }

    import java.util.regex.Pattern
    val projectPattern = Pattern.compile("^project\\s*=\\s*(.*)$")
    val sectionPattern = Pattern.compile("^\\[(.*)\\]$")
    val src = scala.io.Source.fromFile(configFile)(scala.io.Codec.UTF8)
    val iterator = src
      .getLines()
      .map(_.trim)
      .filterNot(l => l.isEmpty || l.startsWith(";"))

    var section: String = null
    var project: String = null
    while (project == null && iterator.hasNext) {
      val l = iterator.next()
      val m1 = sectionPattern.matcher(l)
      if (m1.matches()) {
        section = m1.group(1)
      } else if (section == null || section == "core") {
        val m2 = projectPattern.matcher(l)
        if (m2.matches()) {
          project = m2.group(1).trim
        }
      }
    }
    src.close()

    project
  }

  private val credentials: GoogleCredentials =
    GoogleCredentials.getApplicationDefault.createScoped(BigqueryScopes.BIGQUERY)

  private val client: Bigquery = {
    new Bigquery.Builder(
      new NetHttpTransport,
      new JacksonFactory,
      new HttpCredentialsAdapter(credentials)
    )
      .build()
  }

  private val storage: BigQueryReadClient = {
    val settings = BigQueryReadSettings
      .newBuilder()
      .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
      .build()
    BigQueryReadClient.create(settings)
  }

  private def parseTableSpec(table: String): TableReference = {
    val m = TABLE_SPEC.matcher(table)
    require(m.matches(), s"Malformed table specification: $table")
    new TableReference()
      .setProjectId(m.group("PROJECT"))
      .setDatasetId(m.group("DATASET"))
      .setTableId(m.group("TABLE"))
  }

  def fromSchema(json: String): TableSchema =
    mapper.readValue(new File(json), classOf[TableSchema])

  def fromTable(table: String): TableSchema = {
    val tr = parseTableSpec(table)
    client.tables().get(tr.getProjectId, tr.getDatasetId, tr.getTableId).execute().getSchema
  }

  def fromQuery(query: String): TableSchema = {
    val jobConfQuery = new JobConfigurationQuery()
      .setQuery(query)
      .setUseLegacySql(false)
    val jobConf = new JobConfiguration().setQuery(jobConfQuery).setDryRun(true)
    val job = new Job().setConfiguration(jobConf)

    client.jobs().insert(defaultProject, job).execute().getStatistics.getQuery.getSchema
  }

  def fromStorage(
    table: String,
    selectedFields: List[String],
    rowRestriction: Option[String]
  ): TableSchema = {
    val tr = parseTableSpec(table)
    val request = CreateReadSessionRequest
      .newBuilder()
      .setReadSession(
        ReadSession
          .newBuilder()
          .setTable(
            s"projects/${tr.getProjectId}/datasets/${tr.getDatasetId}/tables/${tr.getTableId}"
          )
          .setDataFormat(DataFormat.AVRO)
          .setReadOptions(
            ReadSession.TableReadOptions
              .newBuilder()
              .addAllSelectedFields(selectedFields.asJava)
              .setRowRestriction(rowRestriction.getOrElse(""))
          )
      )
      .setParent(s"projects/$defaultProject")
      .build()
    val schemaString = storage.createReadSession(request).getAvroSchema.getSchema
    BigQueryAvro.fromAvro(new Schema.Parser().parse(schemaString))
  }
}
