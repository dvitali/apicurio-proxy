## Apicurio proxy:
This code show how to expose a wrapping service over apicurio registry that create a new OpenAPI artifact base on the group, artifact and the content that is provided in a local file (it's a demo, in the new version content will be provided as field in the json).
The code extract all the extended attribute in the info object of the OAS spec and upload as version labels to enriche the catalog
Requirements:
- an apicurio registry running on localhost:8080

## Build Configuration

Run, `mvn clean install` produces an executable JAR and then `mvn spring-boot:run`

The application will start on localhost:8123 
You can Try Out running the POST http://localhost:8123/openapiSpecs
```json
{
    "groupid": "Test",
    "artifactid": "Test",
    "filename": "openapiSpecs.yaml"
}
```

