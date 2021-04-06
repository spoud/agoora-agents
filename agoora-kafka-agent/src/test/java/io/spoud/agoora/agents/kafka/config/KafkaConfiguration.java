package io.spoud.agoora.agents.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import java.util.Properties;

@Dependent
public class KafkaConfiguration {

    @Produces
    KafkaAdminClient kafkaAdminClient(
            @ConfigProperty(name = "agoora.kafka.bootstrap-servers") String bootstrapServer){
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        return (KafkaAdminClient) KafkaAdminClient.create(props);
    }
}
