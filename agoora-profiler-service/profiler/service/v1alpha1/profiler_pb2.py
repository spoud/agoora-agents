# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: profiler/service/v1alpha1/profiler.proto

from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from profiler.domain.v1alpha1 import domain_pb2 as profiler_dot_domain_dot_v1alpha1_dot_domain__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='profiler/service/v1alpha1/profiler.proto',
  package='io.spoud.sdm.profiler.v1alpha1',
  syntax='proto3',
  serialized_options=b'\n&io.spoud.sdm.profiler.service.v1alpha1P\001',
  serialized_pb=b'\n(profiler/service/v1alpha1/profiler.proto\x12\x1eio.spoud.sdm.profiler.v1alpha1\x1a%profiler/domain/v1alpha1/domain.proto\"7\n\x0eProfileRequest\x12\x12\n\nrequest_id\x18\x01 \x01(\t\x12\x11\n\tjson_data\x18\x02 \x01(\t\"w\n\x19ProfileDataStreamResponse\x12;\n\x04meta\x18\x01 \x01(\x0b\x32+.io.spoud.sdm.profiler.domain.v1alpha1.MetaH\x00\x12\x11\n\x07profile\x18\x02 \x01(\tH\x00\x42\n\n\x08response\"Z\n\x11InspectionRequest\x12\x14\n\x0csamples_json\x18\x01 \x01(\t\x12\x13\n\x0bschema_json\x18\x02 \x01(\t\x12\x1a\n\x12is_schema_inferred\x18\x03 \x01(\x08\"\xbc\x01\n\x1cInspectionDataStreamResponse\x12G\n\x06metric\x18\x01 \x01(\x0b\x32\x35.io.spoud.sdm.profiler.domain.v1alpha1.QualityMetricsH\x00\x12G\n\x05\x65rror\x18\x02 \x01(\x0b\x32\x36.io.spoud.sdm.profiler.domain.v1alpha1.InspectionErrorH\x00\x42\n\n\x08response2\x95\x02\n\x08Profiler\x12\x82\x01\n\x11ProfileDataStream\x12..io.spoud.sdm.profiler.v1alpha1.ProfileRequest\x1a\x39.io.spoud.sdm.profiler.v1alpha1.ProfileDataStreamResponse(\x01\x30\x01\x12\x83\x01\n\x0eInspectQuality\x12\x31.io.spoud.sdm.profiler.v1alpha1.InspectionRequest\x1a<.io.spoud.sdm.profiler.v1alpha1.InspectionDataStreamResponse(\x01\x42*\n&io.spoud.sdm.profiler.service.v1alpha1P\x01\x62\x06proto3'
  ,
  dependencies=[profiler_dot_domain_dot_v1alpha1_dot_domain__pb2.DESCRIPTOR,])




_PROFILEREQUEST = _descriptor.Descriptor(
  name='ProfileRequest',
  full_name='io.spoud.sdm.profiler.v1alpha1.ProfileRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='request_id', full_name='io.spoud.sdm.profiler.v1alpha1.ProfileRequest.request_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='json_data', full_name='io.spoud.sdm.profiler.v1alpha1.ProfileRequest.json_data', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=115,
  serialized_end=170,
)


_PROFILEDATASTREAMRESPONSE = _descriptor.Descriptor(
  name='ProfileDataStreamResponse',
  full_name='io.spoud.sdm.profiler.v1alpha1.ProfileDataStreamResponse',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='meta', full_name='io.spoud.sdm.profiler.v1alpha1.ProfileDataStreamResponse.meta', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='profile', full_name='io.spoud.sdm.profiler.v1alpha1.ProfileDataStreamResponse.profile', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
    _descriptor.OneofDescriptor(
      name='response', full_name='io.spoud.sdm.profiler.v1alpha1.ProfileDataStreamResponse.response',
      index=0, containing_type=None, fields=[]),
  ],
  serialized_start=172,
  serialized_end=291,
)


_INSPECTIONREQUEST = _descriptor.Descriptor(
  name='InspectionRequest',
  full_name='io.spoud.sdm.profiler.v1alpha1.InspectionRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='samples_json', full_name='io.spoud.sdm.profiler.v1alpha1.InspectionRequest.samples_json', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='schema_json', full_name='io.spoud.sdm.profiler.v1alpha1.InspectionRequest.schema_json', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='is_schema_inferred', full_name='io.spoud.sdm.profiler.v1alpha1.InspectionRequest.is_schema_inferred', index=2,
      number=3, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=293,
  serialized_end=383,
)


_INSPECTIONDATASTREAMRESPONSE = _descriptor.Descriptor(
  name='InspectionDataStreamResponse',
  full_name='io.spoud.sdm.profiler.v1alpha1.InspectionDataStreamResponse',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='metric', full_name='io.spoud.sdm.profiler.v1alpha1.InspectionDataStreamResponse.metric', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='error', full_name='io.spoud.sdm.profiler.v1alpha1.InspectionDataStreamResponse.error', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
    _descriptor.OneofDescriptor(
      name='response', full_name='io.spoud.sdm.profiler.v1alpha1.InspectionDataStreamResponse.response',
      index=0, containing_type=None, fields=[]),
  ],
  serialized_start=386,
  serialized_end=574,
)

_PROFILEDATASTREAMRESPONSE.fields_by_name['meta'].message_type = profiler_dot_domain_dot_v1alpha1_dot_domain__pb2._META
_PROFILEDATASTREAMRESPONSE.oneofs_by_name['response'].fields.append(
  _PROFILEDATASTREAMRESPONSE.fields_by_name['meta'])
