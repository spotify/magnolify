#!/bin/bash

TMP=$(mktemp -d -t magnolify-XXXXX)

echo "Generating test files in $TMP"
sbt "avro/test:run $TMP; bigquery/test:run $TMP"

bq mk scio-playground:magnolify_test

echo "Testing Avro records"
# Expected
# +---------+---------------------+------------+----------+----------------------------+
# |   bd    |         ts          |     d      |    t     |             dt             |
# +---------+---------------------+------------+----------+----------------------------+
# | 123.456 | 2020-08-01 01:23:45 | 2020-08-01 | 01:23:45 | 2020-08-01 01:23:45.000000 |
# +---------+---------------------+------------+----------+----------------------------+
for name in $(find $TMP -name '*.avro' | sort); do
    echo "Testing $name"
    TABLE="scio-playground:magnolify_test.$(basename $name | cut -d '.' -f 1)"
    bq load --source_format AVRO --use_avro_logical_types $TABLE $name
    bq head $TABLE
done

echo "Testing TableRow records"
# Expected
# +---------+---------------------+------------+----------+---------------------+
# |   bd    |         ts          |     d      |    t     |         dt          |
# +---------+---------------------+------------+----------+---------------------+
# | 123.456 | 2020-08-01 01:23:45 | 2020-08-01 | 01:23:45 | 2020-08-01T01:23:45 |
# +---------+---------------------+------------+----------+---------------------+
for name in $(find $TMP -name '*.json' -not -name schema.json | sort); do
    echo "Testing $name"
    TABLE="scio-playground:magnolify_test.$(basename $name | cut -d '.' -f 1)"
    bq load --source_format NEWLINE_DELIMITED_JSON $TABLE $name $TMP/schema.json
    bq head $TABLE
done

bq rm -r -f scio-playground:magnolify_test
rm -rf $TMP
