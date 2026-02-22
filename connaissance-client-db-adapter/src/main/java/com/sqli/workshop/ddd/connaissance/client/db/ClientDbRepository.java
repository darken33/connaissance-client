package com.sqli.workshop.ddd.connaissance.client.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
/**
 * ClientDbRepository - TODO: description
 *
 * @author TODO
 */
public interface ClientDbRepository extends MongoRepository<ClientDb, String> {
}