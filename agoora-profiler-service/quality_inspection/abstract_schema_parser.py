import typing
import json
from abc import ABC, abstractmethod

from quality_inspection.schema_definition import SchemaDefinition


class SchemaParserResult:
    """
    Result of a parsed schema with the definition of the types and expectations.
    """

    def __init__(self, type_definitions: dict, expectation_definitions: dict,
                 required_types: list, type_expectations: dict):
        self.type_definitions = type_definitions
        self.expectation_definitions = expectation_definitions
        self.required_types = required_types
        self.type_expectations = type_expectations


class AbstractSchemaParser(ABC):
    """
    Abstract base class for the concrete JSON and AVRO parsers (and the others which are yet to come)
    Note: The derivations of these class are very similar in implementations though the underlying
    schema has subtle differences (e.g. properties/fields are list/dicts) and therefore the logic was
    not generalized to keep the readability (readability matters we know).
    """

    # delimiter for the combination of nested properties
    PROPERTY_DELIMITER = "/"
    # attributes that do not count to expectations
    EXCLUDED_ATTRIBUTES = {"name", "type", "expectations"}

    def parse_schema(self, schema_definition: SchemaDefinition) -> SchemaParserResult:
        """
        Template method
        """

        # execute preprocessing step
        schema_definition = self.convert_expectations(schema_definition)

        schema_content = schema_definition.get_content()

        # load types from schema
        type_definitions, expectation_definitions = self.load_types_from_schema(schema_content)

        # load required types from schema
        required_types, type_expectations = self.load_required_types_from_schema(schema_content)

        return SchemaParserResult(type_definitions, expectation_definitions, required_types, type_expectations)


    def convert_expectations(self, schema_obj: SchemaDefinition) -> SchemaDefinition:
        """
        Method for converting the supported specifications to great expectation format
        """
        if schema_obj.is_empty():
            return schema_obj

        content = schema_obj.get_content()
        if self.get_property_keyword() not in content:
            return schema_obj

        self.convert_expectations_recursive(content)
        return SchemaDefinition.create(json.dumps(content), schema_obj.is_schema_inferred)


    @classmethod
    def create_column_values_between_expectation(cls, node: typing.Any) -> dict:
        expectation = {
            "kwargs": {
            },
            "expectation_type": "expect_column_values_to_be_between"
        }
        if "minimum" in node:
            expectation["kwargs"]["min_value"] = node["minimum"]
        if "maximum" in node:
            expectation["kwargs"]["max_value"] = node["maximum"]

        return expectation

    @classmethod
    def create_column_values_to_match_regex_expectation(cls, node: typing.Any) -> dict:
        expectation = {
            "kwargs": {
                "regex": node["pattern"]
            },
            "expectation_type": "expect_column_values_to_match_regex"
        }
        return expectation


    def convert_supported_expectations(self, field: typing.Any):
        """
        Converts json-schema-style expectations into great_expectations format
        """
        expectations = []
        # minimum, maximum
        if "minimum" in field or "maximum" in field:
            expectation = self.create_column_values_between_expectation(field)
            expectations.append(expectation)
        # pattern
        if "pattern" in field:
            expectation = self.create_column_values_to_match_regex_expectation(field)
            expectations.append(expectation)
        if len(expectations) > 0:
            field["expectations"] = expectations


    @abstractmethod
    def convert_expectations_recursive(self, schema_obj: typing.Any) -> None:
        pass

    @abstractmethod
    def load_types_from_schema(self, schema_obj: SchemaDefinition) -> (dict, dict):
        pass

    @abstractmethod
    def load_required_types_from_schema(self, schema_obj: SchemaDefinition) -> (list, dict):
        pass

    @abstractmethod
    def get_property_keyword(self):
        pass
