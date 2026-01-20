## Demo Customer API:
This code show how to:
- Generate an OpenAPI Spec including extended attributes, security, custom samples, etc.
- Automatically register the generated OpenAPI specification in Apicurio Registry


## Build Configuration

Run, `mvn clean install` produces an executable JAR and then `mvn spring-boot:run`

The application will start on localhost:8081 and Swagger UI is here -> http://localhost:8081/swagger-ui/index.html
You can Try Out the 3 operations exposed on Customer entity 

## Generate the Open API spec 
With the application running on 8081, run `mvn springdoc-openapi:generate`. 
This goal will call the openapi spec endpoint at localhost:8081 and generate customerAPI-openapi.json file.  
For generating a json file format point to the json enpoint http://localhost:8081/v3/api-docs
In a real world scenario this step could be inserted in the test phase when the application is up for running the automation tests

```xml
<plugin>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-maven-plugin</artifactId>
    <version>1.5</version>
    <executions>
        <execution>
            <id>integration-test</id>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- use api-docs.yaml for generating yaml file -->
        <apiDocsUrl>http://localhost:8081/v3/api-docs</apiDocsUrl>
        <outputFileName>customerAPI-openapi.json</outputFileName>
        <outputDir>target/openapi</outputDir>
        <skip>false</skip>
        <failOnError>true</failOnError>
    </configuration>
</plugin>
```

## Open API annotations
In this project there is [SpringBoot configuration class](src/main/java/config/OpenApiConfig,java) that create a Bean that inject configurations in the OpenAPI objects used by the openapi generatot during the run
The Java class showcase how to configure extended attribute ("x-"), external documentation, security schemas, version, title, description,..
In the [rest controller](/src/main/java/controller/CustomerController.java) there are example that showcase how to properly manage the OpenAPI annotations to add meaningful information to the generated openapi spec.

Samples:
- On the main class controller
```text
  @OpenAPIDefinition(tags = {
  @Tag(name = "Customers", description = "Manage customer... "),
  })
  @RestController
  @RequestMapping("/api/customers")
```

- In the GET /customers  
```text
@Operation(
            operationId = "getCustomers",
            summary = "Get all customers or filter by name",
            description = "Get all customers or filter by name",
            tags = {"Customers"},
            parameters = { @Parameter(
                                        name = "name",
                                        description = "Name of the client",
                                        required = false
                                      )
                         },
            responses = { @ApiResponse(
                                        responseCode = "200",
                                        description = "OK",
                                        content = @Content(
                                                            mediaType = "application/json",
                                                            schema = @Schema(implementation = Customer.class)
                                                           )
                                       )
                        }
)
```
- In the [Customer repository](scr/main/java/model/Customer.java) the @Schema annotation add description to the field but also an example value
```java
    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "Name", example = "John")
    private String name;

    @Schema(description = "Email", example = "john.doe@unicredit.eu")
    private String email;
```

## Bonus: upload to Apicurio registry
You need to install and run an instance of Apicurio registry on port 8080 (or change host and port in the following xml configuration)
Run `mvn apicureio-registry:register` to register the artifact under group "customer" that has to be pre-created in Apicurio registry calling a POST on groups 
See the enclosed [postman collection](ApicurioRegistryAPI[v3].postman_collection) 

In the POM the apicurio-maven-plugin is configured in this way

```xml
<plugin>
    <groupId>io.apicurio</groupId>
    <artifactId>apicurio-registry-maven-plugin</artifactId>
    <version>3.1.2</version>
    <configuration>
        <registryUrl>http://localhost:8080/apis/registry/v3</registryUrl>
        <artifacts>
            <artifact>
                <groupId>customer</groupId>
                <artifactId>customerOpenAPI</artifactId>
                <artifactType>OPENAPI</artifactType>
                <file>${project.basedir}/target/openapi/customerAPI-openapi.json</file>
                <ifExists>FIND_OR_CREATE_VERSION</ifExists>
            </artifact>
        </artifacts>
    </configuration>
    <executions>
        <execution>
            <id>register-artifact</id>
            <goals>
                <goal>register</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```