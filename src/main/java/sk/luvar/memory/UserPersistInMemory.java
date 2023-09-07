package sk.luvar.memory;

import sk.luvar.service.UserDTO;
import sk.luvar.service.UserPersist;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple implementation for basic architectural idea test.
 */
public class UserPersistInMemory implements UserPersist {
    /**
     * Wannabe sequence.
     */
    private long nextId = 1;
    private final ArrayList<UserDTO> storage = new ArrayList<>(20);

    @Override
    public List<UserDTO> getAllBlocking() {
        return List.copyOf(storage);
    }

    @Override
    public void saveUsers(List<String> username) {
        // bad for performance, but this class is about testing instead of production.
        this.storage.addAll(username.stream().map(u -> new UserDTO(nextId++, "", u)).toList());
    }

    @Override
    public void deleteAll() {
        this.storage.clear();
        this.nextId = 1;
    }
}
