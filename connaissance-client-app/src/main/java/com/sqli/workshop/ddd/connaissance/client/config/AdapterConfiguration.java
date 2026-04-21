package com.sqli.workshop.ddd.connaissance.client.config;

import com.sqli.workshop.ddd.connaissance.client.db.ClientDbMapper;
import com.sqli.workshop.ddd.connaissance.client.db.ClientDbRepository;
import com.sqli.workshop.ddd.connaissance.client.db.ClientRepositoryImpl;
import com.sqli.workshop.ddd.connaissance.client.db.adapter.InMemoryClientRepositoryAdapter;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.AdresseEventService;
import com.sqli.workshop.ddd.connaissance.client.event.AdresseEventServiceImpl;
import com.sqli.workshop.ddd.connaissance.client.event.adapter.InMemoryAdresseEventServiceImpl;
import com.sqli.workshop.ddd.connaissance.client.generated.event.producer.IDefaultServiceEventsProducer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.NonNull;

/**
 * AdapterConfiguration - Spring bean configuration for production vs workshop profiles
 * 
 * Conditionally wires ClientRepository and AdresseEventService adapters
 * based on the "workshop.enabled" property.
 * 
 * Profile: workshop → In-memory adapters (no Docker)
 * Profile: prod   → MongoDB + Kafka adapters (production)
 * 
 * @author Workshop Mode Feature
 */
@Configuration
public class AdapterConfiguration {

    // ============================================
    // ClientRepository Adapter
    // ============================================

    /**
     * In-Memory Client Repository (Workshop Profile)
     * Used when workshop.enabled=true
     * 
     * @param mapper ClientDbMapper for DTO conversions
     * @return InMemoryClientRepositoryAdapter instance
     */
    @Bean
    @ConditionalOnProperty(name = "workshop.enabled", havingValue = "true")
    public ClientRepository inMemoryClientRepository(
            @NonNull ClientDbMapper mapper) {
        return new InMemoryClientRepositoryAdapter(mapper);
    }

    /**
     * MongoDB Client Repository (Production Profile)
     * Used when workshop.enabled=false or not set (default)
     * 
     * @param dbRepository Spring Data MongoDB repository
     * @param mapper ClientDbMapper for DTO conversions
     * @return ClientRepositoryImpl instance configured for MongoDB
     */
    @Bean
    @ConditionalOnProperty(
        name = "workshop.enabled",
        havingValue = "false",
        matchIfMissing = true  // Default to production if property not set
    )
    public ClientRepository mongoClientRepository(
            @NonNull ClientDbRepository dbRepository,
            @NonNull ClientDbMapper mapper) {
        return new ClientRepositoryImpl(dbRepository, mapper);
    }

    // ============================================
    // AdresseEventService Adapter
    // ============================================

    /**
     * In-Memory Adresse Event Service (Workshop Profile)
     * Captures events in-memory; not published to Kafka
     * Used when workshop.enabled=true
     * 
     * @return InMemoryAdresseEventServiceImpl instance
     */
    @Bean
    @ConditionalOnProperty(name = "workshop.enabled", havingValue = "true")
    public AdresseEventService inMemoryAdresseEventService() {
        return new InMemoryAdresseEventServiceImpl();
    }

    /**
     * Kafka Adresse Event Service (Production Profile)
     * Publishes events to Kafka broker
     * Used when workshop.enabled=false or not set (default)
     * 
     * @param producer IDefaultServiceEventsProducer for Kafka publishing
     * @return AdresseEventServiceImpl instance configured for Kafka
     */
    @Bean
    @ConditionalOnProperty(
        name = "workshop.enabled",
        havingValue = "false",
        matchIfMissing = true
    )
    public AdresseEventService kafkaAdresseEventService(
            @NonNull IDefaultServiceEventsProducer producer) {
        return new AdresseEventServiceImpl(producer);
    }
}
