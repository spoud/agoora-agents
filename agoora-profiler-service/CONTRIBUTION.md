## Development

### Local

To run the agoora-profiler-service locally, do:

```
pip3 install -Ur requirements.txt
python3 -m grpc_tools.protoc -I./proto --python_out=./ --grpc_python_out=./ ./proto/profiler/service/v1alpha1/profiler.proto
```

### Docker
```
docker build . -f Dockerfile.jvm -t spoud/agoora-profiler-service:latest
docker run -it --rm -p 8089:8089 spoud/agoora-profiler-service:latest
```

### GRPCC request
To send a sample request to the service with GRPCC run:
```
grpcc --proto proto/profiler.proto --address localhost:8089 -i
client.profile({id: '1', json_data: '[{"x": 0, "y": 2 }, {"x": 1, "y": 3}]'}, printReply)
```

## Tests

To run the tests for the integration locally:

```bash
    mkdir -p testresults && python profiler_it.py && rm -rf testresults
```


To run the unit tests of the calculations:

```bash
    python -m unittest quality_inspection/tests/*
```

## Licences
```
pip3 install third-party-license-file-generator
python3 -m third_party_license_file_generator -r requirements.txt -p `which python3`

```
