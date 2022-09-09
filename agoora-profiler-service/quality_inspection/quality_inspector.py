from __future__ import annotations
import numpy as np
from great_expectations.core import ExpectationSuite, ExpectationConfiguration
from great_expectations.dataset import PandasDataset
from pandas import json_normalize
from collections import defaultdict

from quality_inspection.abstract_schema_parser import SchemaParserResult
from quality_inspection.schema_parser_factory import SchemaParserFactory
from quality_inspection.quality_metrics import QualityMetrics
from quality_inspection.schema_definition import SchemaDefinition

# mappings from schema types to python types
from quality_inspection.type_specification import TypeSpecification


class QualityInspector:
    """
    Inspects the samples based on the passed schema and evaluates:
        * Attribute integrity
    In later steps this should be extended with:
        * Row integrity
        * Set integrity
        * Domain integrity
        * or stuff like AI integrity (e.g. auto-encoders)
    to form a complete Data Quality Index

    Note:
    * At the moment only positive and negative numbers are handled as numbers and
    special chars are not allowed (10'000 or 34$ will not be parsed as number)
    * For the moment we're handling an inferred schema as no schema at all
    """

    def inspect(self, samples: list, schema_definition: SchemaDefinition) -> QualityMetrics:
        """
        Inspects the samples based on the schema definition and
        returns a quality metric.
        """
        assert schema_definition is not None, "Schema must not be None"
        if not schema_definition.schema_content:
            return QualityMetrics.without_schema()
        if schema_definition.is_schema_inferred:
            return QualityMetrics.for_inferred_schema()

        return self.inspect_attributes(samples, schema_definition)

    def inspect_attributes(self, samples: list, schema_definition: SchemaDefinition) -> QualityMetrics:
        """
        Inspects the integrity and specification of the attributes.
        """

        integrity_details, specification_details = \
            self.compare_attributes_with_schema(samples, schema_definition)

        # create metrics return type
        metric = QualityMetrics.create(integrity_details, specification_details)

        return metric

    @classmethod
    def parse_schema(cls, schema_definition: SchemaDefinition) -> SchemaParserResult:
        """
        Parses the schema with the types, the required types and the expectations.
        """

        # instantiate concrete parser
        parser = SchemaParserFactory.create(schema_definition)

        # parse schema with concrete parser
        specification = parser.parse_schema(schema_definition)
        return specification

    def compare_attributes_with_schema(self, samples: list,
                                       schema_definition: SchemaDefinition) -> (dict, dict):
        """
        Calculates attribute integrity and specification of the samples.
        """
        specs = self.parse_schema(schema_definition)

        df_normalized = json_normalize(samples, sep="/")
        df_ge = PandasDataset(df_normalized)

        # calculate integrity
        integrity_details = self.calculate_integrity(df_ge, specs)

        # calculate specification
        specification_details = self.calculate_specification(df_ge, specs)

        return integrity_details, specification_details

    @classmethod
    def calculate_specification(cls, df_ge: PandasDataset, specs: SchemaParserResult) -> dict:
        specification_details = {}

        # handle all found attributes
        for attribute in df_ge.columns:
            specification = TypeSpecification.create(specs.type_definitions.get(attribute))
            specification_details[attribute] = specification.calculate(specs.type_expectations.get(attribute))

        # handle specified but not found values
        for attribute in specs.type_definitions.keys():
            if attribute not in specification_details.keys():
                specification_details[attribute] = 1

        return specification_details

    def calculate_integrity(self, df_ge: PandasDataset, specs: SchemaParserResult) -> dict:
        """
        Calculates the integrity from the defined types and the expectations.
        """

        def get_unexpected(eg_result):
            return eg_result['unexpected_count'] if 'unexpected_count' in eg_result else 0

        def merge_dicts(d1, d2):
            for key, value in d2.items():
                for inner_value in d2[key]:
                    d1[key].append(inner_value)
            return d1

        all_elements = defaultdict(list)
        invalid_elements = defaultdict(list)
        for definition in specs.type_definitions:
            result = df_ge.expect_column_to_exist(definition)
            if not result.success:
                if definition in specs.required_types:  # does only count as error if required
                    invalid_elements[definition].append(df_ge.shape[0])
                    all_elements[definition].append(df_ge.shape[0])
                continue

            # check missing values
            result = df_ge.expect_column_values_to_not_be_null(definition)
            if definition in specs.required_types:  # only count as error if required
                invalid_elements[definition].append(get_unexpected(result.result))
            all_elements[definition].append(result.result['element_count'])

            # check not correct types
            type_specification = TypeSpecification.create(specs.type_definitions.get(definition))
            type_list = [t.__name__ for t in type_specification.get_types()]
            # noinspection PyTypeChecker
            result = df_ge.expect_column_values_to_be_in_type_list(definition, type_list)
            invalid_elements[definition].append(get_unexpected(result.result))

        # handle attributes that are not specified
        not_specified_fields = set(df_ge.columns) - set(specs.type_definitions)
        if len(not_specified_fields) > 0:
            for attribute in not_specified_fields:
                result = df_ge.expect_column_values_to_be_null(attribute)
                # integrity of not specified fields has been defined as 1 - so we add 0 to unexpected
                invalid_elements[attribute].append(0)
                all_elements[attribute].append(get_unexpected(result.result))


        # check expectations
        expectation_violations = self.validate_expectations(df_ge, specs)
        merge_dicts(invalid_elements, expectation_violations)

        # flatten attribute metrics
        integrity_details = dict()
        for k, v in invalid_elements.items():
            integrity_details[k] = 1 - (np.sum(v) / sum(all_elements[k]))

        return integrity_details

    @classmethod
    def validate_expectations(cls, df_ge: PandasDataset, specs: SchemaParserResult) -> defaultdict[list]:
        """
        Validates the dynamic expectations from the schema via the
        great expectations library.
        """
        invalid_elements = defaultdict(list)
        suite = ExpectationSuite(expectation_suite_name="custom_specifications")
        for column in specs.expectation_definitions.keys():
            for expectation in specs.expectation_definitions[column]:
                kwargs_extended = dict(expectation['kwargs'])
                kwargs_extended['column'] = column
                suite.append_expectation(ExpectationConfiguration(
                    expectation_type=expectation['expectation_type'],
                    kwargs=kwargs_extended))
        # noinspection PyTypeChecker
        result = df_ge.validate(expectation_suite=suite, result_format="BASIC")
        for expectation_result in result.results:
            if expectation_result.exception_info['raised_exception']:
                continue
            column_name = expectation_result.expectation_config.kwargs["column"]
            n_invalid = expectation_result.result['unexpected_count']
            invalid_elements[column_name].append(n_invalid)

        return invalid_elements
