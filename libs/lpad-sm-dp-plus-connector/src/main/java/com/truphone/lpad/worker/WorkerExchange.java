package com.truphone.lpad.worker;

/**
 * Message used within Workers containing all necessary data
 * @param <T> Data to be used on Workers operations
 */
public interface WorkerExchange<T> {
    /**
     * Get data to be used on Workers operations
     *
     * @return The data to be used on Workers operations
     */
    T getBody();
}
