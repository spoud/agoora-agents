import unittest
from quality_inspection.type_specification import TypeSpecification, NumberSpecification, \
    BooleanSpecification, StringSpecification, UntypedSpecification


class TypeSpecificationTest(unittest.TestCase):

    def test_create_number(self):
        # arrange
        attribute_type = "number"
        # act
        specification = TypeSpecification.create(attribute_type)
        # assert
        self.assertIsInstance(specification, NumberSpecification)

    def test_create_boolean(self):
        # arrange
        attribute_type = "boolean"
        # act
        specification = TypeSpecification.create(attribute_type)
        # assert
        self.assertIsInstance(specification, BooleanSpecification)

    def test_create_untyped(self):
        # arrange
        attribute_type = None
        # act
        specification = TypeSpecification.create(attribute_type)
        # assert
        self.assertIsInstance(specification, UntypedSpecification)

    def test_calculate_untyped(self):
        # arrange
        specification = UntypedSpecification()
        # act/assert
        self.assertEqual(.0, specification.calculate(set()))

    def test_calculate_number(self):
        # arrange
        specification = NumberSpecification()
        # act/assert
        self.assertEqual(.5, specification.calculate(set()))
        self.assertEqual(.75, specification.calculate({"minimum"}))
        self.assertEqual(.75, specification.calculate({"maximum"}))
        self.assertEqual(1.0, specification.calculate({"minimum", "maximum"}))

    def test_calculate_boolean(self):
        # arrange
        specification = BooleanSpecification()
        # act/assert
        self.assertEqual(1.0, specification.calculate(set()))

    def test_create_string(self):
        # arrange
        attribute_type = "string"
        # act
        specification = TypeSpecification.create(attribute_type)
        # assert
        self.assertIsInstance(specification, StringSpecification)

    def test_calculate_string(self):
        # arrange
        specification = StringSpecification()
        # act/assert
        self.assertEqual(.5, specification.calculate(set()))
        self.assertEqual(1.0, specification.calculate({"pattern"}))
