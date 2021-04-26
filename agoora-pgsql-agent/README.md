# Agoora Postgresql Agent

This agent will scrape a postgresql database.

It will create offer-state for all tables. If enabled it will profile the tables (extract data samples
and create a profile).

## Configuration

```
--8<-- "components/agoora-pgsql-agent/src/main/resources/application.yml"
```

## Minimal configuration 

```
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://{HOST}:{PORT}/{DATABASE}
QUARKUS_DATASOURCE_USERNAME={USERNAME}
QUARKUS_DATASOURCE_PASSWORD={PASSWORD}
AGOORA_ENDPOINT=app.agoora.com
AGOORA_AUTH_USER_NAME={AGENT_USERNAME}
AGOORA_AUTH_USER_PASSWORD={AGENT_SECRET}
AGOORA_PROFILER_ENDPOINT=localhost:8089
```

## Enable/disable profiling

If you don't want the agent to look into your data you can disable the profiling by using:
```
AGOORA_SCRAPPER_PROFILING_ENABLED=false
```
This way you can avoid that some sample data end up in the UI.
