package sk.luvar;

import lombok.extern.slf4j.Slf4j;
import net.jqwik.api.Example;

import java.util.concurrent.ExecutionException;

@Slf4j
class ServiceTest {

    @Example
    void printAllUsingBarrier() {
        Service service = new Service();
        service.add("Skúška");
        service.printAllUsingBarrier();
        service.deleteAll();
        service.shutdown();
    }

    @Example
    void printAllUsingCommand() throws ExecutionException, InterruptedException {
        log.info("Going to create service.");
        Service service = new Service();
        log.info("Going to add sample user with name USR1.");
        service.add("USR1");
        log.info("Test is about to call printAllUsingCommand() method.");
        service.printAllUsingCommand();
        log.info("Test is going to request delete of all users.");
        service.deleteAll();
        log.info("Test is about to call printAllUsingCommand() method second time, after delete all.");
        service.printAllUsingCommand();
        log.info("Test is about the end, going to call service.shutdown().");
        service.shutdown();
        log.info("Test is at the end of the Universe.");
    }
}