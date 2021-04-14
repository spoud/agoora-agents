import json
from quality_inspection.schema_definition import SchemaDefinition

TEST_DATA_FOLDER = 'testdata'


class DataLoader(object):
    @classmethod
    def load_schema(cls) -> str:
        return cls.load_schema_with_name("schema_samples.json")

    @classmethod
    def load_schema_json(cls) -> str:
        return cls.load_schema_with_name("schema_samples_json.json")

    @classmethod
    def load_samples(cls) -> list:
        with open(f"{TEST_DATA_FOLDER}/samples.json") as json_file:
            samples = json.load(json_file)
        return samples

    @classmethod
    def load_samples_from_file(cls, file_name: str) -> list:
        with open(f"{TEST_DATA_FOLDER}/{file_name}") as json_file:
            samples = json.load(json_file)
        return samples

    @classmethod
    def load_schema_with_name(cls, schema_name:str) -> str:
        with open(f"{TEST_DATA_FOLDER}/{schema_name}") as json_file:
            return json_file.read()

    @classmethod
    def create_dummy_samples(cls) -> (list, str):
        samples = [
            {"random_string": "foo"},
            {"random_string": "bar"}
        ]

        schema_definition = cls.expand_schema(
            [("random_string", "string")],
            []
        )

        return samples, schema_definition

    @classmethod
    def expand_schema(cls, types: [], required_types: [] = None, expectations: dict = None) -> SchemaDefinition:
        if required_types is None:
            required_types = []
        if expectations is None:
            expectations = {}
        properties = ""
        for name, t in types:
            type_description = f'"{t}"' if name in required_types else f'["{t}", "null"]'
            properties += '{"name": "' + name + '","type": ' + type_description
            if name in expectations.keys():
                for key, value in expectations[name].items():
                    inner_value = f'"{value}"' if type(value) is str else value
                    properties += f', "{key}": {inner_value}'
            properties += '},'

        schema = r'''
                   {
                    "type": "record",
                    "name": "RandomMessage",
                    "namespace": "data.producer.random",
                    "fields": [
                        #PROPERTIES#
                    ]
                }
                   '''

        schema_content = schema \
            .replace("#PROPERTIES#", properties.rstrip(','))

        return SchemaDefinition.create(schema_content, False)
