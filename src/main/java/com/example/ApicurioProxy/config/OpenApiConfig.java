package com.example.ApicurioProxy.config;

import io.swagger.v3.oas.annotations.servers.Servers;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Value("${springdoc.info.title}")
    private String title;

    @Value("${springdoc.info.description}")
    private String description;

    @Value("${springdoc.info.version}")
    private String version;

    @Value("${springdoc.info.contact.name}")
    private String contactName;

    @Value("${springdoc.info.contact.email}")
    private String contactEmail;

    @Value("${springdoc.info.extensions.x-app-id}")
    private String applicationId;

    @Value("${springdoc.info.externaldocs.description}")
    private String externalDocDesc;

    @Value("${springdoc.info.externaldocs.url}")
    private String externalDocUrl;

    @Value("${server.port}")
    private String serverPort;

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        // Extension
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-app-id", applicationId);

        Info info = new Info()
                .title(title)
                .description(description)
                .version(version)
                .extensions(extensions)
                .contact(new Contact()
                        .name(contactName)
                        .email(contactEmail)
                );

        ExternalDocumentation documentation = new ExternalDocumentation()
                .description(externalDocDesc)
                .url(externalDocUrl);

        // Server url, could be in the app properties
        Server server = new Server().url("http://localhost:"+ serverPort);

        // Costruisci OpenAPI
        return new OpenAPI()
                .info(info)
                .externalDocs(documentation)
                .servers(Collections.singletonList(server))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
