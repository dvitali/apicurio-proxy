## Apicurio proxy
This code show how to expose a wrapping service over apicurio registry that create a new OpenAPI artifact base on the group, artifact and the content that can provided in a local file  or as field content in the body of the request 
The code extract all the extended attribute in the info object of the OAS spec and upload as version labels to enrich the catalog
For running the apps a apicurio registry running on localhost:8080 is neede. You need to create the group before running creating the artifact.

## Build Configuration

Run, `mvn clean install` produces an executable JAR and then `mvn spring-boot:run`

The application will start on localhost:8123 
You can try uut running the POST http://localhost:8123/openapiSpecs
```json
{
    "groupid": "Test",
    "artifactid": "Test",
    "filename": "openapiSpecs.yaml"
}
```

Or passing a valid openapi specification in json or yaml format
```json
{
    "groupid": "Test",
    "artifactid": "Test",
    "content": "{\"openapi\":\"3.0.0\",\"info\":{\"title\":\"OpenAPI Specification Management API\",\"version\":\"1.0.1\",\"description\":\"API to manage and upload OpenAPI specifications.\",\"x-test\":\"disabled\"},\"paths\":{\"/openapiSpecs\":{\"post\":{\"summary\":\"Create a new OpenAPI specification entry\",\"operationId\":\"createOpenApiSpec\",\"requestBody\":{\"description\":\"Metadata for the OpenAPI specification file\",\"required\":true,\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/SpecUploadRequest\"}}}},\"responses\":{\"201\":{\"description\":\"Specification metadata received successfully\"},\"400\":{\"description\":\"Invalid input provided\"}}}}},\"components\":{\"schemas\":{\"SpecUploadRequest\":{\"type\":\"object\",\"required\":[\"groupid\",\"artifactid\",\"filename\"],\"properties\":{\"groupid\":{\"type\":\"string\",\"example\":\"com.enterprise.api\"},\"artifactid\":{\"type\":\"string\",\"example\":\"user-service\"},\"filename\":{\"type\":\"string\"}}}}}}",
     "contentType": "application/json"
}
```