package com.bluestaq.notesapi.controller;

import com.bluestaq.notesapi.dto.ErrorResponse;
import com.bluestaq.notesapi.service.NoUpdateFieldsException;
import com.bluestaq.notesapi.service.NoteAccessDeniedException;
import com.bluestaq.notesapi.service.NoteNotFoundException;
import com.bluestaq.notesapi.service.SelfShareException;
import com.bluestaq.notesapi.service.UserNotFoundException;
import com.bluestaq.notesapi.service.UsernameAlreadyTakenException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        return new ErrorResponse(400, "Bad Request", message);
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUsernameConflict(UsernameAlreadyTakenException ex) {
        return new ErrorResponse(409, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentials(BadCredentialsException ex) {
        return new ErrorResponse(401, "Unauthorized", "Invalid username or password");
    }

    @ExceptionHandler(NoteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoteNotFound(NoteNotFoundException ex) {
        return new ErrorResponse(404, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(NoteAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleNoteAccessDenied(NoteAccessDeniedException ex) {
        return new ErrorResponse(403, "Forbidden", ex.getMessage());
    }

    @ExceptionHandler(NoUpdateFieldsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNoUpdateFields(NoUpdateFieldsException ex) {
        return new ErrorResponse(400, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFound(UserNotFoundException ex) {
        return new ErrorResponse(404, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(SelfShareException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleSelfShare(SelfShareException ex) {
        return new ErrorResponse(422, "Unprocessable Entity", ex.getMessage());
    }
}
