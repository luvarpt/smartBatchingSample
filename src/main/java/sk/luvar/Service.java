package sk.luvar;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class Service {
    final RingBuffer<UnionedServiceEvent> ringBuffer;
    final Disruptor<UnionedServiceEvent> disruptor;

    public Service() {
        final ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;
        final WaitStrategy waitStrategy = new BlockingWaitStrategy();
        this.disruptor = new Disruptor<>(
                UnionedServiceEvent.EVENT_FACTORY,
                128, // we would not like to hit pathological mapping ( https://en.algorithmica.org/hpc/cpu-cache/associativity/#pathological-mappings )
                threadFactory,
                ProducerType.SINGLE,
                waitStrategy);
        //disruptor.handleEventsWith(getEventHandler());
        disruptor.handleEventsWith(new BatchingEventHandler());
        ringBuffer = disruptor.start();
    }

    public void add(String username) {
        final long sequenceId = ringBuffer.next();
        final UnionedServiceEvent serviceEvent = ringBuffer.get(sequenceId);
        serviceEvent.addUser(username);
        ringBuffer.publish(sequenceId);
    }

    public void printAll() {
        this.waitTillNewBarrierProcessed();
        throw new RuntimeException("NotImplementedYet! this.userRepository.findAll().foreach.print");
        // TODO this.userRepository.findAll().foreach.print
    }

    public void deleteAll() {
        final long sequenceId = ringBuffer.next();
        final UnionedServiceEvent serviceEvent = ringBuffer.get(sequenceId);
        serviceEvent.deleteAll();
        ringBuffer.publish(sequenceId);
    }

    private void waitTillNewBarrierProcessed() {
        final long sequenceId = ringBuffer.next();
        final CompletableFuture<Void> barrierToComplete = new CompletableFuture<>();

        try {
            final UnionedServiceEvent serviceEvent = ringBuffer.get(sequenceId);
            // Pass own instance to get "result", i.e. signal of processed event (command)
            serviceEvent.resetBarrier(barrierToComplete);
            log.debug("Going to publish barrier command.");
        } finally {
            ringBuffer.publish(sequenceId);
        }

        try {
            log.debug("Going to wait till barrier command is processed (and all commands issued before it).");
            barrierToComplete.get();
        } catch (ExecutionException | InterruptedException ex) {
            throw new RuntimeException("Unexpected error while waiting for applying previous commands.", ex);
        }
    }

    public void shutdown() {
        this.disruptor.shutdown();
    }
}
