from __future__ import annotations

import numpy as np


class QualityMetrics:
    """
    Represents the combination of the different aspects
    of quality metrics (for now just attribute quality)
    """

    class QualityMetricsDetail:
        def __init__(self, specification: float, integrity: float):
            self.attribute_specification = specification
            self.attribute_integrity = integrity
            self.attribute_quality_index = QualityMetrics.quality_measure([self.attribute_specification,
                                                                           self.attribute_integrity])

        def __eq__(self, other):
            if other is None or type(other) != type(self):
                return False
            return self.attribute_specification == other.attribute_specification \
                and self.attribute_integrity == other.attribute_integrity \
                and self.attribute_quality_index == other.attribute_quality_index

        def __repr__(self):
            return f"QualityMetricDetail (Quality Index: {self.attribute_quality_index}, Quality Specification: " \
                   f"{self.attribute_specification}, Quality Integrity: {self.attribute_integrity})"

    @classmethod
    def create(cls, integrity_details, specification_details):
        assert len(set(integrity_details.keys()) - set(specification_details.keys())) == 0, \
            "Set of integrities and specifications are not equal"

        metrics = QualityMetrics()
        for attribute, integrity in integrity_details.items():
            metrics.attribute_details[attribute] = QualityMetrics.QualityMetricsDetail(
                specification_details[attribute], integrity)

        metrics.calculate_quality()
        return metrics

    def __init__(self):
        self.attribute_integrity = 0.0
        self.attribute_quality_index = 0.0
        self.attribute_specification = 0.0
        self.attribute_details = dict()

    @classmethod
    def without_schema(cls) -> QualityMetrics:
        metrics = QualityMetrics()
        metrics.attribute_integrity = 1.0
        metrics.attribute_specification = 0.0
        metrics.attribute_quality_index = cls.quality_measure([metrics.attribute_integrity,
                                                               metrics.attribute_specification])
        cls.attribute_details = dict()
        return metrics

    @classmethod
    def quality_measure(cls, measures: list) -> float:
        # noinspection PyTypeChecker
        return np.mean(measures)

    def calculate_quality(self):
        self.attribute_integrity = np.mean([
            i.attribute_integrity for i in self.attribute_details.values()])
        self.attribute_specification = np.mean([
            i.attribute_specification for i in self.attribute_details.values()])
        self.attribute_quality_index = np.mean([
            i.attribute_quality_index for i in self.attribute_details.values()])

    @classmethod
    def for_inferred_schema(cls):
        """
        For now we decided to handle an inferred schema
        equally to no schema at all
        """
        return cls.without_schema()

    def __eq__(self, other):
        if other is None:
            return False
        if type(other) != type(self):
            return False
        if self.attribute_quality_index != other.attribute_quality_index \
            or self.attribute_integrity != other.attribute_integrity \
            or self.attribute_specification != other.attribute_specification:
            return False

        return self.attribute_details == other.attribute_details

    def __repr__(self):
        message = f"QualityMetricDetail (Quality Index: {self.attribute_quality_index}, Quality Specification: " \
                   f"{self.attribute_specification}, Quality Integrity: {self.attribute_integrity}) \nDetails:\n"
        for item in self.attribute_details.items():
            message += f"{item}\n"
        return message
