package com.sqli.workshop.ddd.connaissance.client;

import com.sqli.workshop.ddd.connaissance.client.domain.ConnaissanceClientService;
import com.sqli.workshop.ddd.connaissance.client.domain.ConnaissanceClientServiceImpl;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.AdresseEventService;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.CodePostauxService;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository;
import com.sqli.workshop.ddd.connaissance.client.generated.codepostal.client.CodesPostauxApi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
@EnableWebSecurity
@EnableMongoRepositories
@OpenAPIDefinition(info = @Info(title = "Connaissance CLient", version = "v1"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        in = SecuritySchemeIn.HEADER
)
/**
 * ConnaissanceClientApplication - TODO: description
 *
 * @todo Add detailed Javadoc
 */
public class ConnaissanceClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnaissanceClientApplication.class, args);
    }

    @Bean
    CodesPostauxApi getCodesPostauxApi() {
         return new CodesPostauxApi();
    }

    /*
    @Bean
/**
 * getAdresseEventService() - TODO: description
 *
 * @todo Add detailed Javadoc
 */
    public AdresseEventService getAdresseEventService() {
        return new AdresseEventService() {
            
        };
    }

    @Bean
    ConnaissanceClientService getService(ClientRepository repositoryImpl, CodePostauxService codePostauxService, AdresseEventService adresseEventService) {
        return new ConnaissanceClientServiceImpl(repositoryImpl, codePostauxService, adresseEventService);
    }

}