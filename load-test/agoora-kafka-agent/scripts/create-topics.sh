#!/bin/bash

BOOTSTRAP_SERVER="kafka:9092"
SCHEMA_REGISTRY="http://schema-registry:8081"
NUMBER_OF_TOPIC=1000
PARTITIONS_PER_TOPIC=10

function create_schema(){
  SUBJECT=$1
  content=$(cat schema.json)
  curl -X POST --location "${SCHEMA_REGISTRY}/subjects/${SUBJECT}/versions" \
    --data "@schema.json" \
    --header "Content-Type:application/json" > /dev/null 2>&1
}

for (( i=1; i<=$NUMBER_OF_TOPIC; i++ ))
do
  TOPIC_NAME="topic-$(printf "%04d" $i)"
  echo "creating topic $TOPIC_NAME"
  kafka-topics --bootstrap-server="$BOOTSTRAP_SERVER" --create --partitions=$PARTITIONS_PER_TOPIC --topic "$TOPIC_NAME"
#  create_schema "${TOPIC_NAME}-key"
  create_schema "${TOPIC_NAME}-value"
done

echo ""
COUNT=$(kafka-topics --bootstrap-server=$BOOTSTRAP_SERVER --list | wc | awk '{print $1}')
echo "topic count: $COUNT"
