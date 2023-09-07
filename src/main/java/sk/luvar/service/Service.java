package sk.luvar.service;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Slf4j
public class Service<P extends UserPersist> {
    final RingBuffer<UnionedServiceEvent> ringBuffer;
    final Disruptor<UnionedServiceEvent> disruptor;

    final P userPersist;

    public Service(P userPersist) {
        this.userPersist = userPersist;
        final ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;
        final WaitStrategy waitStrategy = new BlockingWaitStrategy();
        this.disruptor = new Disruptor<>(
                UnionedServiceEvent.EVENT_FACTORY,
                128, // we would not like to hit pathological mapping ( https://en.algorithmica.org/hpc/cpu-cache/associativity/#pathological-mappings )
                threadFactory,
                ProducerType.SINGLE,
                waitStrategy);
        //disruptor.handleEventsWith(getEventHandler());
        disruptor.handleEventsWith(new BatchingEventHandler<>(this.userPersist));
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

    /**
     * Standard approach to get data with some barrier. There is basically NOOP command, which does serve as barrier and
     * when this command is applied, read directly from storage is made.
     *
     * @return immutable snapshot of all users in storage
     */
    public List<UserDTO> getAllUsingBarrier() {
        this.waitTillNewBarrierProcessed();
        return this.userPersist.getAllBlocking();
    }

    /**
     * Non-usual approach to get data using command. Instead of using query path from CQRS pattern, we use command here.
     *
     * @return immutable snapshot of all users in storage
     */
    public List<UserDTO> getAllUsingCommand() throws ExecutionException, InterruptedException {
        final CompletableFuture<List<UserDTO>> dataHolder = new CompletableFuture<>();
        this.makeEventRequest(serviceEvent -> serviceEvent.getAllData(dataHolder));
        return dataHolder.get();
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
