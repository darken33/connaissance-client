package com.sqli.workshop.ddd.connaissance.client.domain.exceptions;

/**
 * AdresseInvalideException
 */
public class ClientInconnuException extends Exception {

    public ClientInconnuException() {
        super("Client Inconnu");
    }
}