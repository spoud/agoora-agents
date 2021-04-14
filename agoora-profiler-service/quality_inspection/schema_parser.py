import typing
from quality_inspection.abstract_schema_parser import AbstractSchemaParser


class AvroSchemaParser(AbstractSchemaParser):
    """
    Class for parsing the AVRO schema from the registry.
    """

    def get_property_keyword(self):
        return "fields"

    def load_types_from_schema(self, schema_obj: typing.Any) -> (dict, dict):
        """ Wraps recursive method """
        return self.extract_props_from_schema(schema_obj["fields"])

    def extract_props_from_schema(self, schema_obj: dict, property_path: str = "",
                                  property_dict: dict = None, expectation_dict: dict = None) -> (dict, dict):
        """
        Recursive method which extracts the types from a recursive (how surprising!) json structure from
        the registry.
        """
        if property_dict is None:
            property_dict = dict()
            expectation_dict = dict()
        for field in schema_obj:
            final_key = f"{property_path}{AbstractSchemaParser.PROPERTY_DELIMITER}{field['name']}" \
                if property_path is not "" else field['name']
            if type(field['type']) is dict and "fields" in field['type'].keys():
                self.extract_props_from_schema(field['type']["fields"], final_key,
                                               property_dict, expectation_dict)
            else:
                if type(field['type']) is list:
                    attribute_type = field['type'][0]
                else:
                    attribute_type = field['type']
                property_dict[final_key] = attribute_type
                expectation_dict[final_key] = field['expectations'] if 'expectations' in field else []

        return property_dict, expectation_dict

    def load_required_types_from_schema(self, schema_obj: dict) -> (list, dict):
        """ Wraps recursive method """
        return self.extract_required_props(schema_obj)

    def extract_required_props(self, schema_obj, property_path: str = "",
                               required_types: list = None,
                               type_expectations: dict = None) -> (list, dict):
        """
        Recursive method for extracting the required fields from a schema from registry
        """
        if required_types is None:
            required_types = list()
        if type_expectations is None:
            type_expectations = dict()

        if "fields" in schema_obj.keys():
            for field in schema_obj["fields"]:
                final_key = f"{property_path}{AbstractSchemaParser.PROPERTY_DELIMITER}{field['name']}" \
                    if property_path is not "" else field['name']
                if "type" in field.keys() and not type(field["type"]) is list:
                    if type(field["type"]) is dict:
                        self.extract_required_props(field["type"], final_key,
                                                    required_types, type_expectations)
                    else:
                        required_types.append(final_key)
                # collect expectations
                for key in set(field.keys()) - AbstractSchemaParser.EXCLUDED_ATTRIBUTES:
                    if final_key not in type_expectations.keys():
                        type_expectations[final_key] = []
                    type_expectations[final_key].append(key)

        return required_types, type_expectations

    def convert_expectations_recursive(self, content: typing.Any) -> None:
        """
        Convert all async-style expectations into the great_expectations format
        """
        for field in content["fields"]:
            self.convert_supported_expectations(field)

            if "type" in field and type(field["type"]) is dict:
                self.convert_expectations_recursive(field["type"])