_PROFILEDATASTREAMRESPONSE.fields_by_name['meta'].containing_oneof = _PROFILEDATASTREAMRESPONSE.oneofs_by_name['response']
_PROFILEDATASTREAMRESPONSE.oneofs_by_name['response'].fields.append(
  _PROFILEDATASTREAMRESPONSE.fields_by_name['profile'])
_PROFILEDATASTREAMRESPONSE.fields_by_name['profile'].containing_oneof = _PROFILEDATASTREAMRESPONSE.oneofs_by_name['response']
_INSPECTIONDATASTREAMRESPONSE.fields_by_name['metric'].message_type = profiler_dot_domain_dot_v1alpha1_dot_domain__pb2._QUALITYMETRICS
_INSPECTIONDATASTREAMRESPONSE.fields_by_name['error'].message_type = profiler_dot_domain_dot_v1alpha1_dot_domain__pb2._INSPECTIONERROR
_INSPECTIONDATASTREAMRESPONSE.oneofs_by_name['response'].fields.append(
  _INSPECTIONDATASTREAMRESPONSE.fields_by_name['metric'])
_INSPECTIONDATASTREAMRESPONSE.fields_by_name['metric'].containing_oneof = _INSPECTIONDATASTREAMRESPONSE.oneofs_by_name['response']
_INSPECTIONDATASTREAMRESPONSE.oneofs_by_name['response'].fields.append(
  _INSPECTIONDATASTREAMRESPONSE.fields_by_name['error'])
_INSPECTIONDATASTREAMRESPONSE.fields_by_name['error'].containing_oneof = _INSPECTIONDATASTREAMRESPONSE.oneofs_by_name['response']
DESCRIPTOR.message_types_by_name['ProfileRequest'] = _PROFILEREQUEST
DESCRIPTOR.message_types_by_name['ProfileDataStreamResponse'] = _PROFILEDATASTREAMRESPONSE
DESCRIPTOR.message_types_by_name['InspectionRequest'] = _INSPECTIONREQUEST
DESCRIPTOR.message_types_by_name['InspectionDataStreamResponse'] = _INSPECTIONDATASTREAMRESPONSE
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

ProfileRequest = _reflection.GeneratedProtocolMessageType('ProfileRequest', (_message.Message,), {
  'DESCRIPTOR' : _PROFILEREQUEST,
  '__module__' : 'profiler.service.v1alpha1.profiler_pb2'
  # @@protoc_insertion_point(class_scope:io.spoud.sdm.profiler.v1alpha1.ProfileRequest)
  })
_sym_db.RegisterMessage(ProfileRequest)

ProfileDataStreamResponse = _reflection.GeneratedProtocolMessageType('ProfileDataStreamResponse', (_message.Message,), {
  'DESCRIPTOR' : _PROFILEDATASTREAMRESPONSE,
  '__module__' : 'profiler.service.v1alpha1.profiler_pb2'
  # @@protoc_insertion_point(class_scope:io.spoud.sdm.profiler.v1alpha1.ProfileDataStreamResponse)
  })
_sym_db.RegisterMessage(ProfileDataStreamResponse)

InspectionRequest = _reflection.GeneratedProtocolMessageType('InspectionRequest', (_message.Message,), {
  'DESCRIPTOR' : _INSPECTIONREQUEST,
  '__module__' : 'profiler.service.v1alpha1.profiler_pb2'
  # @@protoc_insertion_point(class_scope:io.spoud.sdm.profiler.v1alpha1.InspectionRequest)
  })
_sym_db.RegisterMessage(InspectionRequest)

InspectionDataStreamResponse = _reflection.GeneratedProtocolMessageType('InspectionDataStreamResponse', (_message.Message,), {
  'DESCRIPTOR' : _INSPECTIONDATASTREAMRESPONSE,
  '__module__' : 'profiler.service.v1alpha1.profiler_pb2'
  # @@protoc_insertion_point(class_scope:io.spoud.sdm.profiler.v1alpha1.InspectionDataStreamResponse)
  })
_sym_db.RegisterMessage(InspectionDataStreamResponse)


DESCRIPTOR._options = None

_PROFILER = _descriptor.ServiceDescriptor(
  name='Profiler',
  full_name='io.spoud.sdm.profiler.v1alpha1.Profiler',
  file=DESCRIPTOR,
  index=0,
  serialized_options=None,
  serialized_start=577,
  serialized_end=854,
  methods=[
  _descriptor.MethodDescriptor(
    name='ProfileDataStream',
    full_name='io.spoud.sdm.profiler.v1alpha1.Profiler.ProfileDataStream',
    index=0,
    containing_service=None,
    input_type=_PROFILEREQUEST,
    output_type=_PROFILEDATASTREAMRESPONSE,
    serialized_options=None,
  ),
  _descriptor.MethodDescriptor(
    name='InspectQuality',
    full_name='io.spoud.sdm.profiler.v1alpha1.Profiler.InspectQuality',
    index=1,
    containing_service=None,
    input_type=_INSPECTIONREQUEST,
    output_type=_INSPECTIONDATASTREAMRESPONSE,
    serialized_options=None,
  ),
])
_sym_db.RegisterServiceDescriptor(_PROFILER)

DESCRIPTOR.services_by_name['Profiler'] = _PROFILER

# @@protoc_insertion_point(module_scope)