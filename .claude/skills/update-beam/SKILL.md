---
name: update-beam
description: >
  Update Apache Beam and all pinned Google Cloud dependency versions in
  Magnolify's build.sbt. Use this skill whenever someone asks to bump Beam,
  upgrade Beam, update Beam, align dependencies with libraries-bom, or sync
  Google Cloud dependency versions for the Magnolify project. Also trigger when
  the user says "update-beam", "/update-beam", or mentions upgrading any of:
  beamVersion, bigtableVersion, datastoreVersion, guavaVersion, protobufVersion,
  jacksonVersion in the context of Magnolify.
---

# Update Beam & Align Google Cloud Dependencies

This skill updates Magnolify's `build.sbt` to use the latest stable Apache Beam
release and aligns every pinned Google Cloud / shared dependency version with the
`com.google.cloud:libraries-bom` that Beam ships against.

## Priority rule

> **Beam wins.** If a dependency version is pinned in *both* Beam's
> `BeamModulePlugin.groovy` and the resolved `libraries-bom`, use the Beam
> version. The BOM is the fallback for deps Beam doesn't explicitly pin.

---

## Workflow (execute every step in order)

All steps below should be performed directly by fetching URLs and parsing
their content inline — no external scripts are needed.

### Step 1 — Find the latest stable Beam release

Search the web for the latest Apache Beam release version, or fetch:

```
https://raw.githubusercontent.com/apache/beam/master/buildSrc/src/main/groovy/org/apache/beam/gradle/BeamModulePlugin.groovy
```

If the `master` branch is ahead of the latest release tag, use the release tag
instead (e.g. `v2.75.0`). Confirm the version with a web search if unsure.
Store the result as `$BEAM_VERSION` (e.g. `2.75.0`).

### Step 2 — Extract dependency versions from Beam

First, read `build.sbt` and collect every `val …Version = "…"` declaration.
These are the dependencies Magnolify pins. Note each variable name and its
current value — you will need the current values for the Step 6 report.

Next, fetch:

```
https://raw.githubusercontent.com/apache/beam/v$BEAM_VERSION/buildSrc/src/main/groovy/org/apache/beam/gradle/BeamModulePlugin.groovy
```

Parse this Groovy file to build a map called **beam_pins**:

1. **`libraries-bom` version (required)** — find the line containing
   `google_cloud_platform_libraries_bom` with a string like
   `"com.google.cloud:libraries-bom:26.83.0"`. Extract the version after the
   last colon. Store this as `$LIBRARIES_BOM_VERSION`. This is needed for Step 3.

2. **`def …_version` variables** — scan for all lines matching
   `def <name>_version = "<value>"`. For each one, convert the Groovy-style
   snake_case name to the camelCase equivalent used in `build.sbt`
   (e.g. `guava_version` → `guavaVersion`). If a matching `build.sbt` variable
   exists, record it in **beam_pins**.

3. **Inline artifact versions** — scan for dependency strings with inline
   versions, e.g. `"groupId:artifactId:version"`. For each artifact that
   corresponds to a `build.sbt` variable (match by artifact name — e.g.
   `google-api-services-bigquery` → `bigqueryVersion`,
   `datastore-v1-proto-client` → `datastoreVersion`,
   `org.apache.avro:avro` → `avroVersion`), extract the version and record
   it in **beam_pins**.

Note: some deps (like `proto_google_cloud_bigtable_v2`) are marked
"google_cloud_platform_libraries_bom sets version" — meaning Beam delegates to
the BOM and doesn't pin them. In that case there is no beam_pin entry; Step 3
will fill it in.

### Step 3 — Resolve the libraries-bom dependency tree

The libraries-bom is a hierarchy of nested BOMs. Resolve them in order:

#### 3a. Fetch the google-cloud-bom POM

