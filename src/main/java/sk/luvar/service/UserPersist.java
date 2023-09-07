package sk.luvar.service;

import java.util.List;

public interface UserPersist {
    /**
     * Get all users at once.
     *
     * @return unmodifiable list of all users stored right now in storage
     */
    List<UserDTO> getAllBlocking();

    /**
     * Blocking method for saving user in storage. When method finishes, user is stored.
     *
     * @param usernames list of new usernames which should be persisted
     */
    void saveUsers(List<String> usernames);

    void deleteAll();
}
