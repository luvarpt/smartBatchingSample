package sk.luvar;

import lombok.extern.slf4j.Slf4j;
import net.jqwik.api.Example;
import net.jqwik.testcontainers.Container;
import net.jqwik.testcontainers.Testcontainers;
import org.assertj.core.api.Assertions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import sk.luvar.database.UserPersistInPostgresql;
import sk.luvar.service.Service;
import sk.luvar.service.UserDTO;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Testcontainers
@Slf4j
class ServicePostgresqlTest {
    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.4"));

    @Example
    void printAllUsingBarrier() {
        // TODO do not forget to use UNLOGGED table schema for efficiency
        final Service<UserPersistInPostgresql> service = new Service<>(new UserPersistInPostgresql(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        service.add("Skúška");
        service.getAllUsingBarrier();
        service.deleteAll();
        service.shutdown();
    }

    @Example
    void printAllUsingCommand() throws ExecutionException, InterruptedException {
        log.info("Going to create service.");
        //final Service service = new Service(new UserPersistInPostgresql(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        final Service<UserPersistInPostgresql> service = new Service<>(new UserPersistInPostgresql("jdbc:postgresql://localhost:15432/postgres", "postgres", "yourpgpass"));
        log.info("Going to add sample user with name USR1.");
        service.add("USR1");
        service.add("USR2");
        log.info("Test is about to call printAllUsingCommand() method.");
        final List<UserDTO> allUsingCommand = service.getAllUsingCommand();
        log.info("There are these users: {}.", allUsingCommand);
        Assertions.assertThat(allUsingCommand)
                        .size().isEqualTo(2);
        log.info("Test is going to request delete of all users.");
        service.deleteAll();
        log.info("Test is about to call printAllUsingCommand() method second time, after delete all.");
        service.getAllUsingCommand();
        log.info("Test is about the end, going to call service.shutdown().");
        service.shutdown();
        log.info("Test is at the end of the Universe.");
    }
}