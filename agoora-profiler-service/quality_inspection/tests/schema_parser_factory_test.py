import unittest
from quality_inspection.json_schema_parser import JsonSchemaParser
from quality_inspection.schema_definition import SchemaDefinition
from quality_inspection.schema_parser import AvroSchemaParser
from quality_inspection.schema_parser_factory import SchemaParserFactory
from quality_inspection.tests.data_loader import DataLoader


class SchemaParserFactoryTest(unittest.TestCase):

    def test_create_avro_parser(self):
        # arrange
        definition = SchemaDefinition.create(DataLoader.load_schema(), False)

        # act
        parser = SchemaParserFactory.create(definition)

        # assert
        self.assertIsInstance(parser, AvroSchemaParser)


    def test_create_json_parser(self):
        # arrange
        definition = SchemaDefinition.create(DataLoader.load_schema_json(), False)

        # act
        parser = SchemaParserFactory.create(definition)

        # assert
        self.assertIsInstance(parser, JsonSchemaParser)
