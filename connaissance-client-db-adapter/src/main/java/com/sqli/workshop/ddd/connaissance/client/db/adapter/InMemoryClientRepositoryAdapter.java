package com.sqli.workshop.ddd.connaissance.client.db.adapter;

import com.sqli.workshop.ddd.connaissance.client.db.ClientDb;
import com.sqli.workshop.ddd.connaissance.client.db.ClientDbMapper;
import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * InMemoryClientRepositoryAdapter - Workshop profile implementation of ClientRepository
 * 
 * Stores clients in-memory using ConcurrentHashMap for thread-safe concurrent access.
 * Transient storage (cleared on app restart); suitable for development and workshops.
 * 
 * No external persistence required (no MongoDB dependency in workshop mode).
 * 
 * Thread-safety: All operations are atomic at the segment level (ConcurrentHashMap guarantees).
 * 
 * @author Workshop Mode Feature
 */
@AllArgsConstructor
public class InMemoryClientRepositoryAdapter implements ClientRepository {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryClientRepositoryAdapter.class);

    private final @NonNull ClientDbMapper mapper;
    private final Map<String, ClientDb> storage = new ConcurrentHashMap<>();

    /**
     * List all clients stored in memory, sorted by nom then prenom
     * 
     * @return List of all clients (sorted alphabetically)
     */
    @Override
    public List<Client> lister() {
        logger.debug("Listing all {} clients from in-memory storage", storage.size());
        
        return storage.values().stream()
                .sorted((c1, c2) -> {
                    int nomComparison = c1.getNom().compareTo(c2.getNom());
                    if (nomComparison != 0) return nomComparison;
                    return c1.getPrenom().compareTo(c2.getPrenom());
                })
                .map(mapper::mapToDomain)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a single client by ID
     * 
     * @param id UUID of the client to retrieve
     * @return Optional containing the client if found, empty otherwise
     */
    @Override
    public Optional<Client> lire(@NonNull UUID id) {
        String idStr = id.toString();
        logger.debug("Reading client with ID: {}", idStr);
        
        ClientDb clientDb = storage.get(idStr);
        if (clientDb != null) {
            return Optional.of(mapper.mapToDomain(clientDb));
        }
        return Optional.empty();
    }

    /**
     * Create or update a client
     * 
     * @param client Client to persist (immutable; creates new entry or updates existing)
     * @return The persisted client (with same ID)
     */
    @Override
    public Client enregistrer(@NonNull Client client) {
        logger.debug("Registering/updating client: {}", client.getId());
        
        // Convert domain model to persistence model
        ClientDb clientDb = mapper.mapFromDomain(client);
        
        // Store in-memory (replace if exists)
        storage.put(clientDb.getId(), clientDb);
        
        // Return converted back to domain model to ensure consistency
        return mapper.mapToDomain(clientDb);
    }

    /**
     * Delete a client by ID
     * 
     * @param id UUID of the client to delete
     */
    @Override
    public void supprimer(@NonNull UUID id) {
        String idStr = id.toString();
        logger.debug("Deleting client with ID: {}", idStr);
        storage.remove(idStr);
    }

    /**
     * Get current size of in-memory storage (for testing/debugging)
     * 
     * @return Number of clients currently stored
     */
    public int size() {
        return storage.size();
    }

    /**
     * Clear all clients from in-memory storage (for testing)
     */
    public void clear() {
        logger.debug("Clearing all clients from in-memory storage");
        storage.clear();
    }
}
