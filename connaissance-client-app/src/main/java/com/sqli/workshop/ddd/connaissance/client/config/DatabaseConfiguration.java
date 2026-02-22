package com.sqli.workshop.ddd.connaissance.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
@PropertySource(value = "application.yml") 
/**
 * DatabaseConfiguration - TODO: description
 *
 * @todo Add detailed Javadoc
 */
public class DatabaseConfiguration {

    @Value("${spring.mongodb.uri}")
    String uri;

	@Value("${spring.mongodb.database}")
    String database;

	@Bean
	MongoTemplate mongoTemplate(MongoClient mongoClient) {
		return new MongoTemplate(mongoClient, database);
	}

 	@Bean
	MongoClient mongoClient(MongoClientSettings settings) {
		return MongoClients.create(settings);
	}


	@Bean
	MongoClientSettings settings() {
		return MongoClientSettings.builder().applyConnectionString(new ConnectionString(uri)).build();
	}

}