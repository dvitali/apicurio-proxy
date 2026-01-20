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
        logger.info("Service received: filename={}, content={}", request.getFilename(), request.getContent() != null ? "provided" : "null");
        String fileContent;
        if (request.getFilename() != null) {
            // Read from file
            logger.debug("Reading file: {}", request.getFilename());
            ClassPathResource resource = new ClassPathResource(request.getFilename());
            if (!resource.exists()) {
                throw new IOException("File not found: " + request.getFilename());
            }
            fileContent = new String(resource.getInputStream().readAllBytes());
        } else {
            // Use provided content
            fileContent = request.getContent();
            logger.debug("Using provided content, length: {}", fileContent != null ? fileContent.length() : "null");
            if (fileContent == null || fileContent.trim().isEmpty()) {
                throw new IllegalArgumentException("Content is required and cannot be empty when filename is not provided");
            }
        }

        // Parse the file as OpenAPI using Swagger Parser Library
        logger.info("Try to parse the OpenAPI spec");
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(fileContent, null, null).getOpenAPI();
        logger.info("Successfully parsed OpenAPI document with title: {}", openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : "Unknown");

        if (openAPI.getInfo() != null && openAPI.getInfo().getExtensions() != null) {
            logger.info("Extensions in info section: {}", openAPI.getInfo().getExtensions());
        }

        // Prepare request body for creating artifact
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("artifactId", request.getArtifactid());
        requestBody.put("artifactType", "OPENAPI");

        Map<String, Object> firstVersion = new HashMap<>();
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("content", fileContent);
        contentMap.put("contentType", request.getContentType());
        firstVersion.put("content", contentMap);

        // Add extensions as version labels
        if (openAPI.getInfo() != null && openAPI.getInfo().getExtensions() != null) {
            Map<String, String> labels = new HashMap<>();
            for (Map.Entry<String, Object> entry : openAPI.getInfo().getExtensions().entrySet()) {
                labels.put(entry.getKey(), entry.getValue().toString());
            }
            firstVersion.put("labels", labels);
        }
        // this attribute is part of the create artifact and it's mandatory, the api will check if the content is changed respect the last version and in case create a new version
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
