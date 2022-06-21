/*
 * $Id$
 * $URL$
 *
 * ====================================================================
 * Ikasan Enterprise Integration Platform
 *
 * Distributed under the Modified BSD License.
 * Copyright notice: The copyright for this software and a full listing
 * of individual contributors are as shown in the packaged copyright.txt
 * file.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  - Neither the name of the ORGANIZATION nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 */
package org.ikasan.notification.notifier;

import org.apache.commons.lang3.StringUtils;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.NotificationType;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAudit;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAuditRecord;
import org.ikasan.spec.scheduled.notification.model.*;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.notification.service.NotificationSendAuditService;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;

public class EmailNotifier extends AbstractEmailNotifierBase implements Notifier<GenericNotificationDetails> {

    private EmailNotificationDetailsService<EmailNotificationDetailsRecord> emailNotificationDetailsService;
    private NotificationSendAuditService<NotificationSendAuditRecord> notificationSendAuditService;
    private TemplateEngine templateEngine;

    public EmailNotifier(EmailNotificationDetailsService emailNotificationDetailsService, NotificationSendAuditService notificationSendAuditService, TemplateEngine templateEngine) {
        this.emailNotificationDetailsService = emailNotificationDetailsService;
        this.notificationSendAuditService = notificationSendAuditService;
        this.templateEngine = templateEngine;
    }

    @Override
    public void invoke(GenericNotificationDetails notificationDetails) {
        EmailNotificationDetailsRecord emailNotificationDetailsRecord = emailNotificationDetailsService.
                        findByJobNameAndMonitorType(notificationDetails.getJobName(), notificationDetails.getContextName(), notificationDetails.getMonitorType().name());

        if (emailNotificationDetailsRecord != null) {
            EmailNotificationDetails emailNotificationDetails = emailNotificationDetailsRecord.getEmailNotificationDetails();

            NotificationSendAuditRecord notificationSendAuditRecord = notificationSendAuditService.find(notificationDetails.getContextInstanceId(),
                notificationDetails.getJobName(), notificationDetails.getMonitorType().name(), NotificationType.EMAIL.name());

            if (notificationSendAuditRecord == null || notificationSendAuditRecord.getNotificationSendAudit() == null ||
                  !notificationSendAuditRecord.getNotificationSendAudit().isNotificationSend()) {

                final Context ctx = new Context();
                ctx.setVariable("emailNotificationDetails", emailNotificationDetails);

                if (StringUtils.isNotBlank(emailNotificationDetails.getEmailBodyTemplate())) {
                    // todo fix this
                    emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_BODY_LINK.name(), "link-1");
                    emailNotificationDetails.setEmailBody(this.templateEngine.process(emailNotificationDetails.getEmailBodyTemplate(), ctx));
                }
                if (StringUtils.isNotBlank(emailNotificationDetails.getEmailSubjectTemplate())) {
                    emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_SUBJECT_LINK.name(), "link-2");
                    emailNotificationDetails.setEmailSubject(this.templateEngine.process(emailNotificationDetails.getEmailSubjectTemplate(), ctx));
                }

                super.sendEmail(emailNotificationDetails);

                // save the notification
                NotificationSendAudit notificationSendAudit = new SolrNotificationSendAudit();
                notificationSendAudit.setContextInstanceId(notificationDetails.getContextInstanceId());
                notificationSendAudit.setJobName(notificationDetails.getJobName());
                notificationSendAudit.setMonitorType(notificationDetails.getMonitorType().name());
                notificationSendAudit.setNotifierType(NotificationType.EMAIL.name());
                notificationSendAudit.setNotificationSend(true);

                NotificationSendAuditRecord record = new SolrNotificationSendAuditRecord();
                record.setNotificationSendAudit(notificationSendAudit);
                record.setTimestamp(new Date().getTime());

                notificationSendAuditService.save(record);
            }
        }
    }
}
