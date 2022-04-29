package com.truphone.lpad.worker;

/**
 * Message used within LPAD Workers containing all necessary data
 *
 * @param <T> Data to be used on LPAD Workers operations
 */
public class LpadWorkerExchange<T> implements WorkerExchange<T> {

    private final T body;

    public LpadWorkerExchange(final T body) {
        this.body = body;
    }

    /**
     * Get data to be used on LPAD Workers operations
     *
     * @return The data to be used on LPAD Workers operations
     */
    @Override
    public T getBody() {
        return this.body;
    }
}
