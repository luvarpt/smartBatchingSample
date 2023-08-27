package sk.luvar;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class BarierServiceEvent implements ServiceEvent {
    private CompletableFuture<Void> barrierProcessed = new CompletableFuture<>();

    public void processingFinished() {
        this.barrierProcessed.complete(null);
    }

    public void waitTillProcessed() throws ExecutionException, InterruptedException {
        this.barrierProcessed.get();
    }
}
