# SDM Profiler Service  

The Agoora profiler service takes a grpc stream of `ProfileRequest` (id-string, json-string) as input and tries to do a
JSON decode. If successful pandas profiling is run against. It also gives the possibility to inspect a stream for its 
quality (level of specification and integrity). For more information about data quality see [here](#data-quality-inspection).



## Configuration

Environment variables:

| Environment variables | Default | Description |
|-----------------------|----------|------------|
|`PROFILER_SERVER_PORT` | 8089     | Port gRPC servers listens on |
|`PROFILER_TIMEOUT`     | 30       | Seconds until the profiling times out. On huge samples with lots of correlation the profiling takes too long otherwise.


There are two ways to configure the pandas profiler:

1. Put the config file at `config/pandas_config.yaml`. (`/app/config/pandas_config.yaml` within the docker container)
2. Pass environment variables. Note that they are cast to lower case, single `_` to `.` and `__` to `_`, where the `.` are used to construct the config hierarchy.

*Example:*
To change the number of samples displayed, which are set in the setting file at

```yaml
samples:
    head: 10
    tail: 10
```

One can also set `SAMPLES_HEAD=5`. The rule is simply a concatenation of the keys on different levels in yaml by an `_`.

The defaults are set like this:

```yaml
--8<-- "components/agoora-profiler-service/config/pandas_config_defaults.yaml"
```
 
Reference pandas profiling for more details on configuration. 
[See config_default.yaml](https://github.com/pandas-profiling/pandas-profiling/blob/v2.3.0/pandas_profiling/config_default.yaml)

## API

See proto files:

```yaml
--8<-- "components/agoora-profiler-service/proto/profiler/service/v1alpha1/profiler.proto"
```

## Test connectivity by GRPCC request

To send a sample request to the service with GRPCC run:

```bash
grpcc --proto proto/profiler/service/v1alpha1/profiler.proto --address localhost:8089 -i
```

Within the grpcc command line run:

```javascript
let em
em = client.profile(pr)
em.write({id: '1', json_data: '{"x": 0, "y": 2 }'})
em.write({id: '1', json_data: '{"x": 0, "y": 2 }'})
em.write({id: '1', json_data: '{"x": 0, "y": 2 }'})
em.write({id: '1', json_data: '{"x": 0, "y": 2 }'})
em.end()
// here the output will be dumped in a few seconds...
```

## Data quality inspection

At the moment only attribute quality is implemented. So the data will only be validated on attribute
level and no row (e.g. conditional logic between fields) or set (e.g. distribution, continuity) metrics are implemented.
The design however should be open for extension and additional metrics can be implemented.

For attribute quality the following semantics have been defined:

#### Attribute quality
This is the composite metric of integrity and specification because the quality of data only implies
corresponding expectations, respectively specifications. In the current implementation attribute quality weights
both integrity and specification equally.

#### Attribute specification
Attribute specification measures how precisely the expectations of the schema (e.g. type, min, max) are. The expectations
can be defined on numbers and strings separately: 

##### Numbers
* 0%: no type is specified
* 50%: type is specified
* 75%: type and minimum or maximum is specified
* 100%: type and both minimum and maximum is specified

##### Strings
* 0%: no type is specified
* 50%: type is specified
* 100%: type and a regex pattern is specified

#### Attribute integrity 
Integrity evaluates how many of the samples are compliant with the specification. 

