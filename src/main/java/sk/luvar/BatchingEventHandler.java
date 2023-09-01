package sk.luvar;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BatchingEventHandler implements EventHandler<UnionedServiceEvent> {
    /**
     * Maximum events to process at once. This is upper cap for smart batch size.
     */
    public static final int MAXIMUM_BATCH_SIZE = 20;
    private Sequence sequenceCallback;
    private final ArrayList<UnionedServiceEvent> internalBatch = new ArrayList<>(MAXIMUM_BATCH_SIZE + 1);
    //private int currentBatchRemaining = 20;

    @Override
    public void setSequenceCallback(final Sequence sequenceCallback) {
        this.sequenceCallback = sequenceCallback;
    }

    @Override
    public void onEvent(final UnionedServiceEvent event, final long sequence, final boolean endOfBatch) {
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
                    case NOOP -> {
                    }
                    case BARRIER -> se.processingFinished();
                    case ADD_USER -> System.out.println("Insert into DB: " + se.getNewUserName());
                    case DELETE_ALL -> System.out.println("Delete all from DB");
                    case GET_ALL_USERS -> se.getDataHolderForRead().complete(List.of(new UserDTO("NOT IMPLEMENTED")));
                    default -> log.warn("Unknown operation type! Implement it! type:{}.", se.getOperationType());
                }
            });
            this.internalBatch.clear();
        }
        return internalBatchMaximumSizeReached;
    }
}
