package ch.inf.usi.mindbricks.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Singleton class for managing background tasks. Using a singleton allows to ensure only one
 * Executor is used throughout the application.
 */
public class AppExecutor {

    /**
     * Singleton instance for this class.
     */
    private static AppExecutor instance;

    /**
     * Executor for background tasks.
     */
    private final Executor executor;

    private AppExecutor(Executor executor) {
        this.executor = executor;
    }

    public static Executor getInstance() {
        if (instance == null) {
            instance = new AppExecutor(
                    Executors.newSingleThreadExecutor()
            );
        }
        return instance.executor;
    }
}
