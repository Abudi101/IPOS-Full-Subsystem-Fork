package main.db;

import main.model.CommercialApplication;

/**
 * Forwards commercial membership applications from IPOS-PU toward IPOS-SA storage.
 * When the shared SA database is not reachable, returns {@link SubmitResult#DB_UNAVAILABLE}
 * so PU can still retain the application locally (see {@link main.ui.CommercialApplicationFrame}).
 */
public class SAApplicationDbAdapter {

    public enum SubmitResult {
        SUCCESS,
        ALREADY_EXISTS,
        INVALID_INPUT,
        DB_UNAVAILABLE
    }

    /**
     * Attempts to persist the application to the SA-side store. Extend this class with JDBC
     * to your group's SA PostgreSQL schema when integration is available.
     */
    public SubmitResult submitApplication(CommercialApplication application) {
        if (application == null) {
            return SubmitResult.INVALID_INPUT;
        }
        return SubmitResult.DB_UNAVAILABLE;
    }
}
