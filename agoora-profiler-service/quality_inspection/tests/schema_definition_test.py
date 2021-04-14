from unittest import TestCase
from quality_inspection.schema_definition import SchemaDefinition


class TestSchemaDefinition(TestCase):

    def test_equality_true(self):
        # arrange
        schema = SchemaDefinition.empty()
        another_schema = SchemaDefinition.empty()

        # act / assert
        self.assertEqual(schema, another_schema)

    def test_equality_inferred(self):
        # arrange
        schema = SchemaDefinition.empty()
        another_schema = SchemaDefinition.empty()
        another_schema.is_schema_inferred = True

        # act / assert
        self.assertNotEqual(schema, another_schema)

    def test_equality_content(self):
        # arrange
        schema = SchemaDefinition.empty()
        another_schema = SchemaDefinition.empty()
        another_schema.schema_content = "another schema content"

        # act / assert
        self.assertNotEqual(schema, another_schema)
