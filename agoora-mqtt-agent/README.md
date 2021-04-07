# SDM MQTT Agent

This agent will scrape an MQTT broker.

## Configuration

```
--8<-- "components/agoora-agents/agoora-mqtt-agent/src/main/resources/application.yml"
```

## Minimal configuration 

```

SDM_ENDPOINT=app.agoora.com
SDM_AUTH_USER_NAME={AGENT_USERNAME}
SDM_AUTH_USER_PASSWORD={AGENT_SECRET}
SDM_PROFILER_ENDPOINT=localhost:8089
SDM_TRANSPORT_PATH=/your/path/
SDM_MQTT_BROKER=tcp://1.2.3.4:1883
SDM_MQTT_USERNAME=username
SDM_MQTT_PASSWORD=******
SDM_MQTT_PATHS=/base-path1/,/my-base-2/
```

## Enable/disable profiling

If you don't want the agent to look into your data you can disable the profiling by using:
```
SDM_SCRAPPER_PROFILING_ENABLED=false
```
This way you can avoid that some sample data end up in the UI.
