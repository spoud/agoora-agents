# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
import grpc

from profiler.service.v1alpha1 import profiler_pb2 as profiler_dot_service_dot_v1alpha1_dot_profiler__pb2


class ProfilerStub(object):
    """Missing associated documentation comment in .proto file"""

    def __init__(self, channel):
        """Constructor.

        Args:
            channel: A grpc.Channel.
        """
        self.ProfileDataStream = channel.stream_stream(
                '/io.spoud.sdm.profiler.v1alpha1.Profiler/ProfileDataStream',
                request_serializer=profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.ProfileRequest.SerializeToString,
                response_deserializer=profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.ProfileDataStreamResponse.FromString,
                )
        self.InspectQuality = channel.stream_unary(
                '/io.spoud.sdm.profiler.v1alpha1.Profiler/InspectQuality',
                request_serializer=profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.InspectionRequest.SerializeToString,
                response_deserializer=profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.InspectionDataStreamResponse.FromString,
                )


class ProfilerServicer(object):
    """Missing associated documentation comment in .proto file"""

    def ProfileDataStream(self, request_iterator, context):
        """Missing associated documentation comment in .proto file"""
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def InspectQuality(self, request_iterator, context):
        """Missing associated documentation comment in .proto file"""
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')


def add_ProfilerServicer_to_server(servicer, server):
    rpc_method_handlers = {
            'ProfileDataStream': grpc.stream_stream_rpc_method_handler(
                    servicer.ProfileDataStream,
                    request_deserializer=profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.ProfileRequest.FromString,
                    response_serializer=profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.ProfileDataStreamResponse.SerializeToString,
            ),
            'InspectQuality': grpc.stream_unary_rpc_method_handler(
                    servicer.InspectQuality,
                    request_deserializer=profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.InspectionRequest.FromString,
                    response_serializer=profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.InspectionDataStreamResponse.SerializeToString,
            ),
    }
    generic_handler = grpc.method_handlers_generic_handler(
            'io.spoud.sdm.profiler.v1alpha1.Profiler', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))


 # This class is part of an EXPERIMENTAL API.
class Profiler(object):
    """Missing associated documentation comment in .proto file"""

    @staticmethod
    def ProfileDataStream(request_iterator,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.stream_stream(request_iterator, target, '/io.spoud.sdm.profiler.v1alpha1.Profiler/ProfileDataStream',
            profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.ProfileRequest.SerializeToString,
            profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.ProfileDataStreamResponse.FromString,
            options, channel_credentials,
            call_credentials, compression, wait_for_ready, timeout, metadata)

    @staticmethod
    def InspectQuality(request_iterator,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.stream_unary(request_iterator, target, '/io.spoud.sdm.profiler.v1alpha1.Profiler/InspectQuality',
            profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.InspectionRequest.SerializeToString,
            profiler_dot_service_dot_v1alpha1_dot_profiler__pb2.InspectionDataStreamResponse.FromString,
            options, channel_credentials,
            call_credentials, compression, wait_for_ready, timeout, metadata)