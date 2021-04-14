import json
import unittest
from concurrent import futures
import grpc
from profiler.service.v1alpha1 import profiler_pb2 as profiler_pb2
from profiler.service.v1alpha1 import profiler_pb2_grpc as profiler_pb2_grpc
from profiler.domain.v1alpha1 import domain_pb2 as domain_pb2
from profiler.domain.v1alpha1 import domain_pb2_grpc as domain_pb2_grpc
import profiler_app


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


    def test_inspect_stream_quality(self):
        # arrange
        def request_messages():
            request_id = "123"
            with open('testdata/schema_samples.json') as schema_file:
                schema = schema_file.read()
                with open('testdata/samples.json') as samples_file:
                    all_samples = json.loads(samples_file.read())
                    for line in all_samples:
                        yield profiler_pb2.InspectionRequest(
                            samples_json=json.dumps(line),
                            schema_json=schema,
                            is_schema_inferred=False
                        )

        # act
        result = self._stub.InspectQuality(request_messages())

        # assert
        self.assertEqual(0.8125, result.metric.attribute_quality_index)
        self.assertIsNotNone(result.metric.attribute_details, "Quality details must not be null")
        details = {detail.name: (detail.integrity, detail.specification, detail.quality_index) for detail
                   in result.metric.attribute_details}
        self.assertTrue("random_integer" in details.keys(),
                        "Quality details for random_integer is missing")
        self.assertEqual(1.0, details["random_integer"][0],
                         "Attribute integrity for random_integer is not correct")
        self.assertEqual(.5, details["random_integer"][1],
                         "Attribute specification for random_integer is not correct")
        self.assertEqual(.75, details["random_integer"][2],
                         "Quality details for random_integer is not correct")

    def test_inspect_stream_quality_invalid_schema(self):
        # arrange
        def request_messages():
            request_id = "123"
            with open('testdata/samples.json') as samples_file:
                with open('testdata/schema_invalid.json') as schema_file:
                    yield profiler_pb2.InspectionRequest(
                            samples_json=samples_file.read(),
                            schema_json=schema_file.read()
                    )

        # act
        result = self._stub.InspectQuality(request_messages())

        # assert
        self.assertIsNotNone(result.error)
        self.assertEqual(0.0, result.metric.attribute_quality_index, "Quality index must be 0%")
        self.assertNotEqual("", result.error.message)

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

        self.assertRegex(message.profile, r'Profile report generated with the `pandas-profiling` Python package')

    def test_profile_stream_no_error_on_data(self):
        def request_messages():
            request_id = "123"
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
                self.assertEqual(json.loads(message.meta.request_id), 123)
                self.assertEqual(5, message.meta.total_records)
                self.assertSchema(message.meta.schema, 'x', 'string', 'y', 'integer')
            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile
        self.assertRegex(message.profile, r'Profile report generated with the `pandas-profiling` Python package')

    def test_profile_stream_json_normalize(self):
        def request_messages():
            request_id = "123"
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
                self.assertEqual(json.loads(message.meta.request_id), 123)
                self.assertEqual(5, message.meta.total_records)
                self.assertSchema(message.meta.schema, 'x', 'object', 'foo', 'string')
                self.assertSchema(message.meta.schema, 'x', 'object', 'test', 'integer')
            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile

        self.assertRegex(message.profile, r'Profile report generated with the `pandas-profiling` Python package')
        self.assertRegex(message.profile, r'x/foo')
        self.assertRegex(message.profile, r'x/test')

    def test_profile_stream_size_toeggolomat(self):
        def request_messages():
            request_id = "123"
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

                self.assertEqual(json.loads(message.meta.request_id), 123)
                self.assertEqual(27, message.meta.total_records)
                self.assertSchema(message.meta.schema, 'matchUuid', 'string', 'blueScore', 'integer')
            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile

    def test_profile_stream_size_huge_report_timeout(self):
        def request_messages():
            request_id = "123"
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

                self.assertEqual(json.loads(message.meta.request_id), 123)
                self.assertEqual(3, message.meta.total_records)
                self.assertSchema(message.meta.schema, 'field1', 'string', 'field101', 'string')

            elif profile.HasField("profile"):
                num_returned += 1
                message.profile += profile.profile

        # Validate error thrown
        self.assertEqual(0, message.meta.error.type)

    def test_profile_stream_not_so_huge_report_returns_2_profile_messages(self):
        def request_messages():
            request_id = "123"
            with open(
                'testdata/not-so-huge-report.json') as json_file:
                data = json.load(json_file)
                for d in data:
                    request = profiler_pb2.ProfileRequest(
                        request_id=request_id,
                        json_data=json.dumps(d))
                    yield request

        # os.getenv('PROFILER_TIMEOUT', '30')
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
