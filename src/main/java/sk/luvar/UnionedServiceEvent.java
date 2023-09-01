package sk.luvar;

import com.lmax.disruptor.EventFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class UnionedServiceEvent {
    @Getter
    private OperationType operationType = OperationType.NOOP;
    private CompletableFuture<Void> barrierProcessed = null;

    public final static EventFactory<UnionedServiceEvent> EVENT_FACTORY = UnionedServiceEvent::new;

    // TODO do not break "allocate once" rule. Use: ByteBuffer.allocate(120).asCharBuffer().put(username) approach
    private String newUserName;

    public void processingFinished() {
        this.barrierProcessed.complete(null);
    }

    public void resetBarrier(CompletableFuture<Void> barrierToComplete) {
        // TODO unnecessary memory allocation! Solve using mutex, lock or wait/notify with static pre-allocated instance
        this.barrierProcessed = barrierToComplete;
        this.operationType = OperationType.BARRIER;
    }

    public void deleteAll() {
        this.operationType = OperationType.DELETE_ALL;
    }

    public void addUser(String username) {
        this.newUserName = username;
        this.operationType = OperationType.ADD_USER;
    }
}
