package sk.luvar;

import com.lmax.disruptor.EventFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Universal pre-allocatable class which can serve for a few operations. Setting instance to some operation request
 * is done using these methods:
 * <ul>
 *     <li>{@link #addUser(String)}</li>
 *     <li>{@link #deleteAll()}</li>
 *     <li>{@link #getAllData(CompletableFuture)}</li>
 *     <li>{@link #resetBarrier(CompletableFuture)}</li>
 * </ul>
 * <p>
 * Other methods should be used only from processor.
 */
@Slf4j
public class UnionedServiceEvent implements ServiceEvent {
    @Getter
    private OperationType operationType = OperationType.NOOP;
    private CompletableFuture<Void> barrierProcessed = null;

    public final static EventFactory<UnionedServiceEvent> EVENT_FACTORY = UnionedServiceEvent::new;

    // TODO do not break "allocate once" rule. Use: ByteBuffer.allocate(120).asCharBuffer().put(username) approach
    @Getter
    private String newUserName;

    @Getter
    private CompletableFuture<List<UserDTO>> dataHolderForRead;

    public void processingFinished() {
        this.barrierProcessed.complete(null);
        this.setAllDataToNull();
    }

    @Override
    public void resetBarrier(CompletableFuture<Void> barrierToComplete) {
        this.setAllDataToNull();
        this.barrierProcessed = barrierToComplete;
        this.operationType = OperationType.BARRIER;
    }

    @Override
    public void deleteAll() {
        this.setAllDataToNull();
        this.operationType = OperationType.DELETE_ALL;
    }

    @Override
    public void addUser(String username) {
        this.setAllDataToNull();
        this.newUserName = username;
        this.operationType = OperationType.ADD_USER;
    }

    @Override
    public CompletableFuture<List<UserDTO>> getAllData(CompletableFuture<List<UserDTO>> dataHolder) {
        this.setAllDataToNull();
        this.operationType = OperationType.GET_ALL_USERS;
        this.dataHolderForRead = dataHolder;
        return dataHolder;
    }

    private void setAllDataToNull() {
        this.barrierProcessed = null;
        this.newUserName = null;
        this.dataHolderForRead = null;
    }
}
