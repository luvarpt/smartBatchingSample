package sk.luvar.memory;

import junit.framework.TestCase;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.NotBlank;
import org.assertj.core.api.Assertions;
import sk.luvar.service.UserDTO;

import java.util.List;
import java.util.stream.Collectors;

public class UserPersistInMemoryTest extends TestCase {
    final UserPersistInMemory persister = new UserPersistInMemory();

    /**
     * Simple unit test of {@link UserPersistInMemory} class for these invariants:
     * <ol>
     *     <li>nothing is in storage at the beginning</li>
     *     <li>after inserting random users (note, there can be duplicates), they are all retrievable using {@link UserPersistInMemory#getAllBlocking()} method</li>
     *     <li>all retrieved {@link UserDTO} instances have different uuid values</li>
     *     <li>all retrieved {@link UserDTO} instances have different id values</li>
     *     <li>after issuing delete, there is empty list returned from {@link UserPersistInMemory#getAllBlocking()} method</li>
     * </ol>
     *
     * @param usernames list of usernames to be used in test
     */
    @Property
    void persistAndRetrieve(@ForAll @NotBlank List<String> usernames) {
        Assertions.assertThat(persister.getAllBlocking()).isEmpty();
        persister.saveUsers(usernames);
        final List<UserDTO> allUsersFromPersist = persister.getAllBlocking();
        Assertions.assertThat(allUsersFromPersist)
                // Do not use UserDTO::username, if UserDTO is actual DTO!
                .extracting("username")
                .containsExactlyElementsOf(usernames);
        Assertions.assertThat(allUsersFromPersist.stream().map(UserDTO::uuid).collect(Collectors.toSet()))
                .as("UUIDs from storage should be unique!")
                .size().isEqualTo(allUsersFromPersist.size());
        Assertions.assertThat(allUsersFromPersist.stream().map(UserDTO::id).collect(Collectors.toSet()))
                .as("IDs from storage should be unique!")
                .size().isEqualTo(allUsersFromPersist.size());
        persister.deleteAll();
        Assertions.assertThat(persister.getAllBlocking()).isEmpty();
    }
}