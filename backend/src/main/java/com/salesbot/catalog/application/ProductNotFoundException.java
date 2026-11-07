package com.salesbot.catalog.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Rendered as a 404 ProblemDetail by Spring MVC. */
public class ProductNotFoundException extends ResponseStatusException {

    public ProductNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Product " + id + " not found");
    }
}
