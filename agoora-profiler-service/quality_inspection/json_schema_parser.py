import typing

from quality_inspection.abstract_schema_parser import AbstractSchemaParser


class JsonSchemaParser(AbstractSchemaParser):
    """
    Class for parsing the JSON schemas (from registry and potentially also inferred ones).
    """

    def get_property_keyword(self):
        return "properties"

    def load_types_from_schema(self, schema_obj: typing.Any) -> (dict, dict):
        """ Wraps recursive method """
        return self.extract_props_from_schema(schema_obj["properties"])

    def extract_props_from_schema(self, schema_obj: dict, property_path: str = "",
                                  property_dict: dict = None, expectation_dict: dict = None) -> (dict, dict):
        """
        Recursive method which extracts the types from a recursive (how surprising!) json structure from
        the registry.
        """
        if property_dict is None:
            property_dict = dict()
            expectation_dict = dict()

        for key, value in schema_obj.items():
            final_key = f"{property_path}{AbstractSchemaParser.PROPERTY_DELIMITER}{key}" if property_path is not "" \
                else key
            if "properties" in schema_obj[key].keys():
                self.extract_props_from_schema(schema_obj[key]["properties"], final_key,
                                               property_dict, expectation_dict)
            else:
                property_dict[final_key] = value['type']
                expectation_dict[final_key] = value['expectations'] if 'expectations' in value else []
        return property_dict, expectation_dict

    def load_required_types_from_schema(self, schema_obj: dict) -> (list, dict):
        """ Wraps recursive method """
        return self.extract_required_props(schema_obj)

    def extract_required_props(self, schema_obj, property_path: str = "",
                               required_types: list = None,
                               type_expectations: dict = None) -> (list, dict):
        """
        Recursive method for extracting the required fields from an inferred schema
        """
        if required_types is None:
            required_types = list()
        if type_expectations is None:
            type_expectations = dict()

        if "required" in schema_obj.keys():
            for key in schema_obj["required"]:
                if property_path in required_types:
                    required_types.remove(property_path)
                final_key = f"{property_path}{AbstractSchemaParser.PROPERTY_DELIMITER}{key}" \
                    if property_path is not "" else key
                required_types.append(final_key)

        if "properties" in schema_obj.keys():
            for field in schema_obj['properties'].keys():
                final_key = f"{property_path}{AbstractSchemaParser.PROPERTY_DELIMITER}{field}" \
                    if property_path is not "" else field
                if "properties" in schema_obj['properties'][field].keys():
                    self.extract_required_props(schema_obj['properties'][field],
                                                final_key, required_types, type_expectations)
                else:
                    # collect expectations
                    for key in set(schema_obj['properties'][field].keys()) - AbstractSchemaParser.EXCLUDED_ATTRIBUTES:
                        if final_key not in type_expectations.keys():
                            type_expectations[final_key] = []
                        type_expectations[final_key].append(key)

        return required_types, type_expectations

    def convert_expectations_recursive(self, content: typing.Any) -> None:
        """
        Method for converting the supported specifications to great expectation format
        """
        properties = content["properties"]
        for attribute in properties:
            self.convert_supported_expectations(properties[attribute])

            if "properties" in properties[attribute].keys():
                self.convert_expectations_recursive(properties[attribute])
