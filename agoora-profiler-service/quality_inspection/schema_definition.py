from __future__ import annotations
import json
from typing import Any


class SchemaDefinition:
    """
    Wraps the content of a schema together with its origin (inferred or non inferred)
    """

    def __init__(self):
        self.schema_content = None
        self.is_schema_inferred = False

    @classmethod
    def create(cls, content: str, is_schema_inferred=True) -> SchemaDefinition:
        definition = SchemaDefinition()
        definition.is_schema_inferred = is_schema_inferred
        definition.schema_content = content
        return definition

    def is_empty(self) -> bool:
        return self.schema_content == ""

    @classmethod
    def empty(cls):
        definition = SchemaDefinition()
        definition.schema_content = ""
        return definition

    def is_json(self):
        if self.is_schema_inferred:
            return False
        return "properties" in self.schema_content

    def is_avro(self):
        if self.is_schema_inferred:
            return False
        return "fields" in self.schema_content

    def __eq__(self, other):
        """Overrides the default implementation"""
        if isinstance(other, SchemaDefinition):
            return self.is_schema_inferred == other.is_schema_inferred and self.schema_content == other.schema_content
        return False

    def get_content(self) -> Any:
        assert self.schema_content != "", "Schema content must not be empty"
        return json.loads(self.schema_content)
