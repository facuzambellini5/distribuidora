package com.example.distribuidora.exceptions;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entityName, Object searchParameter){
        super(entityName + " with id '" + searchParameter + "' not found.");
    }
}