From the `googleapis/java-cloud-bom` GitHub repo, fetch `google-cloud-bom/pom.xml`.
Try the `main` branch first; if the repo is archived or 404s, try the tag
matching the libraries-bom version (e.g. `v$LIBRARIES_BOM_VERSION`).

URL: `https://raw.githubusercontent.com/googleapis/java-cloud-bom/main/google-cloud-bom/pom.xml`

From the POM XML, extract:

- `gapic-libraries-bom` version (e.g. `1.87.1`) — look for a `<dependency>`
  entry with `<artifactId>gapic-libraries-bom</artifactId>` and extract its
  `<version>` element. Store as `$GAPIC_VERSION`.

#### 3b. Fetch the gapic-libraries-bom POM

URL: `https://raw.githubusercontent.com/googleapis/google-cloud-java/v$GAPIC_VERSION/gapic-libraries-bom/pom.xml`

This contains ~215 sub-BOM imports. You only need to identify the bigtable
sub-BOM entry to proceed to 3c.

#### 3c. Resolve the bigtable sub-BOM

Fetch the bigtable sub-BOM. The directory structure in `google-cloud-java` uses
varying prefixes, so try these paths in order:

1. `https://raw.githubusercontent.com/googleapis/google-cloud-java/v$GAPIC_VERSION/java-bigtable/google-cloud-bigtable-bom/pom.xml`
2. `https://raw.githubusercontent.com/googleapis/google-cloud-java/v$GAPIC_VERSION/google-cloud-bigtable/google-cloud-bigtable-bom/pom.xml`

From the bigtable BOM POM, find the `<dependency>` entry with
`<artifactId>proto-google-cloud-bigtable-v2</artifactId>` and extract its
`<version>`. This is the `bigtableVersion` value.

The `proto-google-cloud-bigtable-v2` version will match the
`google-cloud-bigtable` main artifact version (they share a BOM).

#### 3d. Resolve first-party-dependencies for core library versions

The `first-party-dependencies` BOM (from `googleapis/sdk-platform-java`) manages
core deps like Guava, Protobuf, and gRPC. Fetch:

URL: `https://raw.githubusercontent.com/googleapis/sdk-platform-java/main/gapic-generator-java-pom-parent/pom.xml`

Parse the `<properties>` block and extract `guava.version`, `protobuf.version`,
`grpc.version`. These serve as BOM fallback values for `guavaVersion` and
`protobufVersion` if Beam doesn't pin them explicitly.

> **Important:** The sdk-platform-java repo tags don't match the
> `first-party-dependencies` Maven version. Use the `main` branch as a
> reasonable proxy, or search for the correct tag via the GitHub API.

### Step 4 — Merge beam_pins with BOM versions

Build the final version map. For every pinned dependency in `build.sbt`:

1. If **beam_pins** has an explicit version → use it.
2. Else if the BOM resolution (Steps 3c–3d) has a version → use it.
3. Else → leave unchanged.

### Step 5 — Update build.sbt

Read `build.sbt` (at repository root). Apply `str_replace` edits to each `val …Version` line
that needs updating. The version declarations are all in the first ~50 lines.

Here is a nonexhaustive list of variables to check and potentially update:

```
val beamVersion = "$BEAM_VERSION"
val bigtableVersion = "…"       ← from BOM bigtable sub-BOM (proto-google-cloud-bigtable-v2)
val datastoreVersion = "…"      ← beam_pin OR BOM (datastore-v1-proto-client)
val guavaVersion = "…"          ← beam_pin OR first-party-dependencies
val jacksonVersion = "…"        ← beam_pin
val protobufVersion = "…"       ← beam_pin OR first-party-dependencies
val hadoopVersion = "…"         ← beam_pin
val avroVersion = "…"           ← beam_pin (the default, not the sys.prop override)
val bigqueryVersion = "…"       ← beam_pin (google-api-services-bigquery)
```

**Do NOT touch** these unless you have a concrete reason:

