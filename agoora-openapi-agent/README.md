# agoora-openapi-agent

This agent scrape an Open API endpoint and extract all tags as Data Port and all method/path 
as Data Item.

The schema is also extracted, but you will find everything in the same schema (request param, request body,
responses, ...)

## Configuration

The required configuration is:
```shell
AGOORA_ENDPOINT=app.agoora.com
AGOORA_TRANSPORT_PATH=/public/openapi
AGOORA_AUTH_USER_NAME=${AGENT_USERNAME}
AGOORA_AUTH_USER_TOKEN=${AGENT_TOKEN}
AGOORA_OPENAPI_BASE_URL=https://petstore3.swagger.io/
```
