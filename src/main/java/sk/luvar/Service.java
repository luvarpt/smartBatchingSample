package sk.luvar;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

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

    /**
     * Simple internal method to place request to the queue.
     * <p>
     * Method will request next free {@link ServiceEvent} from disruptors ringbuffer (a.k.a. processing FIFO queue),
     * than apply your passed lambda (which should set-up request) and after it, finally publishes given request
     * to the processing.
     *
     * @param serviceEventSettingLambda do make your request on passed instance in lambda. It will be published
     *                                  to the queue
     */
    private void makeEventRequest(Consumer<ServiceEvent> serviceEventSettingLambda) {
        final long sequenceId = ringBuffer.next();
        try {
            final ServiceEvent serviceEvent = ringBuffer.get(sequenceId);
            serviceEventSettingLambda.accept(serviceEvent);
        } finally {
            ringBuffer.publish(sequenceId);
        }
    }

    public void add(String username) {
        makeEventRequest(se -> se.addUser(username));
    }

    public void printAllUsingBarrier() {
        this.waitTillNewBarrierProcessed();
        throw new RuntimeException("NotImplementedYet! this.userRepository.findAll().foreach.print");
        // TODO this.userRepository.findAll().foreach.print
    }

    /**
     * Non-usual approach to get data using command. Instead of using query path from CQRS pattern, we use command here.
     */
    public void printAllUsingCommand() throws ExecutionException, InterruptedException {
        final CompletableFuture<List<UserDTO>> dataHolder = new CompletableFuture<>();
        this.makeEventRequest(serviceEvent -> serviceEvent.getAllData(dataHolder));
        dataHolder.get().forEach(System.out::println);
    }

    public void deleteAll() {
        this.makeEventRequest(ServiceEvent::deleteAll);
    }

    private void waitTillNewBarrierProcessed() {
        final CompletableFuture<Void> barrierToComplete = new CompletableFuture<>();
        this.makeEventRequest(se -> se.resetBarrier(barrierToComplete));

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
