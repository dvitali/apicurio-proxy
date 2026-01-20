package com.example.ApicurioProxy.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.ApicurioProxy.model.SpecUploadRequest;
import com.example.ApicurioProxy.service.OpenApiSpecService;

import java.io.IOException;



@RestController
@RequestMapping("/openapiSpecs")
@Tag(name = "OpenAPI Specification Management", description = "API to manage and upload OpenAPI specifications")
public class OpenApiSpecController {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiSpecController.class);

    private final OpenApiSpecService openApiSpecService;

    public OpenApiSpecController(OpenApiSpecService openApiSpecService) {
        this.openApiSpecService = openApiSpecService;
    }

    @PostMapping
    @Operation(summary = "Create a new OpenAPI specification entry", operationId = "createOpenApiSpec")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Specification metadata received successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input provided"),
        @ApiResponse(responseCode = "401", description = "Group or artifact not found in registry")
    })
    public ResponseEntity<Void> createOpenApiSpec(@RequestBody SpecUploadRequest request) {
        logger.info("Inbound request to create OpenAPI spec: groupId={}, artifactId={}, filename={}",
                    request.getGroupid(), request.getArtifactid(), request.getFilename());

        // Validate required fields
        if (request.getGroupid() == null || request.getArtifactid() == null || request.getFilename() == null) {
            logger.warn("Request validation failed: missing required fields");
            return ResponseEntity.badRequest().build();
        }

        try {
            openApiSpecService.createArtifact(request);
            return ResponseEntity.status(201).build();
        } catch (IOException e) {
            logger.error("Failed to create artifact: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Unexpected error during processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
