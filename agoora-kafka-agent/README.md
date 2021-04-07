# Kafka Agent

It implements all transport related management responsibilities for SDM:

- Synchronisation of Kafka Topics between Logistics and Kafka
- Scrape schema registries for schemas
- Forwarding of metrics to SDM

## Configuration

```
--8<-- "components/agoora-agents/agoora-kafka-agent/src/main/resources/application.properties"
```

### SSL

If you want to use SSL with kafka, use those environment variable:

```bash
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
SPRING_KAFKA_PROPERTIES_PROTOCOL=SSL
SPRING_KAFKA_ADMIN_SSL_KEY_STORE_LOCATION=/data/keys/...
SPRING_KAFKA_ADMIN_SSL_KEY_STORE_PASSWORD=123456
SPRING_KAFKA_ADMIN_SSL_TRUST_STORE_LOCATION=/data/keys...
SPRING_KAFKA_ADMIN_SSL_TRUST_STORE_PASSWORD=123456
SPRING_KAFKA_CONSUMER_SSL_KEY_STORE_LOCATION=/data/keys/...
SPRING_KAFKA_CONSUMER_SSL_KEY_STORE_PASSWORD=123456
SPRING_KAFKA_CONSUMER_SSL_TRUST_STORE_LOCATION=/data/keys...
SPRING_KAFKA_CONSUMER_SSL_TRUST_STORE_PASSWORD=123456
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

### Kafka ACLs (`bin/kafka-acls.sh`)

```bash
bin/kafka-acls.sh --add --allow-principal <principal> --operation Describe --topic '*'
bin/kafka-acls.sh --add --allow-principal <principal> --operation Describe --group '*'
```

## Monitoring

 * `/actuator` => shows every actuator endpoint
 * `/actuator/kafka` => shows every topic and consumer group available
 * `/actuator/prometheus` => shows metrics in the prometheus format
