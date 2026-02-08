package com.example.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/oneInAll")
    public ResponseEntity<ErrorResponse> eventService() {
        ErrorResponse errorResponse = new ErrorResponse("Service is Down, Please Try again");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
}
