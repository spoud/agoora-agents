# sdm-openapi-agent

This agent scrape an Open API endpoint and extract all tags as Data Port and all method/path 
as Data Item.

The schema is also extracted, but you will find everything in the same schema (request param, request body,
responses, ...)

## Configuration

The required configuration is:
```shell
SDM_ENDPOINT=app.agoora.com
SDM_TRANSPORT_PATH=/public/openapi
SDM_AUTH_USER_NAME=${AGENT_USERNAME}
SDM_AUTH_USER_TOKEN=${AGENT_TOKEN}
SDM_OPENAPI_BASE_URL=https://petstore3.swagger.io/
```
