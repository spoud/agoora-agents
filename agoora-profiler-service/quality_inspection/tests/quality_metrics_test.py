import unittest
from quality_inspection.quality_metrics import QualityMetrics


class TestQualityMetrics(unittest.TestCase):

    def test_equals_empty(self):
        metric = QualityMetrics()
        another_metric = QualityMetrics()

        self.assertEqual(metric, another_metric)

    def test_equals_true(self):
        metric = self.create_metric()
        another_metric = self.create_metric()

        self.assertEqual(metric, another_metric)

    def test_equals_false_specification(self):
        metric = self.create_metric()
        another_metric = self.create_metric()
        another_metric.attribute_specification = .5

        self.assertNotEqual(metric, another_metric)

    def test_equals_false_integrity(self):
        metric = self.create_metric()
        another_metric = self.create_metric()
        another_metric.attribute_integrity = .5

        self.assertNotEqual(metric, another_metric)

    def test_equals_false_quality(self):
        metric = self.create_metric()
        another_metric = self.create_metric()
        another_metric.attribute_quality_index = .5

        self.assertNotEqual(metric, another_metric)

    def test_equals_false_details(self):
        metric = self.create_metric()
        another_metric = self.create_metric()
        another_metric.attribute_details["random_integer"] = .5

        self.assertNotEqual(metric, another_metric)

    @classmethod
    def create_metric(cls):
        metric = QualityMetrics()
        metric.attribute_specification = .8
        metric.attribute_integrity = 1
        metric.attribute_quality_index = .9
        detail = QualityMetrics.QualityMetricsDetail(.8, .9)
        metric.attribute_details = {"random_integer": detail}
        return metric
