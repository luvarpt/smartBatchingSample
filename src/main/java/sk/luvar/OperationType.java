package sk.luvar;

public enum OperationType {
    NOOP,
    DELETE_ALL,
    ADD_USER,
    /**
     * Make it easy to implement CQRS pattern barrier, to read from repository after barrier.
     */
    BARRIER,
    /**
     * Make a non-standard approach to get snapshot of data from service using command.
     */
    GET_ALL_USERS
}
