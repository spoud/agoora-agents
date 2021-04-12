# Kafka Agent

This agents target Kafka. It will:
- Synchronisation of Kafka Topics
- Synchronisation of Kafka Consumer groups
- Scrape schema registries for schemas
- Forwarding of metrics to SDM
- Profile data (if enabled)

## Configuration

```
--8<-- "components/agoora-agents/agoora-kafka-agent/src/main/resources/application.yml"
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

### Kafka ACLs (`bin/kafka-acls.sh`)

```bash
bin/kafka-acls.sh --add --allow-principal <principal> --operation Describe --topic '*'
bin/kafka-acls.sh --add --allow-principal <principal> --operation Describe --group '*'
```

## Monitoring

 * `/actuator` => shows every actuator endpoint
 * `/actuator/kafka` => shows every topic and consumer group available
 * `/actuator/prometheus` => shows metrics in the prometheus format
