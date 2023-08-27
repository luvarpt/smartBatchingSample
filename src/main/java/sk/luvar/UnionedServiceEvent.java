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

    public final static EventFactory EVENT_FACTORY = UnionedServiceEvent::new;

    public void processingFinished() {
        this.barrierProcessed.complete(null);
    }

    public CompletableFuture<Void> resetBarrier(CompletableFuture<Void> barrierToComplete) {
        // TODO unnecessary memory allocation! Solve using mutex, lock or wait/notify with static pre-allocated instance
        this.barrierProcessed = barrierToComplete;
        operationType = OperationType.BARRIER;
        return this.barrierProcessed;
    }

    /*public void waitTillProcessed() throws ExecutionException, InterruptedException {
        if (this.operationType != OperationType.BARRIER) {
            throw new UnsupportedOperationException("UnionedServiceEvent is not in barrier operation! Switch it to given operation using resetBarrier() method, or process it correctly according current operation type.");
        }
        this.barrierProcessed.get();
    }*/

    public void deleteAll() {
        operationType = OperationType.DELETE_ALL;
    }
}
