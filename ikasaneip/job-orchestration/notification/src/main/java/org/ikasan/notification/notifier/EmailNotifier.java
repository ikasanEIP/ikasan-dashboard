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
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.NotificationType;
import org.ikasan.notification.configuration.EmailNotificationParamsConfiguration;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAudit;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAuditRecord;
import org.ikasan.spec.scheduled.notification.model.*;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.notification.service.NotificationSendAuditService;
import org.ikasan.spec.search.SearchResults;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;

public class EmailNotifier extends AbstractEmailNotifierBase implements Notifier<GenericNotificationDetails> {

    private static final Logger LOG = LoggerFactory.getLogger(EmailNotifier.class);

    private EmailNotificationDetailsService emailNotificationDetailsService;
    private EmailNotificationContextService emailNotificationContextService;
    private NotificationSendAuditService<NotificationSendAuditRecord> notificationSendAuditService;
    private EmailNotificationParamsConfiguration emailNotificationParamsConfiguration;
    private TemplateEngine templateEngine;
    private String mailLinkUrl;

    public EmailNotifier(EmailNotificationDetailsService emailNotificationDetailsService,
                         EmailNotificationContextService emailNotificationContextService,
                         NotificationSendAuditService notificationSendAuditService,
                         EmailNotificationParamsConfiguration emailNotificationParamsConfiguration,
                         TemplateEngine templateEngine,
                         String mailLinkUrl) {
        this.emailNotificationDetailsService = emailNotificationDetailsService;
        this.emailNotificationContextService = emailNotificationContextService;
        this.notificationSendAuditService = notificationSendAuditService;
        this.emailNotificationParamsConfiguration = emailNotificationParamsConfiguration;
        this.templateEngine = templateEngine;
        this.mailLinkUrl = mailLinkUrl;
    }

