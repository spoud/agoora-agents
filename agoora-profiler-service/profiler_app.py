import json
import json.decoder
import logging
import os
from concurrent import futures
from genson import SchemaBuilder
from func_timeout import FunctionTimedOut, func_set_timeout
import matplotlib
import grpc
import pandas as pd
import typing

import ydata_profiling

from pandas import json_normalize
from htmlmin.main import minify

from profiler.service.v1alpha1 import profiler_pb2 as profiler_pb2
from profiler.service.v1alpha1 import profiler_pb2_grpc as profiler_pb2_grpc
from profiler.domain.v1alpha1 import domain_pb2 as domain_pb2

logging.basicConfig(
    format='%(asctime)s: %(levelname)s: %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S',
    level=logging.INFO)


class ProfilerServicer(profiler_pb2_grpc.ProfilerServicer):
    config_path = None

    def __init__(self):

        matplotlib.pyplot.switch_backend('Agg')
        file_path = os.path.dirname(os.path.abspath(__file__))
        config_path_defaults = file_path + '/config/pandas_config_defaults.yaml'
        config_path_custom = file_path + '/config/pandas_config.yaml'
        logging.info('looking up the pandas configuration at ' + config_path_custom)

        if os.path.exists(config_path_defaults):
            self.config_path = config_path_defaults
            logging.info('found default config for the pandas profiler at ' + config_path_defaults)

        if os.path.exists(config_path_custom):
            self.config_path = config_path_custom
            logging.info('found config for the pandas profiler at ' + config_path_custom)
        else:
            logging.info('did not find config for the pandas profiler at ' + config_path_custom +
                         ' will use the default config.')


    def ProfileDataStream(self, request_iterator, context):
        request_id = "none"
        builder = SchemaBuilder()
        builder.add_schema({"type": "object", "properties": {}})
        error = domain_pb2.ProfilerError(type=domain_pb2.ProfilerError.Type.Value('UNKNOWN'))

        message = profiler_pb2.ProfileDataStreamResponse()
        total_records = 0
        record_list = []

        try:
            for record in request_iterator:
                total_records += 1
                request_id = record.request_id
                if total_records == 1:
                    logging.info('started profiling for request %s with config %s' % (request_id, self.config_path))
                json_data = json.loads(record.json_data)
                record_list.append(json_data)

            for jd in record_list:
                builder.add_object(jd)
            data_frame = pd.DataFrame(json_normalize(record_list, sep='/'))

            profile = None
            report_length = 0
            try:
                profile = run_profiler(data_frame, self.config_path)
            except FunctionTimedOut as te:
                err_msg = 'profile timeout for request_id %s after %ss data_frame shape (rows, cols): %s' % \
                          (request_id, te.timedOutAfter, data_frame.shape)
                logging.warning(err_msg)
                error = domain_pb2.ProfilerError(
                    message=err_msg,
                    type=domain_pb2.ProfilerError.Type.Value('PROFILE_EXCEPTION')
                )

            except Exception as e:
                logging.error('generic exception in timeout', e)
                error = domain_pb2.ProfilerError(
                    message=str(e),
                    type=domain_pb2.ProfilerError.Type.Value('PROFILE_EXCEPTION')
                )

            schema = builder.to_schema()

            if profile is not None:
                html = profile.to_html()

                html = minify(
                    html, remove_all_empty_space=True, remove_comments=True
                )

                report_length = len(html)

            schema_json = json.dumps(schema)
            schema_length = len(schema_json)
            logging.info('profiling complete for request %s total_records: %s, schema_length: %s, report_length: %s'
                         % (request_id, total_records, schema_length, report_length))

            profile_stream = []

            # The max message size of a GRPC call in bytes is 4194304. The header includes 5 bytes, 1 for
            # the compressed flag and 4 for the unsigned integer. Therefore should be 4194296
            # and there 3 more bytes in newer versions of grpc
            MAX_MESSAGE_SIZE = 4194150

            if report_length == 0 or html is None:
                profile_stream.append('')
            elif report_length < MAX_MESSAGE_SIZE:
                profile_stream.append(html)
            else:
                last = 0
                while last + MAX_MESSAGE_SIZE < report_length:
                    profile_stream.append(html[last:last + MAX_MESSAGE_SIZE])
                    last = last + MAX_MESSAGE_SIZE
                profile_stream.append(html[last: report_length])

            if error is not None and error.type != domain_pb2.ProfilerError.Type.Value('UNKNOWN'):
                message.meta.error.message = error.message
                message.meta.error.type = error.type

            message.meta.request_id = request_id
            message.meta.schema = schema_json
            message.meta.total_records = total_records
            message.meta.service_version = os.getenv('SDM_PROFILER_SERVICE_VERSION', 'default')
            message.meta.schema_byte_size = schema_length
            message.meta.profile_byte_size = report_length

            yield message

            for idx, profile_portion in enumerate(profile_stream):
                message = profiler_pb2.ProfileDataStreamResponse()
                message.profile = profile_portion
                yield message
            return


        except json.decoder.JSONDecodeError as e:
            first_chars = '><'
            if record is not None and record.json_data is not None:
                first_chars = '>' + record.json_data[0:10] + '<'
            err_msg = 'profiling failed for request %s with error %s %s, record nr: %s first 10 chars %s' % \
                      (request_id, type(e), e, total_records, first_chars)
            logging.error(err_msg)
            error = domain_pb2.ProfilerError(
                message=err_msg,
                type=domain_pb2.ProfilerError.Type.Value('UNKNOWN_ENCODING')
            )

        except Exception as e:
            logging.error('profiling failed for request %s with error %s' % (request_id, e))
            error = domain_pb2.ProfilerError(
                message=str(e),
                type=domain_pb2.ProfilerError.Type.Value('NO_DATA')
            )

        message.meta.request_id = request_id
        message.meta.error.message = error.message
        message.meta.error.type = error.type
        yield message


@func_set_timeout(int(os.getenv('PROFILER_TIMEOUT', '30')))
def run_profiler(data_frame, config_path=None):
    return ydata_profiling.ProfileReport(data_frame, lazy=False, config_file=config_path, explorative=True)  # enforce eager loading as in previous versions


def convert(val):
    constructors = [int, float, bool, str]
    for c in constructors:
        try:
            return c(val)
        except ValueError:
            pass


def serve():
    host_port = '[::]:' + os.getenv('PROFILER_SERVER_PORT', '8089')
    logging.info('Starting profiler server, listening on %s', host_port)
    server = grpc.server(
        thread_pool=futures.ThreadPoolExecutor(max_workers=2),
        maximum_concurrent_rpcs=10)
    profiler_pb2_grpc.add_ProfilerServicer_to_server(ProfilerServicer(), server)
    server.add_insecure_port(host_port)
    server.start()
    server.wait_for_termination()


if __name__ == '__main__':
    serve()
