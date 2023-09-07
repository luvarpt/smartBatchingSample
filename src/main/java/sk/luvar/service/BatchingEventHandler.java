package sk.luvar.service;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class BatchingEventHandler<P extends UserPersist> implements EventHandler<UnionedServiceEvent> {
    /**
     * Maximum events to process at once. This is upper cap for smart batch size.
     */
    public static final int MAXIMUM_BATCH_SIZE = 20;
    private Sequence sequenceCallback;
    private final ArrayList<UnionedServiceEvent> internalBatch = new ArrayList<>(MAXIMUM_BATCH_SIZE + 1);

    final P userPersist;

    @Override
    public void setSequenceCallback(final Sequence sequenceCallback) {
        this.sequenceCallback = sequenceCallback;
    }

    @Override
    public void onEvent(final UnionedServiceEvent event, final long sequence, final boolean endOfBatch) {
        // DO actual batching, batch only add user things (as they are only designed that way for now).
        this.internalBatch.add(event);

        boolean logicalChunkOfWorkComplete = isLogicalChunkOfWorkComplete(endOfBatch);
        if (logicalChunkOfWorkComplete) {
            // mark given instances as "free". From now, we can not use any older UnionedServiceEvent instances!
            // We have forgotten them in isLogicalChunkOfWorkComplete method
            sequenceCallback.set(sequence);
        }
    }

    private boolean isLogicalChunkOfWorkComplete(boolean forceEnd) {
        // Consider, what is big batch and do test for large batch when I/O is involved
        boolean internalBatchMaximumSizeReached = this.internalBatch.size() >= MAXIMUM_BATCH_SIZE;
        if (internalBatchMaximumSizeReached || forceEnd) {
            // DO actual I/O operation with all pre-batched instances.
            System.out.printf("Processed %s events in single batch.%n", this.internalBatch.size());
            this.internalBatch.forEach(se -> {
                switch (se.getOperationType()) {
                    case NOOP -> {}
                    case BARRIER -> se.processingFinished();
                    case ADD_USER -> userPersist.saveUsers(List.of(se.getNewUserName()));
                    case DELETE_ALL -> userPersist.deleteAll();
                    case GET_ALL_USERS -> se.getDataHolderForRead().complete(userPersist.getAllBlocking());
                    default -> log.warn("Unknown operation type! Implement it! type:{}.", se.getOperationType());
                }
            });
            this.internalBatch.clear();
        }
        return internalBatchMaximumSizeReached;
    }
}
