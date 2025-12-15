package ch.inf.usi.mindbricks.drivers.calendar;

import android.app.Activity;
import android.content.Context;

import java.util.List;

import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;

/**
 * Interface for calendar data providers
 *
 * Each calendar source (Google Calendar, device calendar, iCal, etc.)
 * implements this interface. This allows:
 *
 * - Adding new calendar sources without modifying existing code
 * - Consistent API for the CalendarSyncService to work with
 * - Each driver encapsulates its own authentication and API logic
 */
public interface CalendarDriver {

    String getSourceName();

    String getDisplayName();

    boolean isAuthenticated();

    void authenticate(Activity activity, AuthCallback callback);

    void signOut(Context context);

    List<CalendarEvent> fetchEvents(long startTime, long endTime) throws CalendarSyncException;

    boolean requiresPermissions();

    String[] getRequiredPermissions();

    boolean hasRequiredPermissions(Context context);

    interface AuthCallback {
        void onAuthSuccess();

        void onAuthFailure(String error);

        void onAuthCancelled();
    }

    class CalendarSyncException extends Exception {

        public enum ErrorType {
            NETWORK_ERROR,          // No internet connection
            AUTH_EXPIRED,           // Token expired, need to re-authenticate
            AUTH_REQUIRED,          // Not authenticated at all
            PERMISSION_DENIED,      // Required permissions not granted
            API_ERROR,              // Calendar API returned an error
            PARSE_ERROR,            // Failed to parse API response
            UNKNOWN                 // Unknown error
        }

        private final ErrorType errorType;

        public CalendarSyncException(String message, ErrorType errorType) {
            super(message);
            this.errorType = errorType;
        }

        public CalendarSyncException(String message, ErrorType errorType, Throwable cause) {
            super(message, cause);
            this.errorType = errorType;
        }

        public ErrorType getErrorType() {
            return errorType;
        }

        public boolean isAuthError() {
            return errorType == ErrorType.AUTH_EXPIRED || errorType == ErrorType.AUTH_REQUIRED;
        }

        public boolean isRetryable() {
            return errorType == ErrorType.NETWORK_ERROR || errorType == ErrorType.API_ERROR;
        }
    }
}