package org.ikasan.job.orchestration.context.util;

import java.util.Date;

// This service allows us to inject time and thus support easier time related testing
public class TimeService {
    public Date getDateNow() {
        return new Date();
    }

    public Date getDate(long milliSinceEpoch) {
        return new Date(milliSinceEpoch);
    }
}
