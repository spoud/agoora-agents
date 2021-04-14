from quality_inspection.abstract_schema_parser import AbstractSchemaParser
from quality_inspection.json_schema_parser import JsonSchemaParser
from quality_inspection.schema_definition import SchemaDefinition
from quality_inspection.schema_parser import AvroSchemaParser


class SchemaParserFactory(object):
    """
    Simple factory for creating the specific parser to the schema format.
    """
    @classmethod
    def create(cls, schema_definition: SchemaDefinition) -> AbstractSchemaParser:
        if schema_definition.is_avro():
            return AvroSchemaParser()
        if schema_definition.is_json():
            return JsonSchemaParser()

        assert False, "Parser not supported"
