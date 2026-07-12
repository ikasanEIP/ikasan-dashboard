package org.ikasan.job.orchestration.rest.dashboard;

import org.ikasan.job.orchestration.model.notification.EmailNotificationContextImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationContextRecordImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.rest.dashboard.model.dto.ErrorDto;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Date;

/**
 * This Class will allow the user to provision Notifications at a Context Level.
 * @author Ikasan Development Team
 */
@RequestMapping("/rest")
@RestController
public class EmailNotificationContextController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationContextController.class);
    private JsonMapper mapper;
    private EmailNotificationContextService emailNotificationContextService;

    public EmailNotificationContextController(EmailNotificationContextService emailNotificationContextService) {
        this.emailNotificationContextService = emailNotificationContextService;
        if(this.emailNotificationContextService == null) {
            throw new IllegalArgumentException("emailNotificationContextService cannot be null!");
        }
        this.mapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    }

    @RequestMapping(method = RequestMethod.PUT,
        value = "/emailNotificationContext/save")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity saveEmailNotificationContext(@RequestBody String emailNotificationContextJsonPayload) {
        try {
            EmailNotificationContext emailNotificationContext = this.mapper.readValue(emailNotificationContextJsonPayload, EmailNotificationContextImpl.class);

            EmailNotificationContextRecord record = new EmailNotificationContextRecordImpl();
            record.setEmailNotificationContext(emailNotificationContext);
            record.setModifiedTimestamp(new Date().getTime());

            this.emailNotificationContextService.save(record);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to perform a save of EmailNotificationContext! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity( HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = {"/emailNotificationContext/get/{contextName}/{limit}/{offset}"})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity getEmailNotificationByContextName(@PathVariable(value = "contextName") String contextName,
                                                               @PathVariable(value = "limit") int limit,
                                                               @PathVariable(value = "offset") int offset) {
        try {
            SearchResults<EmailNotificationContextRecord> notificationResults = emailNotificationContextService.findByContextName(contextName, limit, offset);

            JsonMapper objectMapper = ObjectMapperFactory.newInstance();
            objectMapper = objectMapper.rebuild()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build(); // Export with pretty lines

            String jsonString = objectMapper.writeValueAsString(notificationResults);
            if (jsonString == null || "".equals(jsonString) || "null".equals(jsonString)) {
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity(jsonString, HttpStatus.OK);
            }
        } catch (Exception e) {
            LOGGER.error("Error converting to JSON", e);
            return new ResponseEntity("Error converting to JSON", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE,
        value = "/emailNotificationContext/delete/{contextName}")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity deleteByContextName(@PathVariable(value = "contextName") String contextName) {
        try {
            emailNotificationContextService.deleteByContextName(contextName);
            return new ResponseEntity(HttpStatus.OK);

        } catch (Exception e) {
            String message = String.format("Got exception trying to delete notification for the context [%s]. Error [%s]", contextName, e.getMessage());
            LOGGER.warn(message);
            return new ResponseEntity(message, HttpStatus.BAD_REQUEST);
        }
    }
}
