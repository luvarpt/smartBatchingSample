package sk.luvar;

import lombok.extern.slf4j.Slf4j;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.NotBlank;
import org.assertj.core.api.Assertions;
import sk.luvar.memory.UserPersistInMemory;
import sk.luvar.service.Service;
import sk.luvar.service.UserDTO;

import java.util.concurrent.ExecutionException;

@Slf4j
class ServiceInMemoryTest {
    @Example
    void printAllUsingBarrier() {
        Service<UserPersistInMemory> service = new Service<>(new UserPersistInMemory());
        service.add("Skúška");
        service.getAllUsingBarrier();
        service.deleteAll();
        service.shutdown();
    }

    @Example
    void printAllUsingCommand() throws ExecutionException, InterruptedException {
        log.info("Going to create service.");
        Service<UserPersistInMemory> service = new Service<>(new UserPersistInMemory());
        log.info("Going to add sample user with name USR1.");
        service.add("USR1");
        log.info("Test is about to call printAllUsingCommand() method.");
        service.getAllUsingCommand();
        log.info("Test is going to request delete of all users.");
        service.deleteAll();
        log.info("Test is about to call printAllUsingCommand() method second time, after delete all.");
        service.getAllUsingCommand();
        log.info("Test is about the end, going to call service.shutdown().");
        service.shutdown();
        log.info("Test is at the end of the Universe.");
    }

    @Property(tries = 2000)
    void addSomeUserAndThanGetItUsingBarrier(@ForAll @NotBlank String username) {
        final Service<UserPersistInMemory> service = new Service<>(new UserPersistInMemory());
        service.add(username);
        final java.util.List<UserDTO> getResult = service.getAllUsingBarrier();
        Assertions.assertThat(getResult)
                .extracting("username")
                .containsOnly(username);
        service.deleteAll();
        final java.util.List<UserDTO> getResultAfterDelete = service.getAllUsingBarrier();
        Assertions.assertThat(getResultAfterDelete).isEmpty();
        service.shutdown();
    }
}