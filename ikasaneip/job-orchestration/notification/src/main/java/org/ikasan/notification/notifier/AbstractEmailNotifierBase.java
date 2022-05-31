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

import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.Notifier;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

/**
 * Ikasan default email notifier implementation.
 *
 * @author Ikasan Development Team
 */
public abstract class AbstractEmailNotifierBase implements Notifier<GenericNotificationDetails>
{
    /** logger instance */
    private static Logger logger = LoggerFactory.getLogger(AbstractEmailNotifierBase.class);

    /** regular expression for splitting grouped email addresses in a single String separated by comma, semi-colon, or space */
    private static String EMAIL_ADDRESS_SPLIT_REGEXP = ",| |;";

    /** date time formatter */
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("Y-MM-dd HH:mm:ss.SSS Z");

    /** configured resource identifier */
    private String configuredResourceId;

    /** mail session */
    private Session session;


    @Override
    public void invoke(GenericNotificationDetails notificationDetails)
    {
        sendNotification(notificationDetails);
    }


    protected void sendNotification(GenericNotificationDetails notificationDetails)
    {
        MimeMessage message = new MimeMessage(session);
/*
        message.addRecipients(Message.RecipientType.TO, toArray( configuration.getToRecipients() ));
        message.addRecipients(Message.RecipientType.CC, toArray( configuration.getCcRecipients() ));
        message.addRecipients(Message.RecipientType.BCC, toArray( configuration.getBccRecipients() ));

        if(configuration.getSubject() == null)
        {
            message.setSubject( "[" + env + "] " + name + " is " + currentState );
        }
        else
        {
            String subject = configuration.getSubject().replaceAll("\\$\\{environment\\}", env)
                    .replaceAll("\\$\\{name\\}", name)
                    .replaceAll("\\$\\{state\\}", currentState);

            message.setSubject(subject);
        }

        BodyPart bodyPart = new MimeBodyPart();
        if(content != null)
        {
            bodyPart.setText(content.toString());
        }

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(bodyPart);
        message.setContent(multipart);
        Transport.send(message);

 */
    }

    /**
     * Convert the email addresses to actual Address implementations
     * @param emailAddresses
     * @return
     */
    protected Address[] toArray(List<String> emailAddresses)
    {
        if(emailAddresses == null)
        {
            return null;
        }

        // fix any email Strings which contain mulitple email addresses
        emailAddresses = expandTokenisedAddresses(emailAddresses);

        int index = 0;
        Address[] addresses = new Address[emailAddresses.size()];
        for(String emailAddress:emailAddresses)
        {
            try
            {
                addresses[index++] = new InternetAddress(emailAddress);
            }
            catch(AddressException e)
            {
                logger.warn("Invalid email address", e);
            }
        }

        return addresses;
    }

    /**
     * Ensure email addresses are tokenised correctly when seprated by commas, spaces, or semi-colons.
     * @param addresses
     * @return
     */
    protected List<String> expandTokenisedAddresses(List<String> addresses)
    {
        List<String> reviewedAddresses = new ArrayList<String>();

        for(String address:addresses)
        {
            String[] splitAddresses = address.split(EMAIL_ADDRESS_SPLIT_REGEXP);
            {
                for(String splitAddress:splitAddresses)
                {
                    if(splitAddress.length() > 0)
                    {
                        reviewedAddresses.add(splitAddress);
                    }
                }
            }
        }

        return reviewedAddresses;
    }

}
