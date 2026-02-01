package com.example.ApicurioProxy.service;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.core.util.Yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.example.ApicurioProxy.model.SpecUploadRequest;
import com.example.ApicurioProxy.model.ArtifactCreatedResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpenApiSpecService {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiSpecService.class);

    @Value("${apicurio.registry.url:http://localhost:8080}")
    private String apicurioRegistryUrl;

    @Value("${microcks.url:http://localhost:8085}")
    private String microcksUrl;

    @Value("${microcks.api.upload.path:/api/artifact/upload}")
    private String microcksUploadPath;

    @Value("${microcks.service.account.token:}")
    private String microcksToken;

    public OpenApiSpecService() {
    }

    public ArtifactCreatedResponse createArtifact(SpecUploadRequest request) throws IOException {
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
        String response = restTemplate.postForEntity(createArtifactUrl, entity, String.class).getBody();
        logger.info("Response from Apicurio Registry: {}", response);
        logger.info("Successfully created or updated artifact {}/{}", request.getGroupid(), request.getArtifactid());

        // Parse Apicurio response to extract version information
        ArtifactCreatedResponse artifactResponse = parseApicurioResponse(response, request.getGroupid(), request.getArtifactid());

        if (request.isMockedEndpoint()) {
            logger.info("Mocked endpoint creation requested");
            // Handle Microcks integration and mocked endpoint creation
            handleMicrocksIntegration(openAPI, request.getArtifactid(), request.getContentType(), artifactResponse);
            logger.info("Successfully created mocked endpoint: {}", artifactResponse.getMockedEndpoint());
        }
        
        return artifactResponse;
    }

    /**
     * Parses the Apicurio Registry response to extract artifact metadata.
     *
     * @param response The JSON response from Apicurio Registry
     * @param groupId The group ID used in the request
     * @param artifactId The artifact ID used in the request
     * @return ArtifactCreatedResponse with extracted metadata
     */
    private ArtifactCreatedResponse parseApicurioResponse(String response, String groupId, String artifactId) {
        ArtifactCreatedResponse artifactResponse = new ArtifactCreatedResponse();
        artifactResponse.setGroupId(groupId);
        artifactResponse.setArtifactId(artifactId);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

            // Extract version from the response - Apicurio v3 returns version info in "version" object
            JsonNode versionNode = rootNode.path("version");
            if (!versionNode.isMissingNode()) {
                String version = versionNode.path("version").asText();
                if (version != null && !version.isEmpty()) {
                    artifactResponse.setArtifactVersion(version);
                }
            }

            // Fallback: try to get version directly from root
            if (artifactResponse.getArtifactVersion() == null) {
                String version = rootNode.path("version").asText();
                if (version != null && !version.isEmpty() && !version.equals("null")) {
                    artifactResponse.setArtifactVersion(version);
                }
            }

            logger.debug("Parsed artifact response: groupId={}, artifactId={}, version={}",
                    artifactResponse.getGroupId(), artifactResponse.getArtifactId(), artifactResponse.getArtifactVersion());

        } catch (Exception e) {
            logger.warn("Failed to parse Apicurio response for version info: {}", e.getMessage());
            // Continue without version info
        }

        return artifactResponse;
    }

    /**
     * Handles Microcks integration and mocked endpoint creation.
     * Extracts version from OpenAPI spec, modifies the spec title, uploads to Microcks,
     * and builds the mocked endpoint URL.
     *
     * @param openAPI The parsed OpenAPI specification
     * @param artifactId The artifact ID used for the spec
     * @param contentType The content type of the original request
     * @param artifactResponse The response object to set the mocked endpoint URL
     */
    private void handleMicrocksIntegration(OpenAPI openAPI, String artifactId, String contentType, ArtifactCreatedResponse artifactResponse) {
        // Get the version from OpenAPI spec for Microcks mocked endpoint
        String specVersion = openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : "1.0.0";
        logger.info("Modify OpenapiSpec substituting the title with artifactId");
        openAPI.getInfo().setTitle(artifactId);
        logger.info("Successfully modified OpenapiSpec");
        // Convert OpenAPI object to properly formatted YAML or JSON 
        
        String content = null;
        try {
            if ("application/yaml".equals(contentType) || "application/x-yaml".equals(contentType)) {
                content  = Yaml.pretty().writeValueAsString(openAPI);
                logger.info("Successfully converted OpenAPI to YAML format");
            } else if ("application/json".equals(contentType)) {
                content = io.swagger.v3.core.util.Json.pretty(openAPI);
                logger.info("Successfully converted OpenAPI to JSON format");
            }
        } catch (Exception e) {
            logger.error("Failed to convert OpenAPI to YAML/JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize OpenAPI specification to YAML", e);
        }
        
        logger.info("Try to upload to Microcks");
        uploadToMicrocks(content, artifactId);
        logger.info("Successfully upload to Microcks");
        // Build the mocked endpoint URL: http://localhost:8585/rest/<artifactId>/<version>
        String mockedEndpoint = microcksUrl + "/rest/" + artifactId + "/" + specVersion;
        artifactResponse.setMockedEndpoint(mockedEndpoint);
    }

    /**
     * Uploads the OpenAPI spec to Microcks for mock generation.
     * Uses multipart/form-data as required by Microcks API.
     *
     * @param openApiContent The OpenAPI specification content
     * @param artifactId The artifact ID used as filename
     */
    private void uploadToMicrocks(String openApiContent, String artifactId) {
        logger.info("Uploading OpenAPI spec to Microcks...");

        try {
            RestTemplate restTemplate = new RestTemplate();

            // Set headers for multipart form data
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Add authorization header if token is provided
            if (microcksToken != null && !microcksToken.trim().isEmpty()) {
                headers.set("Authorization", "Bearer " + microcksToken);
            }

            // Create the file resource from content
            String filename = artifactId + "-openapi.yaml";
            ByteArrayResource fileResource = new ByteArrayResource(openApiContent.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            // Build multipart body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Build Microcks upload URL
            String microcksUploadUrl = microcksUrl + microcksUploadPath + "?mainArtifact=true";
            logger.debug("Uploading to Microcks via: {}", microcksUploadUrl);

            // Send POST request to Microcks
            ResponseEntity<String> microcksResponse = restTemplate.postForEntity(microcksUploadUrl, requestEntity, String.class);
            logger.info("Response from Microcks: {}", microcksResponse.getBody());
            logger.info("Successfully uploaded OpenAPI spec to Microcks");

        } catch (Exception e) {
            logger.error("Failed to upload OpenAPI spec to Microcks: {}", e.getMessage(), e);
            // Don't throw - Microcks upload failure shouldn't fail the whole operation
        }
    }
}
