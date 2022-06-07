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
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.Notifier;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

public class EmailNotifier extends AbstractEmailNotifierBase implements Notifier<GenericNotificationDetails> {

    private EmailNotificationDetailsService<SolrEmailNotificationDetailsRecord> emailNotificationDetailsService;
    private TemplateEngine templateEngine;

    public EmailNotifier(EmailNotificationDetailsService emailNotificationDetailsService, TemplateEngine templateEngine) {
        this.emailNotificationDetailsService = emailNotificationDetailsService;
        this.templateEngine = templateEngine;
    }

    @Override
    public void invoke(GenericNotificationDetails notificationDetails) {
        SolrEmailNotificationDetailsRecord solrEmailNotificationDetailsRecord = emailNotificationDetailsService.
                        findByJobNameAndMonitorType(notificationDetails.getJobName(), notificationDetails.getMonitorType().name());

        if (solrEmailNotificationDetailsRecord != null) {
            EmailNotificationDetails emailNotificationDetails = solrEmailNotificationDetailsRecord.getEmailNotificationDetails();

            final Context ctx = new Context();
            // todo fix this
            ctx.setVariable("emailNotificationDetails", emailNotificationDetails);

            if (StringUtils.isNotBlank(emailNotificationDetails.getEmailBodyTemplate())) {
                emailNotificationDetails.setEmailBody(this.templateEngine.process(emailNotificationDetails.getEmailBodyTemplate(), ctx));
            }
            if (StringUtils.isNotBlank(emailNotificationDetails.getEmailSubjectTemplate())) {
                emailNotificationDetails.setEmailSubject(this.templateEngine.process(emailNotificationDetails.getEmailSubjectTemplate(), ctx));
            }

            super.sendEmail(emailNotificationDetails);
        }
    }
}
