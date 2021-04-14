import unittest
import json

from quality_inspection.schema_definition import SchemaDefinition
from quality_inspection.schema_parser import AvroSchemaParser
from quality_inspection.tests.data_loader import DataLoader


class AvroSchemaParserTest(unittest.TestCase):

    def setUp(self) -> None:
        self.parser = AvroSchemaParser()

    def test_load_expectations_from_schema(self):
        # arrange
        schema = '''
                       {
                           "type": "record",
                           "name": "RandomData",
                           "namespace": "data.producer.random",
                           "fields": [
                               {
                                   "name": "random_integer",
                                   "type": "int",
                                   "expectations": [
                                   {
                                      "kwargs": {
                                       "min_value": 0,
                                       "max_value": 10
                                     },
                                     "expectation_type": "expect_column_values_to_be_between"
                                   }
                               ]
                               }
                           ]
                       }
                       '''

        schema_obj = json.loads(schema)

        # act
        _, expectations = self.parser.extract_props_from_schema(schema_obj["fields"])

        # assert
        self.assertDictEqual({'random_integer': [
            {
                "kwargs": {
                    "min_value": 0,
                    "max_value": 10
                },
                "expectation_type": "expect_column_values_to_be_between"
            }
        ]}, expectations)

    def test_load_types_from_schema(self):
        # arrange
        schema = '''
                         {
                    "type": "record",
                    "name": "IssPositionMessage",
                    "namespace": "data.producer.iss",
                    "fields": [
                        {
                            "name": "message",
                            "type": [
                                "string",
                                "null"
                            ]
                        },
                        {
                            "name": "timestamp",
                            "type": "long"
                        }
                    ]
                }
                '''
        schema_obj = json.loads(schema)

        # act
        type_definitions, _ = self.parser.load_types_from_schema(schema_obj)

        # assert
        self.assertDictEqual(type_definitions, {"message": "string", "timestamp": "long"})

    def test_load_required_types_for_schema(self):
        # arrange
        schema = '''
                         {
                    "type": "record",
                    "name": "IssPositionMessage",
                    "namespace": "data.producer.iss",
                    "fields": [
                        {
                            "name": "comment",
                            "type": [
                                "string",
                                "null"
                            ]
                        },
                        {
                            "name": "timestamp",
                            "type": "long"
                        },
                        {
                            "name": "message",
                            "type": "string"
                        }
                    ]
                }
                '''
        schema_obj = json.loads(schema)

        # act
        type_definitions, _ = self.parser.load_required_types_from_schema(schema_obj)

        # assert
        self.assertEqual(["timestamp", "message"], type_definitions)

    def test_load_expectations_asynciostyle_from_schema(self):
        # arrange
        schema = '''
                         {
                    "type": "record",
                    "name": "IssPositionMessage",
                    "namespace": "data.producer.iss",
                    "fields": [
                        {
                            "name": "comment",
                            "type": [
                                "string",
                                "null"
                            ],
                            "pattern": ""
                        },
                        {
                            "name": "timestamp",
                            "type": "long",
                            "minimum": 0,
                            "maximum": 100
                        }
                    ]
                }
                '''
        schema_obj = json.loads(schema)

        # act
        type_definitions, type_expectations = self.parser.load_required_types_from_schema(schema_obj)

        # assert
        comment_expectations = type_expectations["comment"]
        self.assertCountEqual(comment_expectations, ["pattern"])
        timestamp_expectations = type_expectations["timestamp"]
        self.assertCountEqual(["minimum", "maximum"], timestamp_expectations)

    def test_load_required_types_for_deeply_nested_schema(self):
        # arrange
        schema = DataLoader.load_schema_with_name("schema_registry_avro_complex.json")
        schema_obj = json.loads(schema)

        # act
        type_definitions, _ = self.parser.load_required_types_from_schema(schema_obj)

        # assert
        self.assertListEqual([
            "complex/subtypeString",
            "complex/subtypeComplex/subtypeNumber",
            "simpleNumber"], type_definitions)

    def test_load_types_from_schema_complex(self):
        schema = '''
        {
            "type": "record",
            "name": "IssPositionMessage",
            "namespace": "data.producer.iss",
            "fields": [
                {
                    "name": "iss_position",
                    "type": {
                        "type": "record",
                        "name": "IssPosition",
                        "fields": [
                            {
                                "name": "longitude",
                                "type": "string"
                            },
                            {
                                "name": "latitude",
                                "type": "string"
                            }
                        ]
                    }
                },
                {
                    "name": "message",
                    "type": [
                        "string",
                        "null"
                    ]
                },
                {
                    "name": "timestamp",
                    "type": "long"
                }
            ]
        }
        '''
        schema_obj = json.loads(schema)

        # act
        types, _ = self.parser.extract_props_from_schema(schema_obj["fields"])

        # assert
        self.assertDictEqual({
            "iss_position/longitude": "string",
            "iss_position/latitude": "string",
            "message": "string",
            "timestamp": "long"}, types)


    def test_convert_with_min_expectation(self):
        # arrange
        schema = '''
        {
            "fields": [
                {
                    "name": "random_integer",
                    "type": "integer",
                    "minimum": 0
                }
            ]
        }
        '''
        expected_schema = '''
        {
           "fields": [
               {
                   "name": "random_integer",
                   "type": "integer",
                   "minimum": 0,
                   "expectations": [
                       {
                          "kwargs": {
                           "min_value": 0
                         },
                         "expectation_type": "expect_column_values_to_be_between"
                       }
                   ]
               }
           ]
        }
        '''

        # act
        result = self.parser.convert_expectations(SchemaDefinition.create(schema, False))

        # assert
        self.assertStingEqualAsDict(result.schema_content, expected_schema)

    def test_convert_with_min_max_expectation(self):
        # arrange
        schema = '''
        {
            "fields": [
                {
                    "name": "random_integer",
                    "type": "integer",
                    "minimum": 0,
                    "maximum": 10
                }
            ]
        }
        '''
        expected_schema = '''
        {
           "fields": [
               {
                   "name": "random_integer",
                   "type": "integer",
                   "minimum": 0,
                   "maximum": 10,
                   "expectations": [
                       {
                          "kwargs": {
                           "min_value": 0,
                           "max_value": 10
                         },
                         "expectation_type": "expect_column_values_to_be_between"
                       }
                   ]
               }
           ]
        }
        '''

        # act
        result = self.parser.convert_expectations(SchemaDefinition.create(schema, False))

        # assert
        self.assertStingEqualAsDict(result.schema_content, expected_schema)

    def test_convert_with_multiple_expectations(self):
        # arrange
        schema = '''
        {
            "fields": [
                {
                    "name": "random_integer",
                    "type": "integer",
                    "minimum": 0,
                    "maximum": 3
                },
                {
                    "name": "random_string",
                    "type": "string",
                    "pattern": "id_"
                }
            ]
        }
        '''
        expected_schema = '''
        {
           "fields": [
               {
                   "name": "random_integer",
                   "type": "integer",
                   "minimum": 0,
                   "maximum": 3,
                   "expectations": [
                   {
                          "kwargs": {
                           "min_value": 0,
                           "max_value": 3
                         },
                         "expectation_type": "expect_column_values_to_be_between"
                       }
                   ]
               },
                {
                   "name": "random_string",
                   "type": "string",
                   "pattern": "id_",
                   "expectations": [
                   {
                         "kwargs": {
                           "regex": "id_"
                         },
                         "expectation_type": "expect_column_values_to_match_regex"
                       }
                   ]
               }
           ]
        }
        '''

        # act
        result = self.parser.convert_expectations(SchemaDefinition.create(schema, False))

        # assert
        self.assertStingEqualAsDict(result.schema_content, expected_schema)

    def assertStingEqualAsDict(self, resulting_schema: str, expected_schema: str):
        self.assertDictEqual(json.loads(resulting_schema), json.loads(expected_schema))

    def test_convert_with_regex_expectation(self):
        # arrange
        schema = '''
               {
                   "fields": [
                        {
                            "name": "random_string",
                            "type": "string",
                            "pattern": "id_"
                        }
                   ]
               }
               '''
        expected_schema = '''
               {
                  "fields": [
                      {
                          "name": "random_string",
                          "type": "string",
                          "pattern": "id_",
                          "expectations": [
                           {
                                 "kwargs": {
                                   "regex": "id_"
                                 },
                                 "expectation_type": "expect_column_values_to_match_regex"
                               }
                           ]
                      }
                  ]
               }
               '''

        # act
        result = self.parser.convert_expectations(SchemaDefinition.create(schema, False))

        # assert
        self.assertStingEqualAsDict(result.schema_content, expected_schema)


    def test_convert_with_unsupported_property(self):
        # arrange
        schema = '''
                       {
                           "fields": [
                                {
                                    "name": "random_string",
                                    "type": "string",
                                    "foobar": "id_"
                                }
                           ]
                       }
                       '''
        expected_schema = '''
                        {
                           "fields": [
                                {
                                    "name": "random_string",
                                    "type": "string",
                                    "foobar": "id_"
                                }
                           ]
                       }
                       '''

        # act
        result = self.parser.convert_expectations(SchemaDefinition.create(schema, False))

        # assert
        self.assertStingEqualAsDict(result.schema_content, expected_schema)


    def test_convert_with_nested_expectations(self):
        # arrange
        schema = DataLoader.load_schema_with_name("schema_nested_expectation.json")
        expected_schema = DataLoader.load_schema_with_name("schema_nested_expectation_result.json")

        # act
        result = self.parser.convert_expectations(SchemaDefinition.create(schema, False))

        # assert
        self.assertStingEqualAsDict(result.schema_content, expected_schema)
