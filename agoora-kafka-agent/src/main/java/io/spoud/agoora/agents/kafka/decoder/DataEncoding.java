package io.spoud.agoora.agents.kafka.decoder;

import io.spoud.sdm.looker.domain.v1alpha1.DataSample;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DataEncoding {
  UNKNOWN(false, DataSample.Encoding.UNKNOWN, SchemaEncoding.Type.UNKNOWN),
  UNRECOGNIZED(false, DataSample.Encoding.UNRECOGNIZED, SchemaEncoding.Type.UNRECOGNIZED),
  JSON(true, DataSample.Encoding.JSON, SchemaEncoding.Type.JSON),
  XML(true, DataSample.Encoding.XML, SchemaEncoding.Type.JSON),
  PROTOBUF(true, DataSample.Encoding.XML, SchemaEncoding.Type.JSON),
  AVRO(true, DataSample.Encoding.AVRO, SchemaEncoding.Type.AVRO);

  private final boolean valid;
  private final DataSample.Encoding dataSampleEncoding;
  private final SchemaEncoding.Type schemaEncoding;
}
