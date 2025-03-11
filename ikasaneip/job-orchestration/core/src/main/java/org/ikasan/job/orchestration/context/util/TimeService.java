package org.ikasan.job.orchestration.context.util;

import java.time.LocalDateTime;
import java.util.Date;

// This service allows us to inject time and thus support easier time related testing
public class TimeService {
    /**
     * Get the current date and time.
     *
     * @return a Date object representing the current date and time.
     */
    public Date getDateNow() {
        return new Date();
    }

    /**
     * Gets a Date object based on the provided number of milliseconds since the epoch.
     *
     * @param milliSinceEpoch the number of milliseconds since the epoch to create the Date object
     * @return a Date object representing the time elapsed since the epoch and the given milliseconds
     */
    public Date getDate(long milliSinceEpoch) {
        return new Date(milliSinceEpoch);
    }

    /**
     * Gets the current LocalDate.
     *
     * @return the LocalDate object representing the current date.
     */
    public LocalDateTime getLocalDateNow() {
        return LocalDateTime.now();
    }
}
