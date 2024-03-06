# Kafka Agent

This agents target Kafka. It will:
- Synchronisation of Kafka Topics
- Synchronisation of Kafka Consumer groups
- Scrape schema registries for schemas
- Forwarding of metrics to SDM
- Profile data (if enabled)

## Configuration

```
--8<-- "components/agoora-kafka-agent/src/main/resources/application.yml"
```

### SSL

If you want to use SSL with kafka, use those environment variables:

```bash
AGOORA_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
AGOORA_KAFKA_PROTOCOL=SSL
AGOORA_KAFKA_KEY-STORE_LOCATION=/data/keys/...
AGOORA_KAFKA_KEY-STORE_PASSWORD=123456
AGOORA_KAFKA_TRUST-STORE_LOCATION=/data/keys/...
AGOORA_KAFKA_TRUST-STORE_PASSWORD=123456
```

## Kafka Permission Requirements

Minimal permissions required for sdm-kafka-agent:

- `DESCRIBE GROUP '*'`: read all consumer groups (needed for data subscriptions).
- `DESCRIBE TOPIC '*'`: read all topics (needed for data offers)

### Confluent Cloud (`ccloud`):

```bash
ccloud kafka acl create --allow --service-account-id <account-id> --operation describe --topic '*'
ccloud kafka acl create --allow --service-account-id <account-id> --operation describe --consumer-group '*'
```

Configuration:
```
AGOORA_KAFKA_PROTOCOL=SASL_SSL
AGOORA_KAFKA_KEY=
AGOORA_KAFKA_SECRET=
```

### Schema cache settings

The schema by id cache holds the full schema by id. This can become a big object in agents memory.
The topic name and schema id cache holds the schema id by topic name. This is a smaller object in agents memory.

Configuration:
```
AGOORA_SCHEMA_CACHE_SCHEMA_BY_ID= number of schemas to cache by id default 1000
AGOORA_SCHEMA_CACHE_TOPIC_NAME_SCHEMA_ID_CACHE: number of schemas to cache by topic name and schema id default 1000
```

### Kafka ACLs (`bin/kafka-acls.sh`)

```bash
bin/kafka-acls.sh --add --allow-principal <principal> --operation Describe --topic '*'
bin/kafka-acls.sh --add --allow-principal <principal> --operation Describe --group '*'
```

## Monitoring

 * `host:8082/q/health` => Global health endpoint
 * `host:8082/q/health/live` => liveness probe
 * `host:8082/q/health/ready` => readiness

## Additional properties (deep dive tools)

You have the possibility to add properties to the Data Port and Data Subscription State. For this you have to use
a JSON value. There is some variable that you can use. The variables are:

| Variable | Description | 
| --- | --- |
| TOPIC | Topic name |
| TOPIC | Topic name |
| RESOURCE_ID | Data port id or Data subscription state id |

Examples:

```
AGOORA_PROPERTY_TEMPLATES_KAFKA_TOPIC={"data.quality.dashboard":"https://grafana-dev.sdm.spoud.io/d/Zs9TGX7Mk/data-quality-detail?orgId=1&resourceId={RESOURCE_ID}&var-resourceId={RESOURCE_ID}","kafka.manager.url":"https://km.sdm.spoud.io/clusters/sdm/topics/{TOPIC_NAME}","prometheus.url" : "https://prometheus.sdm.spoud.io/graph?g0.range_input=1w&g0.expr=kafka_topic_highwater%7Btopic%3D%22{TOPIC_NAME}%22%7D&g0.tab=0"}
AGOORA_PROPERTY_TEMPLATES_KAFKA_CONSUMER_GROUP={"kafka.manager.url":"https://km.sdm.spoud.io/clusters/sdm/consumers/{CONSUMER_GROUP_NAME}/topic/{TOPIC_NAME}/type/KF"}
```
