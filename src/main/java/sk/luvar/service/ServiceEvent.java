package sk.luvar.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Primary interface, which should be used to caller to set-up event to some request type and data.
 */
public interface ServiceEvent {
    void resetBarrier(CompletableFuture<Void> barrierToComplete);

    void deleteAll();

    void addUser(String username);

    CompletableFuture<List<UserDTO>> getAllData(CompletableFuture<List<UserDTO>> dataHolder);

    OperationType getOperationType();
}