- `algebirdVersion`, `catsVersion`, `magnoliaScala*Version`, `munitVersion`,
  `neo4jDriverVersion`, `paigesVersion`, `parquetVersion`, `refinedVersion`,
  `scalaCollectionCompatVersion`, `scalacheckVersion`, `shapelessVersion`,
  `slf4jVersion`, `tensorflowVersion`, `tensorflowMetadataVersion`

### Step 6 — Report changes

After editing, print a summary table:

```
| Variable          | Old      | New      | Source                              |
|-------------------|----------|----------|-------------------------------------|
| beamVersion       | 2.71.0   | 2.75.0   | Latest stable release               |
| bigtableVersion   | 2.68.0   | 2.79.0   | libraries-bom → bigtable-bom        |
| …                 | …        | …        | …                                   |
```

Flag any **major version bumps** (e.g. datastoreVersion 2.x → 3.x) with a
warning about potential breaking changes.

Present the updated `build.sbt` file to the user.

---

## Useful URLs (fill in version variables at runtime)

| Resource | URL template |
|----------|-------------|
| Beam module plugin | `https://raw.githubusercontent.com/apache/beam/v$BEAM_VERSION/buildSrc/src/main/groovy/org/apache/beam/gradle/BeamModulePlugin.groovy` |
| libraries-bom POM (Sonatype) | `https://central.sonatype.com/artifact/com.google.cloud/libraries-bom/$LIBRARIES_BOM_VERSION` |
| google-cloud-bom POM | `https://raw.githubusercontent.com/googleapis/java-cloud-bom/main/google-cloud-bom/pom.xml` |
| gapic-libraries-bom POM | `https://raw.githubusercontent.com/googleapis/google-cloud-java/v$GAPIC_VERSION/gapic-libraries-bom/pom.xml` |
| Individual library BOM | `https://raw.githubusercontent.com/googleapis/google-cloud-java/v$GAPIC_VERSION/$LIB_NAME/$LIB_NAME-bom/pom.xml` |
| first-party-dependencies properties | `https://raw.githubusercontent.com/googleapis/sdk-platform-java/main/gapic-generator-java-pom-parent/pom.xml` |

---

## Edge cases & gotchas

- **`datastore-v1-proto-client`** lives under group `com.google.cloud.datastore`,
  NOT `com.google.cloud`. It has its own versioning scheme separate from
  `google-cloud-datastore`. Beam pins it explicitly — always prefer Beam's value.

- **`proto-google-cloud-bigtable-v2`** lives under group `com.google.api.grpc`.
  Its version matches the `google-cloud-bigtable` main artifact version (they
  share a BOM). Beam delegates this to the libraries-bom, so resolve from BOM.

- **`bigqueryVersion`** in Magnolify is for `com.google.apis:google-api-services-bigquery`,
  not `com.google.cloud:google-cloud-bigquery`. This is a Google API Client
  library artifact, not a Cloud Client library, so it is NOT in the libraries-bom.
  Take it from Beam's `google_api_services_bigquery` line.

- **Guava** is set in `first-party-dependencies` via `guava-bom`. Beam also pins
  a `guava_version`. Prefer Beam's pin unless it looks stale compared to the BOM
  (Beam sometimes lags the BOM by a minor version — in that case, note the
  discrepancy and prefer Beam's pin to stay compatible with Beam at runtime).

- **sdk-platform-java tags** don't match the `first-party-dependencies` Maven
  artifact version (e.g. repo tag `v2.68.0` ≠ Maven version `3.64.0`). Use the
  `main` branch as a reasonable approximation, or search the GitHub API for the
  correct tag.

- **The java-cloud-bom repo was archived** on 2026-07-14. If fetching from `main`
  fails, try the tag matching the libraries-bom version (e.g. `v26.85.0`).

- **Protobuf major version bumps** (e.g. 3.x → 4.x) are significant. If the BOM
  introduces one, warn the user prominently.
