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
import com.example.ApicurioProxy.model.ArtifactCreatedResponse;
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
    public ResponseEntity<ArtifactCreatedResponse> createOpenApiSpec(@RequestBody SpecUploadRequest request) {
        logger.info("Inbound request to create OpenAPI spec: groupId={}, artifactId={}, contentType={}, content={}, filename={}, mockedEndpoint={}",
                    request.getGroupid(), request.getArtifactid(), request.getContentType(), request.getContent() != null ? "[PROVIDED]" : "[NOT PROVIDED]", request.getFilename() != null ? request.getFilename() : "[NOT PROVIDED]", request.isMockedEndpoint());

        // Validate required fields
        if (request.getGroupid() == null || request.getArtifactid() == null || request.getContentType() == null) {
            logger.warn("Request validation failed: missing required fields");
            return ResponseEntity.badRequest().build();
        }
        if (request.getFilename() == null && request.getContent() == null) {
            logger.warn("Request validation failed: either filename or content must be provided");
            return ResponseEntity.badRequest().build();
        }

        try {
            ArtifactCreatedResponse response = openApiSpecService.createArtifact(request);
            return ResponseEntity.status(201).body(response);
        } catch (IOException e) {
            logger.error("Failed to create artifact: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Unexpected error during processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
