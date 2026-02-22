package com.sqli.workshop.ddd.connaissance.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
/**
 * OpenApiConfig - TODO: description
 *
 * @todo Add detailed Javadoc
 */
public class OpenApiConfig {

    @Bean
/**
 * customOpenAPI() - TODO: description
 *
 * @todo Add detailed Javadoc
 */
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Connaissance Client API")
                        .version("1.0.0")
                        .description("API de gestion des clients")
                        .contact(new Contact()
                                .name("SQLI Workshop DDD")
                                .email("support@sqli.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}