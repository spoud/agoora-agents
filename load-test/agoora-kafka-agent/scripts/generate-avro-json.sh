#!/bin/bash

jq '. | {schemaType:"AVRO", schema: tojson}' schema.avsc
