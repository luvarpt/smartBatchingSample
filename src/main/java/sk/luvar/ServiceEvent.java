package sk.luvar;

public sealed interface ServiceEvent permits AddUserServiceEvent, BarierServiceEvent {
    void processingFinished();
}
