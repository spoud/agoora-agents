import unittest
import json
from quality_inspection.json_schema_parser import JsonSchemaParser
from quality_inspection.schema_definition import SchemaDefinition
from quality_inspection.tests.data_loader import DataLoader


class JsonSchemaParserTest(unittest.TestCase):

    def setUp(self) -> None:
        self.parser = JsonSchemaParser()

    def test_load_types_from_schema(self):
        # arrange
        schema = '''
                {
                    "$schema": "http://json-schema.org/schema#",
                    "type": "object",
                    "properties": {
                        "random_string": {
                            "type": "string"
                        },
                        "random_integer": {
                            "type": "integer"
                        },
                        "random_float": {
                            "type": "number"
                        },
                        "random_boolean": {
                            "type": "boolean"
                        }
                    },
                    "required": [
                        "random_boolean",
                        "random_float",
                        "random_integer",
                        "random_string"
                    ]
                }
                '''
        schema_obj = json.loads(schema)

        # act
        type_definitions, _ = JsonSchemaParser().load_types_from_schema(schema_obj)

        # assert
        self.assertDictEqual(type_definitions, {
            "random_string": "string",
            "random_integer": "integer",
            "random_float": "number",
            "random_boolean": "boolean"})

    def test_load_types_from_schema_complex(self):
        schema = '''
        {
            "$schema": "http://json-schema.org/schema#",
            "type": "object",
            "properties": {
                "complex": {
                    "type": "object",
                    "properties": {
                        "type1number": {
                            "type": "number"
                        },
                        "type2string": {
                            "type": "string"
                        },
                        "type3complex": {
                            "type": "object",
                            "properties": {
                                "subtype1number": {
                                    "type": "number"
                                },
                                "subtype2string": {
                                    "type": "string"
                                }
                            },
                            "required": [
                                "subtype1number"
                            ]
                        }
                    },
                    "required": [
                        "type1number",
                        "type3complex"
                    ]
                },
                "base": {
                    "type": "string"
                }
            },
            "required": [
                "base",
                "complex"
            ]
        }
        '''
        schema_obj = json.loads(schema)

        # act
        types, _ = JsonSchemaParser().extract_props_from_schema(schema_obj["properties"])

        # assert
        # assert
        self.assertDictEqual({"complex/type1number": "number", "complex/type2string": "string",
                              "complex/type3complex/subtype1number": "number",
                              "complex/type3complex/subtype2string": "string",
                              "base": "string"},
                             types)

    def test_load_required_types_for_schema(self):
        # arrange
        schema = '''
                 {
                    "$schema": "http://json-schema.org/schema#",
                    "type": "object",
                    "properties": {
                        "random_string": {
                            "type": "string"
                        },
                        "random_integer": {
                            "type": "integer"
                        },
                        "random_float": {
                            "type": "number"
                        },
                        "random_boolean": {
                            "type": "boolean"
                        }
                    },
                    "required": [
                        "random_boolean",
                        "random_integer"
                    ]
                } 
                '''
        schema_obj = json.loads(schema)

        # act
        type_definitions, _ = JsonSchemaParser().load_required_types_from_schema(schema_obj)

        # assert
        self.assertListEqual(["random_boolean", "random_integer"], type_definitions)

    def test_load_required_types_for_deeply_nested_schema(self):
        # arrange
        schema = DataLoader.load_schema_with_name("schema_inferred_complex.json")
        schema_obj = json.loads(schema)

        # act
        type_definitions, _ = JsonSchemaParser().load_required_types_from_schema(schema_obj)

        # assert
        # assert
        self.assertListEqual(type_definitions, ["base",
                                                "complex/type1number",
                                                "complex/type3complex/subtype1number"])

    def test_load_expectations_asynciostyle_from_schema(self):
        # arrange
        schema = '''
                {
                    "$schema": "http://json-schema.org/schema#",
                    "type": "object",
                    "properties": {
                        "comment": {
                            "type": "string",
                            "pattern": "^b"
                        },
                        "random_integer": {
                            "type": "integer"
                        },
                        "timestamp": {
                            "type": "number",
                            "minimum": 0,
                            "maximum": 23
                        }
                    },
                    "required": [
                        "random_float",
                        "random_integer",
                        "random_string"
                    ]
                }
                '''
        schema_obj = json.loads(schema)

        # act
        type_definitions, type_expectations = JsonSchemaParser().load_required_types_from_schema(schema_obj)

        # assert
        comment_expectations = type_expectations["comment"]
        self.assertCountEqual(comment_expectations, ["pattern"])
        timestamp_expectations = type_expectations["timestamp"]
        self.assertCountEqual(["minimum", "maximum"], timestamp_expectations)


    def test_load_expectations_from_schema(self):
        # arrange
        schema = '''
                        {
                        "$schema": "http://json-schema.org/schema#",
                        "type": "object",
                        "properties": {
                            "random_integer": {
                                "type": "integer",
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
                        },
                        "required": [
                        ]
                    }
                    '''

        schema_obj = json.loads(schema)

        # act
        _, expectations = JsonSchemaParser().extract_props_from_schema(schema_obj["properties"])

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



    def test_convert_with_nested_expectations(self):
        # arrange
        schema = DataLoader.load_schema_with_name("schema_nested_expectation_json.json")
        expected_schema = DataLoader.load_schema_with_name("schema_nested_expectation_result_json.json")

        # act
        result = self.parser.convert_expectations(SchemaDefinition.create(schema, False))

        # assert
        self.assertStingEqualAsDict(result.schema_content, expected_schema)


    def test_convert(self):
        # arrange
        schema = '''
               {
                    "properties": {
                        "random_string": {
                            "type": "string",
                            "minimum": 0,
                            "maximum": 10
                        }
                    }
                }
               '''
        expected_schema = '''
               {
                    "properties": {
                        "random_string": {
                            "type": "string",
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
                  }
               }
               '''

        # act
        result = self.parser.convert_expectations(SchemaDefinition.create(schema, False))

        # assert
        self.assertStingEqualAsDict(result.schema_content, expected_schema)


    def assertStingEqualAsDict(self, resulting_schema: str, expected_schema: str):
        self.assertDictEqual(json.loads(resulting_schema), json.loads(expected_schema))
