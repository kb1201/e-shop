package hr.fer.dipl.exception;

import java.util.List;

public record ValidationErrorResponse(List<FieldValidationError> errors) {}

