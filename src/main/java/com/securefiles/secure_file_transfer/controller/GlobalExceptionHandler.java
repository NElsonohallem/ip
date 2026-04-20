package com.securefiles.secure_file_transfer.controller;

import com.securefiles.secure_file_transfer.dto.FileResponseDtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(
      IllegalArgumentException ex,
      HttpServletRequest request
  ) {
    return ResponseEntity.badRequest().body(
        new ErrorResponse(
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
        )
    );
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(
      IllegalStateException ex,
      HttpServletRequest request
  ) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        new ErrorResponse(
            "Internal Server Error",
            ex.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
        )
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(
      Exception ex,
      HttpServletRequest request
  ) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        new ErrorResponse(
            "Internal Server Error",
            "Something went wrong",
            request.getRequestURI(),
            LocalDateTime.now()
        )
    );
  }
}