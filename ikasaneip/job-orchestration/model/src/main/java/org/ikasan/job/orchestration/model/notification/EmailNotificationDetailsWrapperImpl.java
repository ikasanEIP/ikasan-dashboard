package org.ikasan.job.orchestration.model.notification;

import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsWrapper;

import java.util.List;

public class EmailNotificationDetailsWrapperImpl implements EmailNotificationDetailsWrapper {

    private List<EmailNotificationDetails> emailNotificationDetails;


    @Override
    public List<EmailNotificationDetails> getEmailNotificationDetails() {
        return emailNotificationDetails;
    }

    @Override
    public void setEmailNotificationDetails(List<EmailNotificationDetails> emailNotificationDetails) {
        this.emailNotificationDetails = emailNotificationDetails;
    }
}
