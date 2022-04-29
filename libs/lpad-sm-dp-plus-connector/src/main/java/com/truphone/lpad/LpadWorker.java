package com.truphone.lpad;

import com.truphone.lpad.worker.WorkerExchange;

/**
 * Representation of all classes that contains the implementation of LPAD operations
 */
public interface LpadWorker<T extends WorkerExchange, E> {

    /**
     * Execute operation
     *
     * @param input Input parameters
     * @return Result of the LPAD operation
     */
    E run(T input);
}