    @Override
    public void invoke(GenericNotificationDetails notificationDetails) {

        // Create an EmailNotification which will be the template to create the email, if needed;
        EmailNotificationDetails emailNotificationDetails = null;

        // Check if we have an Detail Record for the notification - this tell us if we have a specific notification for a given job/child context
        EmailNotificationDetailsRecord emailNotificationDetailsRecord = emailNotificationDetailsService.
                        findByJobNameAndMonitorType(notificationDetails.getJobName(), notificationDetails.getChildContextName(), notificationDetails.getMonitorType().name());

        // If we don't have a Detail Record for the notification, check the general notification for the context and build a detail record for it.
        if (emailNotificationDetailsRecord == null) {

            String contextName = notificationDetails.getContextName();
            SearchResults<EmailNotificationContextRecord> emailNotificationContextRecordSearchResults =
                emailNotificationContextService.findByContextName(contextName, 50, 0);

            // Only if we return one record from solr which will have the definition of the general notification for this context.
            if (emailNotificationContextRecordSearchResults != null &&
                emailNotificationContextRecordSearchResults.getResultList() != null &&
                !emailNotificationContextRecordSearchResults.getResultList().isEmpty() &&
                emailNotificationContextRecordSearchResults.getResultList().size() == 1) {

                // Check general notification if we are interested in the Monitor Type that has been issued
                EmailNotificationContextRecord emailNotificationContextRecord = emailNotificationContextRecordSearchResults.getResultList().get(0);
                EmailNotificationContext emailNotificationContext = emailNotificationContextRecord.getEmailNotificationContext();
                boolean foundMonitorType = false;
                if(emailNotificationContext.getMonitorTypes() != null && !emailNotificationContext.getMonitorTypes().isEmpty()) {
                    for (String monitorType : emailNotificationContext.getMonitorTypes()) {
                        if (monitorType.equals(notificationDetails.getMonitorType().name())) {
                            foundMonitorType = true;
                            break;
                        }
                    }
                }

                // Start creating notification details if we have a general notification for this monitor type
                if(foundMonitorType) {
                    emailNotificationDetails = new EmailNotificationDetailsImpl();
                    emailNotificationDetails.setJobName(notificationDetails.getJobName());
                    emailNotificationDetails.setContextName(contextName);
                    emailNotificationDetails.setChildContextName(notificationDetails.getChildContextName());
                    emailNotificationDetails.setMonitorType(notificationDetails.getMonitorType().name());
                    // Email Send To
                    if (emailNotificationContext.getEmailSendToByMonitorType().containsKey(notificationDetails.getMonitorType().name())) {
                        emailNotificationDetails.setEmailSendTo(emailNotificationContext.getEmailSendToByMonitorType().get(notificationDetails.getMonitorType().name()));
                    } else {
                        emailNotificationDetails.setEmailSendTo(emailNotificationContext.getEmailSendTo());
                    }
                    // Email Send CC
                    if (emailNotificationContext.getEmailSendCcByMonitorType().containsKey(notificationDetails.getMonitorType().name())) {
                        emailNotificationDetails.setEmailSendCc(emailNotificationContext.getEmailSendCcByMonitorType().get(notificationDetails.getMonitorType().name()));
                    } else {
                        emailNotificationDetails.setEmailSendCc(emailNotificationContext.getEmailSendCc());
                    }
                    // Email Send Bcc
                    if (emailNotificationContext.getEmailSendBccByMonitorType().containsKey(notificationDetails.getMonitorType().name())) {
                        emailNotificationDetails.setEmailSendBcc(emailNotificationContext.getEmailSendBccByMonitorType().get(notificationDetails.getMonitorType().name()));
                    } else {
                        emailNotificationDetails.setEmailSendBcc(emailNotificationContext.getEmailSendBcc());
                    }
                    emailNotificationDetails.setAttachment(emailNotificationContext.getAttachment());
                    emailNotificationDetails.setHtml(emailNotificationContext.isHtml());
                    emailNotificationDetails.setEmailSubjectTemplate(emailNotificationContext.getEmailSubjectNotificationTemplate().get(notificationDetails.getMonitorType().name()));
                    emailNotificationDetails.setEmailBodyTemplate(emailNotificationContext.getEmailBodyNotificationTemplate().get(notificationDetails.getMonitorType().name()));
                }
            }

        } else {
            //Add the Detail Record for the notification to the details ready to be sent.
            emailNotificationDetails = emailNotificationDetailsRecord.getEmailNotificationDetails();
        }

        if (emailNotificationDetails != null) {

            // Check if we have already sent an notification for this job, childContext, monitor type and context id.
            NotificationSendAuditRecord notificationSendAuditRecord = notificationSendAuditService.find(notificationDetails.getContextInstanceId(),
                notificationDetails.getChildContextName(), notificationDetails.getJobName(), notificationDetails.getMonitorType().name(), NotificationType.EMAIL.name());

            if (notificationSendAuditRecord == null || notificationSendAuditRecord.getNotificationSendAudit() == null ||
                  !notificationSendAuditRecord.getNotificationSendAudit().isNotificationSend()) {

                LOG.info("Email Notification being sent for Context [{}], Child Context [{}], Job Name [{}] and Monitor Type [{}]",
                    emailNotificationDetails.getContextName(), emailNotificationDetails.getChildContextName(),
                    emailNotificationDetails.getJobName(), emailNotificationDetails.getMonitorType());

                // enrich the emailNotificationDetails with parameters from the emailNotificationParamsConfiguration
                this.parameterReplace(emailNotificationDetails, emailNotificationParamsConfiguration);

                final Context ctx = new Context();
                ctx.setVariable("emailNotificationDetails", emailNotificationDetails);

                // apply template parameters
                applyEmailNotificationTemplateParameters(emailNotificationDetails, notificationDetails);


                emailNotificationDetails.setEmailBody(this.templateEngine.process(emailNotificationDetails.getEmailBodyTemplate(), ctx));
                emailNotificationDetails.setEmailSubject(this.templateEngine.process(emailNotificationDetails.getEmailSubjectTemplate(), ctx));

                /*
                if (StringUtils.isNotBlank(emailNotificationDetails.getEmailBodyTemplate())) {
                    emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_BODY_LINK_1.name(), createMailLink(notificationDetails, false));
                    emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_BODY_LINK_2.name(), createMailLink(notificationDetails, true));
                    emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_BODY_MESSAGE_FROM_MONITOR.name(), notificationDetails.getMessage());

                }
                if (StringUtils.isNotBlank(emailNotificationDetails.getEmailSubjectTemplate())) {
                    emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_SUBJECT_LINK.name(), "link-3");

                }
                 */

                super.sendEmail(emailNotificationDetails);

                // save the notification
                NotificationSendAudit notificationSendAudit = new SolrNotificationSendAudit();
                notificationSendAudit.setContextInstanceId(notificationDetails.getContextInstanceId());
                notificationSendAudit.setJobName(notificationDetails.getJobName());
                notificationSendAudit.setContextName(notificationDetails.getChildContextName());
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

    /**
     * Method to apply the parameters required for the templates.
     * @param emailNotificationDetails
     * @param notificationDetails
     */
    private void applyEmailNotificationTemplateParameters(EmailNotificationDetails emailNotificationDetails, GenericNotificationDetails notificationDetails) {
        emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_BODY_LINK_1.name(), createMailLink(notificationDetails, false));
        emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_BODY_LINK_2.name(), createMailLink(notificationDetails, true));
        emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_BODY_MESSAGE_FROM_MONITOR.name(), notificationDetails.getMessage());
        emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_AGENT_NAME.name(), notificationDetails.getAgentName());
        emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_CONTEXT_NAME.name(), emailNotificationDetails.getContextName());
        emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_JOB_NAME.name(), notificationDetails.getJobName());
        emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_CHILD_CONTEXT.name(), notificationDetails.getChildContextName());
        emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_FILE_NAME.name(), notificationDetails.getFileName());
        emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_FILE_PATH.name(), notificationDetails.getFilePath());

        DateTimeFormatter dtf = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss z");
        emailNotificationDetails.getEmailNotificationTemplateParameters().put(EmailNotificationTemplateParameters.EMAIL_FIRE_TIME.name(), dtf.print(notificationDetails.getFiredTime()));
    }

    private String createMailLink(GenericNotificationDetails notificationDetails, boolean isErrorLog) {
        // http://localhost:9090/schedulerJobLogFile/526879ab-58e7-4cd7-8661-2d48baf47d40:::CONTEXT-140537370:::97656185:::true
        return mailLinkUrl+notificationDetails.getContextInstanceId()+":::"+notificationDetails.getChildContextName()+":::"+notificationDetails.getJobName()+":::"+isErrorLog;
    }

    /**
     * This method will enrich the email notification template with values found in the configuration.
     * This will analyse the following attributes from emailNotificationDetails only:
     *
     * emailNotificationTemplateParameters
     * emailSendTo
     * emailSendCc
     * emailSendBcc
     * emailSubject
     * emailSubjectTemplate
     * emailBody
     * emailBodyTemplate
     *
     * In the emailNotificationDetails, the format of the value you want to replace needs to be in the format of ${key}
     * If this is found on the emailNotificationParamsConfiguration then it will do a replace
     *
     * For emailSendTo/Cc/Bcc, as these are list, it will replace and create new list based on the values if there is an
     * exact match, else it will do a find and replace.
     *
     * Please read org.ikasan.notification.configuration.EmailNotificationParamsConfiguration.loadMapFromFile for more
     * details on how the key value pairs are read and created from the config file.
     *
     * @param emailNotificationDetails the notification template that is store on solr to based the email on
     * @param emailNotificationParamsConfiguration key value pairs of config to update the template with
     */
    void parameterReplace(EmailNotificationDetails emailNotificationDetails, EmailNotificationParamsConfiguration emailNotificationParamsConfiguration) {

        // Get the map of all the parameters to replace with based on the Context Name
        Map<String, String> mapOfParamToReplace = emailNotificationParamsConfiguration.getParamsToReplace().get(emailNotificationDetails.getContextName());
        if (mapOfParamToReplace != null && !mapOfParamToReplace.isEmpty()) {

            // Iterate through all the parameters to replace and try and update the emailNotificationDetails
            mapOfParamToReplace.forEach((key, value) -> {

                // EmailNotificationTemplateParameters
                if (emailNotificationDetails.getEmailNotificationTemplateParameters() != null &&
                    !emailNotificationDetails.getEmailNotificationTemplateParameters().isEmpty()) {

                    // For each EmailNotificationTemplateParameters, find and replace the value if we match the key from mapOfParamToReplace
                    emailNotificationDetails.getEmailNotificationTemplateParameters().forEach((ke ,ve) -> {
                        emailNotificationDetails.getEmailNotificationTemplateParameters().put(ke, StringUtils.replace(ve, key, value));
                    });
                }

                // EmailSendTo
                List<String> emailSendTo = new ArrayList<>(); // create and rebuild SendTo
                if (emailNotificationDetails.getEmailSendTo() != null &&
                    !emailNotificationDetails.getEmailSendTo().isEmpty()) {

                    emailNotificationDetails.getEmailSendTo().forEach(s -> {
                        // If the key from mapOfParamToReplace matches exactly the value in List then replace it,
                        // making sure if there are commas in the value from the mapOfParamToReplace that we create new list items
                        if (key.equals(s)) {
                            emailSendTo.addAll(Arrays.asList(value.split(",")));
                        } else {
                            // else treat it like a find and replace
                            emailSendTo.add(StringUtils.replace(s, key, value));
                        }
                    });
                    // update with the new list
                    emailNotificationDetails.setEmailSendTo(emailSendTo);
                }

                // EmailSendCc
                List<String> emailSendCc = new ArrayList<>(); // create and rebuild SendCc
                if (emailNotificationDetails.getEmailSendCc() != null &&
                    !emailNotificationDetails.getEmailSendCc().isEmpty()) {

                    emailNotificationDetails.getEmailSendCc().forEach(s -> {
                        // If the key from mapOfParamToReplace matches exactly the value in List then replace it,
                        // making sure if there are commas in the value from the mapOfParamToReplace that we create new list items
                        if (key.equals(s)) {
                            emailSendCc.addAll(Arrays.asList(value.split(",")));
                        } else {
                            // else treat it like a find and replace
                            emailSendCc.add(StringUtils.replace(s, key, value));
                        }
                    });
                    // update with the new list
                    emailNotificationDetails.setEmailSendCc(emailSendCc);
                }

                // EmailSendBcc
                List<String> emailSendBcc = new ArrayList<>(); // create and rebuild SendBcc
                if (emailNotificationDetails.getEmailSendBcc() != null &&
                    !emailNotificationDetails.getEmailSendBcc().isEmpty()) {

                    emailNotificationDetails.getEmailSendBcc().forEach(s -> {
                        // If the key from mapOfParamToReplace matches exactly the value in List then replace it,
                        // making sure if there are commas in the value from the mapOfParamToReplace that we create new list items
                        if (key.equals(s)) {
                            emailSendBcc.addAll(Arrays.asList(value.split(",")));
                        } else {
                            // else treat it like a find and replace
                            emailSendBcc.add(StringUtils.replace(s, key, value));
                        }
                    });
                    // update with the new list
                    emailNotificationDetails.setEmailSendBcc(emailSendBcc);
                }

                // EmailSubject
                if (StringUtils.isNotBlank(emailNotificationDetails.getEmailSubject())) {
                    emailNotificationDetails.setEmailSubject(StringUtils.replace(emailNotificationDetails.getEmailSubject(), key, value));
                }

                // EmailSubjectTemplate
                if (StringUtils.isNotBlank(emailNotificationDetails.getEmailSubjectTemplate())) {
                    emailNotificationDetails.setEmailSubjectTemplate(StringUtils.replace(emailNotificationDetails.getEmailSubjectTemplate(), key, value));
                }

                // EmailBody
                if (StringUtils.isNotBlank(emailNotificationDetails.getEmailBody())) {
                    emailNotificationDetails.setEmailBody(StringUtils.replace(emailNotificationDetails.getEmailBody(), key, value));
                }

                // EmailBodyTemplate
                if (StringUtils.isNotBlank(emailNotificationDetails.getEmailBodyTemplate())) {
                    emailNotificationDetails.setEmailBodyTemplate(StringUtils.replace(emailNotificationDetails.getEmailBodyTemplate(), key, value));
                }

            });
        }
    }
}
