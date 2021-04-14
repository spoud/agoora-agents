from __future__ import annotations
from abc import ABC, abstractmethod
import typing


# noinspection PyTypeChecker
class TypeSpecification(ABC):
    """
    Base class for typed and untyped specifications.
    """
    @abstractmethod
    def get_type_names(self) -> list:
        pass

    @abstractmethod
    def get_types(self) -> list:
        pass

    @abstractmethod
    def calculate(self, specifications: set) -> float:
        pass

    @classmethod
    def get_base_specification(cls) -> float:
        """
        If only a type is specified we count this at the moment as .5
        """
        return .5

    @classmethod
    def get_all_types(cls) -> set:
        return {NumberSpecification(), StringSpecification(), BooleanSpecification()}

    @classmethod
    def create(cls, attribute_type: typing.Union[str, None]) -> TypeSpecification:

        if attribute_type is None:
            return UntypedSpecification()

        for supported_type in cls.get_all_types():
            if attribute_type in supported_type.get_type_names():
                return supported_type

        assert False, f"Unsupported type found {attribute_type}"


class TypedSpecification(TypeSpecification):
    """
    Base class for typed subclasses (for the moment number and string).
    """
    @abstractmethod
    def calculate_specification(self, specifications: set) -> float:
        pass

    def calculate(self, specifications: set):
        specs = specifications if specifications is not None else {}
        return self.calculate_specification(specs)


class UntypedSpecification(TypeSpecification):

    def get_types(self) -> list:
        return []

    def get_type_names(self) -> list:
        return []

    def calculate(self, specifications: set):
        return 0  # not event a type is specified


class NumberSpecification(TypedSpecification):
    def get_type_names(self) -> list:
        return ["number", "long", "int", "float", "integer"]

    def get_types(self) -> list:
        return [int, float]

    def calculate_specification(self, specifications: set):
        specification = self.get_base_specification()
        if "minimum" in specifications:
            specification += 0.25
        if "maximum" in specifications:
            specification += 0.25
        return specification


class StringSpecification(TypedSpecification):
    def get_types(self) -> list:
        return [str]

    def get_type_names(self) -> list:
        return ["string"]

    def calculate_specification(self, specifications: set):
        if "pattern" in specifications:
            return 1.0
        return self.get_base_specification()


class BooleanSpecification(TypedSpecification):
    def get_types(self) -> list:
        return [bool]

    def get_type_names(self) -> list:
        return ["boolean"]

    def calculate_specification(self, specifications: set) -> float:
        return 1.0  # we consider a boolean as perfectly specified
