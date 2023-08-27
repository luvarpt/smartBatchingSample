package sk.luvar;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;

import java.util.ArrayList;

public class BatchingEventHandler implements EventHandler<UnionedServiceEvent> {
    /**
     * Maximum events to process at once. This is upper cap for smart batch size.
     */
    public static final int MAXIMUM_BATCH_SIZE = 20;
    private Sequence sequenceCallback;
    private final ArrayList<UnionedServiceEvent> internalBatch = new ArrayList(25);
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
            // DO actual I/O operation with all instances.
            System.out.println("Processed %s events in single batch.".formatted(this.internalBatch.size()));
            this.internalBatch.forEach(se -> {
                switch (se.getOperationType()) {
                    case BARRIER -> se.processingFinished();
                }
            });
            this.internalBatch.clear();
        }
        return internalBatchMaximumSizeReached;
    }
}
