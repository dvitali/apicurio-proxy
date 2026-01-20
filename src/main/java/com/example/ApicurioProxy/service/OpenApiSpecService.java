package com.example.ApicurioProxy.service;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.ApicurioProxy.model.SpecUploadRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpenApiSpecService {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiSpecService.class);

    @Value("${apicurio.registry.url:http://localhost:8080}")
    private String apicurioRegistryUrl;

    public OpenApiSpecService() {
    }

    public void createArtifact(SpecUploadRequest request) throws IOException {
        // Read the file content
        logger.debug("Reading file: {}", request.getFilename());
        ClassPathResource resource = new ClassPathResource(request.getFilename());
        if (!resource.exists()) {
            throw new IOException("File not found: " + request.getFilename());
        }
        String fileContent = new String(resource.getInputStream().readAllBytes());

        // Parse the file as OpenAPI using Swagger Parser Library
        logger.info("Try to parse the OpenAPI spec");
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(fileContent, null, null).getOpenAPI();
        logger.info("Successfully parsed OpenAPI document with title: {}", openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : "Unknown");

        if (openAPI.getInfo() != null && openAPI.getInfo().getExtensions() != null) {
            logger.info("Extensions in info section: {}", openAPI.getInfo().getExtensions());
            // log the value of the x-test extension if present
            Object xTest = openAPI.getInfo().getExtensions().get("x-test");
            if (xTest != null) {
                logger.info("x-test extension value: {}", xTest);
            }
        }

        // Prepare request body for creating artifact
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("artifactId", request.getArtifactid());
        requestBody.put("artifactType", "OPENAPI");

        Map<String, Object> firstVersion = new HashMap<>();
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("content", fileContent);
        contentMap.put("contentType", "application/yaml");
        firstVersion.put("content", contentMap);

        // Add extensions as version labels
        if (openAPI.getInfo() != null && openAPI.getInfo().getExtensions() != null) {
            Map<String, String> labels = new HashMap<>();
            for (Map.Entry<String, Object> entry : openAPI.getInfo().getExtensions().entrySet()) {
                labels.put(entry.getKey(), entry.getValue().toString());
            }
            firstVersion.put("labels", labels);
        }

        requestBody.put("firstVersion", firstVersion);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create HTTP entity
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // URL for creating artifact
        String createArtifactUrl = apicurioRegistryUrl + "/groups/" + request.getGroupid() + "/artifacts?ifExists=FIND_OR_CREATE_VERSION";
        logger.debug("Creating artifact via: {}", createArtifactUrl);

        // Send POST request to create the artifact
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(createArtifactUrl, entity, String.class);
        logger.info("Successfully created or updated artifact {}/{}", request.getGroupid(), request.getArtifactid());
    }
}
