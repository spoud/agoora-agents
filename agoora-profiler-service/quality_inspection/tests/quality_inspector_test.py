import unittest
from quality_inspection.quality_inspector import QualityInspector
from quality_inspection.schema_definition import SchemaDefinition
from quality_inspection.tests.data_loader import DataLoader


class QualityInspectorTest(unittest.TestCase):

    def setUp(self) -> None:
        self.inspector = QualityInspector()

    def test_inspect_inferred(self) -> None:
        # arrange
        samples = DataLoader.load_samples()

        # act
        schema_definition = SchemaDefinition.create(DataLoader.load_schema())
        result = self.inspector.inspect(samples, schema_definition)

        # assert
        self.assertEqual(1.0, result.attribute_integrity)
        self.assertEqual(.0, result.attribute_specification)
        self.assertEqual(.5, result.attribute_quality_index)

    def test_inspect_avro(self) -> None:
        # arrange
        samples = DataLoader.load_samples()

        # act
        schema_definition = SchemaDefinition.create(DataLoader.load_schema(), False)
        result = self.inspector.inspect(samples, schema_definition)

        # assert
        self.assertEqual(1.0, result.attribute_integrity)
        self.assertEqual(.625, result.attribute_specification)
        self.assertEqual(.8125, result.attribute_quality_index)

    def test_inspect_json(self) -> None:
        # arrange
        samples = DataLoader.load_samples()

        # act
        schema_definition = SchemaDefinition.create(DataLoader.load_schema_json(), False)
        result = self.inspector.inspect(samples, schema_definition)

        # assert
        self.assertEqual(1.0, result.attribute_integrity)
        self.assertEqual(.625, result.attribute_specification)
        self.assertEqual(.8125, result.attribute_quality_index)

    def test_inspect_with_specified_field(self):
        # arrange
        samples = [
            {"random_int": 1},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            ["random_int"]
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(.5, result.attribute_specification)
        self.assertEqual(1, result.attribute_integrity)
        self.assertEqual(.75, result.attribute_quality_index)

    def test_inspect_with_unspecified_field(self):
        # arrange
        samples = [
            {"random_int": 1},
        ]

        schema_definition = DataLoader.expand_schema(
            [],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(0, result.attribute_specification)
        self.assertEqual(1, result.attribute_integrity)
        self.assertEqual(.5, result.attribute_quality_index)

    def test_inspect_with_missing_field(self):
        # arrange
        samples = [
            {"random_other": "other"},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            ["random_int"]
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        expected_specification = (0 + 1) / 2
        expected_integrity = (1 + 0) / 2
        self.assertEqual(expected_specification, result.attribute_specification,
                         "Attribute specification is not correct")
        self.assertEqual(expected_integrity, result.attribute_integrity,
                         "Attribute integrity is not correct")
        self.assertEqual((expected_specification + expected_integrity) / 2, result.attribute_quality_index,
                         "Attribute quality is not correct")

    def test_specification_with_only_type_specification(self) -> None:
        # arrange
        samples = [
            {"random_int": 1, "random_string": "foo"},
            {"random_int": 2, "random_string": "bar"}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer"), ("random_string", "string")],
            ["random_string", "random_int"],

        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(.5, result.attribute_specification)

    def test_specification_with_complete_specification(self) -> None:
        # arrange
        samples = [
            {"random_int": 1, "random_string": "foo"},
            {"random_int": 2, "random_string": "bar"}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer"), ("random_string", "string")],
            ["random_string", "random_int"],

        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(.5, result.attribute_specification)

    def test_specification_with_inferred_schema(self) -> None:
        # arrange
        samples = [
            {"random_int": 1, "random_string": "foo"},
            {"random_int": 2, "random_string": "bar"}
        ]

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
                }
            },
            "required": [
                "random_integer",
                "random_string"
            ]
        }
        '''
        schema_definition = SchemaDefinition.create(schema, True)

        # act
        result = self.inspector.inspect(samples, schema_definition)

        # assert
        self.assertEqual(.0, result.attribute_specification,
                         "Attribute specification is considered 0% when schema is inferred")

    def test_specification_with_empty_schema(self) -> None:
        # arrange
        samples = [
            {"random_int": 1, "random_string": "foo"},
            {"random_int": 2, "random_string": "bar"}
        ]

        schema_definition = DataLoader.expand_schema(
            [],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(0, result.attribute_specification)

    def test_specification_with_partial_specification(self) -> None:
        # arrange
        samples = [
            {"random_int": 1, "random_string": "foo"},
            {"random_int": 2, "random_string": "bar"}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_string", "string")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert (half of the data is specified to .5)
        self.assertEqual(.25, result.attribute_specification,
                         "Specification must be 25% because only half of the data is specified in schema")

    def test_specification_with_irrelevant_specification(self) -> None:
        # arrange
        samples = [
            {"random_int": 1, "random_string": "foo"},
            {"random_int": 2, "random_string": "bar"}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_other", "string")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(0, result.attribute_specification,
                         "Specification must be 0% because none of the attributes are specified")

    def test_quality_with_complete_specification(self) -> None:
        # arrange
        samples = [
            {"random_int": 1, "random_string": "foo"},  # random_string does not match
            {"random_int": 2, "random_string": "bar"}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_string", "string"), ("random_int", "number")],
            [],
            {"random_string": {"pattern": "bar"}, "random_int": {"minimum": 0, "maximum": 100}}
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(.75, result.attribute_integrity)
        self.assertEqual(1.0, result.attribute_specification)
        self.assertEqual(.875, result.attribute_quality_index)

    def test_quality_with_partial_specification(self) -> None:
        # arrange
        samples = [
            {"random_int": 1, "random_string": "foo"},
            {"random_int": 2, "random_string": "bar"}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_string", "string"), ("random_int", "int")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(1.0, result.attribute_integrity)
        self.assertEqual(.5, result.attribute_specification)
        self.assertEqual(.75, result.attribute_quality_index)

    def test_quality_without_specification(self):
        # arrange
        samples = [
            {"random_int": 1, "random_string": "foo"},
            {"random_int": 2, "random_string": "bar"}
        ]

        schema_definition = DataLoader.expand_schema(
            [],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(.5, result.attribute_quality_index)

    def test_specification_with_partial_schema_and_inferred(self) -> None:
        # arrange
        samples = [
            {"random_int": 1, "random_string": "foo"},
            {"random_int": 2, "random_string": "bar"}
        ]

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
                       }
                   },
                   "required": [
                       "random_integer",
                       "random_string"
                   ]
               }
               '''
        schema_definition = SchemaDefinition.create(schema, True)

        # act
        result = self.inspector.inspect(samples, schema_definition)

        # assert
        self.assertEqual(.0, result.attribute_specification,
                         "Attribute specification is considered 0% when the schema is inferred")

    def test_integrity_with_missing_required(self) -> None:
        # arrange
        samples = [
            {"random_int": 1},
            {"random_int": None},
            {"random_int": 2}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            ["random_int"]
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertAlmostEqual(2 / 3, result.attribute_integrity, 3,
                               "Attribute integrity must be 66%")

    def test_integrity_for_complex_type(self):
        # arrange
        schema = DataLoader.load_schema_with_name("schema_registry_avro.json")

        samples = [
            {"timestamp": 1595601702, "iss_position": {"longitude": "-42.2948", "latitude": "-40.3670"},
             "message": "success"},
            {"timestamp": 1595601702, "iss_position": {"latitude": "-40.3670"},
             "message": "success"},
            {"timestamp": "wrong", "iss_position": {"longitude": 666, "latitude": "-40.0283"},
             "message": "success"},
        ]

        # act
        result = self.inspector.inspect_attributes(samples,
                                                   SchemaDefinition.create(schema, False))

        # assert - only message is not mandatory so 3 out of 12 (3*4) are missing or wrong
        invalid_elements = 3
        all_elements = 12
        expected_integrity = (all_elements - invalid_elements) / all_elements
        self.assertAlmostEqual(expected_integrity,
                               result.attribute_integrity, 3,
                               f"Integrity must be {expected_integrity * 100}%")

    def test_integrity_with_missing_not_required(self) -> None:
        # arrange
        samples = [
            {"random_int": 1},
            {"random_int": None},
            {"random_int": 2}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(1.0, result.attribute_integrity)

    def test_integrity_without_specified_optional_field(self) -> None:
        # arrange
        samples = [
            {"random_int": 1},
            {"random_int": 2},
            {"random_int": 3}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer"), ("random_string", "string")],
            ["random_int"]
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(1.0, result.attribute_integrity)

    def test_integrity_without_specified_required_field(self) -> None:
        # arrange
        samples = [
            {"random_int": 1},
            {"random_int": 2},
            {"random_int": 3}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer"), ("random_string", "string")],
            ["random_string"]
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(.5, result.attribute_integrity)

    def test_integrity_with_additional_field(self) -> None:
        # arrange
        samples = [
            {"random_int": 1, "random_string": "abc"},
            {"random_int": 2, "random_string": "efg"},
            {"random_int": 3, "random_string": "hij"}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(1.0, result.attribute_integrity)

    def test_integrity_with_numeric_as_string(self) -> None:
        # arrange
        samples = [
            {"random_int": "10000001.023"},
            {"random_int": "1"}
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "number")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(.0, result.attribute_integrity)

    def test_integrity_with_float_as_int(self) -> None:
        # arrange
        samples = [{"random_int": "10000001.023"}]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(0.0, result.attribute_integrity)

    def test_integrity_on_attribute_level_with_not_specified_partial_field(self) -> None:
        # arrange
        samples = [
            {"random_int": 1002, "random_string": 1},
            {"random_int": 1003, "random_string": 2},
            {"random_int": 1004},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        attribute_details = result.attribute_details
        self.assertTrue('random_string' in attribute_details.keys(),
                        "Missing integrity for attribute random_string")
        self.assertAlmostEqual(1, attribute_details['random_string'].attribute_integrity, 3,
                               "Integrity of random_string is not correct")

    def test_integrity_on_attribute_level_with_missing_value(self) -> None:
        # arrange
        samples = [
            {"random_int": 1002, "random_string": 1},
            {"random_int": 1003, "random_string": 2},
            {"random_int": "foo", "random_string": 3},
            {"random_int": 1005, "random_string": "fourth"},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer"), ("random_string", "string")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        attribute_details = result.attribute_details
        self.assertTrue('random_int' in attribute_details.keys(),
                        "Missing integrity for attribute random_int")
        self.assertTrue('random_string' in attribute_details.keys(),
                        "Missing integrity for attribute random_string")
        self.assertAlmostEqual((3 / 4), attribute_details['random_int'].attribute_integrity, 3,
                               "Integrity of random_int is not correct")
        self.assertAlmostEqual((1 / 4), attribute_details['random_string'].attribute_integrity, 3,
                               "Integrity of random_string is not correct")

    def test_integrity_on_attribute_level_with_not_specified_fields(self) -> None:
        # arrange
        samples = [
            {"random_int": 1002, "random_string": 1},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        attribute_details = result.attribute_details
        self.assertTrue('random_string' in attribute_details.keys(),
                        "Even a not specified fields needs to be present in the details.")
        self.assertEqual(1.0, attribute_details['random_string'].attribute_integrity)

    def test_specification_on_attribute_level_with_complete_expectations(self) -> None:
        # arrange
        samples = [
            {"random_int": 1002, "random_string": "1"},
            {"random_int": 1003, "random_string": "2"},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer"), ("random_string", "string")],
            [],
            {"random_int": {"minimum": 0, "maximum": 1004}, "random_string": {"pattern": ""}}
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        attribute_details = result.attribute_details
        self.assertTrue('random_int' in attribute_details.keys())
        self.assertTrue('random_string' in attribute_details.keys())
        self.assertEqual(1.0, attribute_details['random_int'].attribute_specification)
        self.assertEqual(1.0, attribute_details['random_string'].attribute_specification)

    def test_specification_on_attribute_level_with_partial_expectations(self) -> None:
        # arrange
        samples = [
            {"random_int": 1002, "random_string": 1},
            {"random_int": 1003, "random_string": 2},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer"), ("random_string", "string")],
            [],
            {"random_int": {"minimum": 0}}
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        attribute_details = result.attribute_details
        self.assertTrue('random_int' in attribute_details.keys())
        self.assertEqual(.75, attribute_details['random_int'].attribute_specification)
        self.assertEqual(.5, attribute_details['random_string'].attribute_specification)

    def test_specification_on_attribute_level_without_expectations(self) -> None:
        # arrange
        samples = [
            {"random_int": 1002, "random_string": 1},
            {"random_int": 1003, "random_string": 2},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer"), ("random_string", "string")],
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        attribute_details = result.attribute_details
        self.assertTrue('random_int' in attribute_details.keys())
        self.assertEqual(.5, attribute_details['random_int'].attribute_specification)
        self.assertEqual(.5, attribute_details['random_string'].attribute_specification)

    def test_specification_on_attribute_level_with_missing_specification(self) -> None:
        # arrange
        samples = [
            {"random_int": 1002, "random_string": 1},
            {"random_int": 1003, "random_string": 2},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        attribute_details = result.attribute_details
        self.assertTrue('random_string' in attribute_details.keys())
        self.assertEqual(0.0, attribute_details['random_string'].attribute_specification)

    def test_quality_on_attribute_level(self) -> None:
        # arrange
        samples = [
            {"random_int": 2, "random_string": "one"},
            {"random_int": 55, "random_string": "two"},
            {"random_int": 101, "random_string": "three"},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            [],
            {"random_int": {"minimum": 50, "maximum": 100}}
        )

        # act
        result = self.inspector.inspect(samples, schema_definition)

        # assert
        attribute_details = result.attribute_details
        self.assertTrue('random_int' in attribute_details.keys())
        self.assertTrue('random_string' in attribute_details.keys())
        self.assertAlmostEqual(((1 / 3) + 1) / 2, attribute_details['random_int'].attribute_quality_index, 3)
        self.assertAlmostEqual((1 + 0) / 2, attribute_details['random_string'].attribute_quality_index, 3)

    def test_inspect_with_non_unique_types_does_not_throw_exception(self) -> None:
        # arrange
        samples = [
            {"random_int": 1002},
            {"random_int": "1003"},
            {"random_int": "1004"},
        ]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            [],
            {"random_int": {"minimum": 0, "maximum": 100}}
        )

        # act
        result = self.inspector.inspect(samples, schema_definition)

        # assert
        attribute_details = result.attribute_details
        self.assertAlmostEqual((1 / 3),
                                attribute_details['random_int'].attribute_integrity, 3)

    def test_integrity_on_attribute_level_with_expectations(self):
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
                        },
                         {
                            "name": "random_string",
                            "type": "string",
                            "expectations": [
                            {       
                                  "kwargs": {
                                    "regex": "id_"
                                  },
                                  "meta": {},
                                  "expectation_type": "expect_column_values_to_match_regex"
                                }
                            ]
                        }
                    ]
                }
                '''

        samples = [
            {'random_integer': 1, 'random_string': 'missing_id'},
            {'random_integer': 11, 'random_string': 'id_1'},
            {'random_integer': 3, 'random_string': 'missing_id'},
        ]

        # act
        result = self.inspector.inspect(samples, SchemaDefinition.create(schema, False))

        # assert
        attribute_details = result.attribute_details
        self.assertAlmostEqual((3 / 6), result.attribute_integrity, 3,
                               "Attribute integrity is not correct")
        self.assertTrue('random_integer' in attribute_details.keys(),
                        "Missing integrity for attribute random_integer")
        self.assertTrue('random_string' in attribute_details.keys(),
                        "Missing integrity for attribute random_string")
        self.assertAlmostEqual((2 / 3), attribute_details['random_integer'].attribute_integrity, 3,
                               "Integrity of random_int is not correct")

        self.assertAlmostEqual((1 / 3), attribute_details['random_string'].attribute_integrity, 3,
                               "Integrity of random_string is not correct")

    def test_integrity_with_negative_as_string(self) -> None:
        # arrange
        samples = [{"random_int": "-10000"}]

        schema_definition = DataLoader.expand_schema(
            [("random_int", "integer")],
            []
        )

        # act
        result = self.inspector.inspect_attributes(samples, schema_definition)

        # assert
        self.assertEqual(.0, result.attribute_integrity,
                         "Attribute integrity must be 0% (even if not required, a "
                         "specified value needs to be correct).")

    def test_integrity_with_wrong_type(self) -> None:
        # arrange
        samples, schema = DataLoader.create_dummy_samples()
        # noinspection PyTypeChecker
        samples[0]['random_string'] = 123

        # act
        result = self.inspector.inspect_attributes(samples, schema)

        # assert
        self.assertEqual(0.5, result.attribute_integrity)

    def test_integrity_without_provided_schema(self) -> None:
        # arrange
        samples, _ = DataLoader.create_dummy_samples()

        # act
        empty_schema = SchemaDefinition.empty()
        result = self.inspector.inspect(samples, empty_schema)

        # assert
        self.assertEqual(1.0, result.attribute_integrity)
        self.assertEqual(.0, result.attribute_specification)
        self.assertEqual(.5, result.attribute_quality_index)

    def test_inspect_with_inferred_schemas(self):
        # arrange
        schema = DataLoader.load_schema_with_name("schema_registry_json.json")
        schema_definition = SchemaDefinition.create(schema, True)
        samples = DataLoader.load_samples()

        # act
        result = self.inspector.inspect(samples, schema_definition)

        # assert
        self.assertEqual(1.0, result.attribute_integrity)
        self.assertEqual(.0, result.attribute_specification)
        self.assertEqual(.5, result.attribute_quality_index)

    def test_various_types_do_not_throw_exceptions(self):
        # arrange
        schema = '''
        {
            "type": "record",
            "name": "RandomData",
            "namespace": "data.producer.random",
            "fields": [
                {
                    "name": "random_string",
                    "type": "string"
                },
                {
                    "name": "random_integer",
                    "type": "int"
                },
                {
                    "name": "random_float",
                    "type": "float"
                },
                {
                    "name": "random_boolean",
                    "type": "boolean"
                }
            ]
        }
        '''

        samples = [
            {'random_string': 'wheyuugkwi', 'random_integer': 876, 'random_float': 0.2295482, 'random_boolean': False}
        ]

        # act
        metrics = self.inspector.inspect(samples, SchemaDefinition.create(schema, False))

        # assert
        self.assertIsNotNone(metrics)

    def test_inspect_with_min_max_range_expectation(self):
        # arrange
        schema = DataLoader.load_schema_with_name("schema_with_min_max.json")

        samples = [
            {'random_integer': 3}, {'random_integer': 11}, {'random_integer': 3}, {'random_integer': 8},
            {'random_integer': 3}, {'random_integer': -5}, {'random_integer': 3}, {'random_integer': 10},
        ]

        # act
        metrics = self.inspector.inspect(samples, SchemaDefinition.create(schema, False))

        # assert
        self.assertEqual((6 / 8), metrics.attribute_integrity,
                         f"Attribute integrity must be {(6 / 8) * 100}%")

    def test_inspect_with_min_expectation(self):
        # arrange
        schema = DataLoader.load_schema_with_name("schema_with_min.json")

        samples = [
            {'random_integer': 3}, {'random_integer': 11}, {'random_integer': 3}, {'random_integer': 8},
            {'random_integer': 3}, {'random_integer': -5}, {'random_integer': 3}, {'random_integer': 10},
        ]

        # act
        metrics = self.inspector.inspect(samples, SchemaDefinition.create(schema, False))

        # assert
        self.assertEqual((7 / 8), metrics.attribute_integrity,
                         f"Attribute integrity must be {(7 / 8) * 100}%")

    def test_inspect_with_multiple_expectations_asyncapi_style(self):
        # arrange
        schema = DataLoader.load_schema_with_name("schema_expectation_asyncapi_style.json")

        samples = [
            {'random_integer': 1, 'random_string': 'id_1'},
            {'random_integer': 2, 'random_string': 'foo'},  # no match (string)
            {'random_integer': 3, 'random_string': 'id_3'},
            {'random_integer': 4, 'random_string': 'id_4'},  # no match (integer)
            {'random_integer': 5, 'random_string': 'foo'},  # no match (integer, string)
        ]

        # act
        metrics = self.inspector.inspect(samples, SchemaDefinition.create(schema, False))

        # assert
        self.assertAlmostEqual(6 / 10, metrics.attribute_integrity, 3)

    def test_inspect_with_multiple_expectations_asyncapi_style_json(self):
        # arrange
        schema = DataLoader.load_schema_with_name("schema_expectation_asyncapi_style_json.json")

        samples = [
            {'random_integer': 1, 'random_string': 'id_1'},
            {'random_integer': 2, 'random_string': 'foo'},  # no match (string)
            {'random_integer': 3, 'random_string': 'id_3'},
            {'random_integer': 4, 'random_string': 'id_4'},  # no match (integer)
            {'random_integer': 5, 'random_string': 'foo'},  # no match (integer, string)
        ]

        # act
        metrics = self.inspector.inspect(samples, SchemaDefinition.create(schema, False))

        # assert
        self.assertAlmostEqual(6 / 10, metrics.attribute_integrity, 3)


    def test_inspect_with_both_schema_formats(self):
        # arrange
        schema_json = DataLoader.load_schema_with_name("schema_diff_json.json")
        schema_avro = DataLoader.load_schema_with_name("schema_diff_avro.json")

        samples = DataLoader.load_samples()

        # act
        result_json = self.inspector.inspect(samples, SchemaDefinition.create(schema_json, False))
        result_avro = self.inspector.inspect(samples, SchemaDefinition.create(schema_avro, False))

        # assert
        self.assertEqual(result_json, result_avro)

    def test_specification_from_toeggelomat_json(self):
        # arrange
        samples = DataLoader.load_samples_from_file("samples_toeggelomat.json")

        # act
        schema = DataLoader.load_schema_with_name("schema_toeggelomat_json.json")
        result = self.inspector.inspect(samples, SchemaDefinition.create(schema, False))

        # assert
        self.assertEqual(53, len(result.attribute_details.keys()),
                         "There should be 53 keys in the dictionary")
        for attribute_metric in result.attribute_details.keys():
            self.assertEqual(1.0, result.attribute_details[attribute_metric].attribute_specification,
                             f"Attribute specification must be 100% ({attribute_metric})")
            self.assertEqual(1.0, result.attribute_details[attribute_metric].attribute_integrity,
                             f"Attribute integrity must be 100% ({attribute_metric})")

    def test_specification_from_toeggelomat(self):
        # arrange
        samples = DataLoader.load_samples_from_file("samples_toeggelomat.json")

        # act
        schema = DataLoader.load_schema_with_name("schema_toeggelomat.json")
        result = self.inspector.inspect(samples, SchemaDefinition.create(schema, False))

        # assert
        self.assertEqual(53, len(result.attribute_details.keys()),
                         "There should be 53 keys in the dictionary")
        for attribute_metric in result.attribute_details.keys():
            self.assertEqual(1.0, result.attribute_details[attribute_metric].attribute_specification,
                             f"Attribute specification must be 100% ({attribute_metric})")
            self.assertEqual(1.0, result.attribute_details[attribute_metric].attribute_integrity,
                             f"Attribute integrity must be 100% ({attribute_metric})")

