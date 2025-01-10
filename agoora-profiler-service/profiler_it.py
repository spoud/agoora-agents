import json
import unittest
from concurrent import futures

import grpc

import profiler_app
from profiler.domain.v1alpha1 import domain_pb2 as domain_pb2
from profiler.service.v1alpha1 import profiler_pb2 as profiler_pb2
from profiler.service.v1alpha1 import profiler_pb2_grpc as profiler_pb2_grpc


class ProfilerTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls._server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
        profiler_pb2_grpc.add_ProfilerServicer_to_server(
            profiler_app.ProfilerServicer(), cls._server)
        cls._server.add_insecure_port('[::]:50051')
        cls._server.start()
        cls._channel = grpc.insecure_channel('localhost:50051')
        cls._stub = profiler_pb2_grpc.ProfilerStub(cls._channel)

    def assertSchema(self, schema, first_field_name, first_field_type, random_field_name, random_field_type):
        self.assertRegex(schema,
                         r'^{"\$schema": "http://json-schema.org/schema#", "type": "object", "properties": {"' +
                         first_field_name + '": {"type": "' + first_field_type + '".*"' +
                         random_field_name + '": {"type": "' + random_field_type + '".*]}$')

    # =========
    # Tests on ProfileDataStream
    # =========

    def test_profile_stream_error_no_data(self):
        def request_messages():
            request_id = "123"
            for i in range(5):
                request = profiler_pb2.ProfileRequest(
                    request_id=request_id)
                yield request

        # test
        num_returned = 0
        message = profiler_pb2.ProfileDataStreamResponse()
        for profile in self._stub.ProfileDataStream(request_messages()):
            if profile.HasField("meta"):
                num_returned += 1
                message.meta.request_id = profile.meta.request_id
                message.meta.schema = profile.meta.schema
                message.meta.total_records = profile.meta.total_records
                message.meta.service_version = profile.meta.service_version
                message.meta.schema_byte_size = profile.meta.schema_byte_size
                message.meta.profile_byte_size = profile.meta.profile_byte_size
                message.meta.error.message = profile.meta.error.message
                message.meta.error.type = profile.meta.error.type
            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile

        self.assertEqual(json.loads(message.meta.request_id), 123)
        self.assertEqual(0, message.meta.total_records)
        self.assertEqual(message.meta.error.type, domain_pb2.ProfilerError.Type.Value('UNKNOWN_ENCODING'))

    def test_profile_stream_error_no_json_string(self):
        def request_messages():
            request_id = "123"
            for i in range(5):
                request = profiler_pb2.ProfileRequest(
                    request_id=request_id,
                    json_data='this crap'
                )
                yield request

        # test
        num_returned = 0
        message = profiler_pb2.ProfileDataStreamResponse()
        for profile in self._stub.ProfileDataStream(request_messages()):
            if profile.HasField("meta"):
                num_returned += 1
                message.meta.request_id = profile.meta.request_id
                message.meta.schema = profile.meta.schema
                message.meta.total_records = profile.meta.total_records
                message.meta.service_version = profile.meta.service_version
                message.meta.schema_byte_size = profile.meta.schema_byte_size
                message.meta.profile_byte_size = profile.meta.profile_byte_size
                message.meta.error.message = profile.meta.error.message
                message.meta.error.type = profile.meta.error.type
            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile

        self.assertEqual(json.loads(message.meta.request_id), 123)
        self.assertEqual(0, message.meta.total_records)
        self.assertEqual(message.meta.error.type, domain_pb2.ProfilerError.Type.Value('UNKNOWN_ENCODING'))

    def test_profile_stream_no_error_without_request_id(self):
        def request_messages():
            for i in range(5):
                request = profiler_pb2.ProfileRequest(
                    json_data=json.dumps({'x': 'a', 'y': 2})
                )
                yield request

        # test
        num_returned = 0
        message = profiler_pb2.ProfileDataStreamResponse()
        for profile in self._stub.ProfileDataStream(request_messages()):
            if profile.HasField("meta"):
                num_returned += 1
                message.meta.request_id = profile.meta.request_id
                message.meta.schema = profile.meta.schema
                message.meta.total_records = profile.meta.total_records
                message.meta.service_version = profile.meta.service_version
                message.meta.schema_byte_size = profile.meta.schema_byte_size
                message.meta.profile_byte_size = profile.meta.profile_byte_size
                message.meta.error.message = profile.meta.error.message
                message.meta.error.type = profile.meta.error.type
                self.assertSchema(message.meta.schema, 'x', 'string', 'y', 'integer')
                self.assertEqual(5, message.meta.total_records)
            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile
        file = open('testresults/test_profile_stream_no_error_without_request_id.html', 'w')
        file.write(message.profile)
        file.close()
        self.assertRegex(message.profile, r'Profile report generated by YData!')

    def test_profile_stream_no_error_on_data(self):
        def request_messages():
            request_id = "124"
            for i in range(5):
                request = profiler_pb2.ProfileRequest(
                    request_id=request_id,
                    json_data=json.dumps({'x': 'a', 'y': 2}))
                yield request

        # test
        num_returned = 0
        message = profiler_pb2.ProfileDataStreamResponse()
        for profile in self._stub.ProfileDataStream(request_messages()):
            if profile.HasField("meta"):
                num_returned += 1
                message.meta.request_id = profile.meta.request_id
                message.meta.schema = profile.meta.schema
                message.meta.total_records = profile.meta.total_records
                message.meta.service_version = profile.meta.service_version
                message.meta.schema_byte_size = profile.meta.schema_byte_size
                message.meta.profile_byte_size = profile.meta.profile_byte_size
                self.assertEqual(json.loads(message.meta.request_id), 124)
                self.assertEqual(5, message.meta.total_records)
                self.assertSchema(message.meta.schema, 'x', 'string', 'y', 'integer')
            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile
        file = open('testresults/test_profile_stream_no_error_on_data.html', 'w')
        file.write(message.profile)
        file.close()
        self.assertRegex(message.profile, r'Profile report generated by YData!')

    def test_profile_stream_json_normalize(self):
        def request_messages():
            request_id = "125"
            for i in range(5):
                request = profiler_pb2.ProfileRequest(
                    request_id=request_id,
                    json_data=json.dumps({'x': {'test': 1, 'foo': 'bar'}, 'y': 2}))
                yield request

        # test
        num_returned = 0
        message = profiler_pb2.ProfileDataStreamResponse()
        for profile in self._stub.ProfileDataStream(request_messages()):
            if profile.HasField("meta"):
                num_returned += 1
                message.meta.request_id = profile.meta.request_id
                message.meta.schema = profile.meta.schema
                message.meta.total_records = profile.meta.total_records
                message.meta.service_version = profile.meta.service_version
                message.meta.schema_byte_size = profile.meta.schema_byte_size
                message.meta.profile_byte_size = profile.meta.profile_byte_size
                self.assertEqual(json.loads(message.meta.request_id), 125)
                self.assertEqual(5, message.meta.total_records)
                self.assertSchema(message.meta.schema, 'x', 'object', 'foo', 'string')
                self.assertSchema(message.meta.schema, 'x', 'object', 'test', 'integer')
            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile
        file = open('testresults/test_profile_stream_json_normalize.html', 'w')
        file.write(message.profile)
        file.close()
        self.assertRegex(message.profile, r'Profile report generated by YData!')
        self.assertRegex(message.profile, r'x/foo')
        self.assertRegex(message.profile, r'x/test')

    def test_profile_stream_size_toeggolomat(self):
        def request_messages():
            request_id = "126"
            with open('testdata/toeggelomat_join.json') as json_file:
                data = json.load(json_file)
                for d in data:
                    request = profiler_pb2.ProfileRequest(
                        request_id=request_id,
                        json_data=json.dumps(d))
                    yield request

        # test
        num_returned = 0
        message = profiler_pb2.ProfileDataStreamResponse()
        for profile in self._stub.ProfileDataStream(request_messages()):
            if profile.HasField("meta"):
                num_returned += 1
                message.meta.request_id = profile.meta.request_id
                message.meta.schema = profile.meta.schema
                message.meta.total_records = profile.meta.total_records
                message.meta.service_version = profile.meta.service_version
                message.meta.schema_byte_size = profile.meta.schema_byte_size
                message.meta.profile_byte_size = profile.meta.profile_byte_size

                self.assertEqual(json.loads(message.meta.request_id), 126)
                self.assertEqual(27, message.meta.total_records)
                self.assertSchema(message.meta.schema, 'matchUuid', 'string', 'blueScore', 'integer')
            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile

    def test_profile_stream_size_huge_report_timeout(self):
        def request_messages():
            request_id = "127"
            with open('testdata/huge-report.json') as json_file:
                data = json.load(json_file)
                for d in data:
                    request = profiler_pb2.ProfileRequest(
                        request_id=request_id,
                        json_data=json.dumps(d))
                    yield request

        # test
        num_returned = 0
        message = profiler_pb2.ProfileDataStreamResponse()
        for profile in self._stub.ProfileDataStream(request_messages()):
            if profile.HasField("meta"):
                num_returned += 1
                message.meta.request_id = profile.meta.request_id
                message.meta.schema = profile.meta.schema
                message.meta.total_records = profile.meta.total_records
                message.meta.service_version = profile.meta.service_version
                message.meta.schema_byte_size = profile.meta.schema_byte_size
                message.meta.profile_byte_size = profile.meta.profile_byte_size

                self.assertEqual(json.loads(message.meta.request_id), 127)
                self.assertEqual(3, message.meta.total_records)
                self.assertSchema(message.meta.schema, 'field1', 'string', 'field101', 'string')

            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile

        # Validate error thrown
        self.assertEqual(0, message.meta.error.type)

    def test_profile_stream_not_so_huge_report_returns_2_profile_messages(self):
        # be sure that the sample data produces a report which is larger than 4MB
        def request_messages():
            request_id = "128"
            with open(
                'testdata/not-so-huge-report.json') as json_file:
                data = json.load(json_file)
                for d in data:
                    request = profiler_pb2.ProfileRequest(
                        request_id=request_id,
                        json_data=json.dumps(d))
                    yield request

        # test
        num_returned = 0
        message = profiler_pb2.ProfileDataStreamResponse()
        for profile in self._stub.ProfileDataStream(request_messages()):
            if profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile

        # here we make sure multiple messages were returned to fit the profile size
        self.assertEqual(num_returned, 2)


if __name__ == '__main__':
    unittest.main()
