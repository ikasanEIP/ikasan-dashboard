package org.ikasan.job.orchestration.rest.dashboard;

import org.apache.commons.lang3.StringUtils;
import org.ikasan.orchestration.service.context.util.ContextExportZipUtils;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;

/**
 * Rest service to allow downloading of the context in a zip format
 *
 * @author Ikasan Development Team
 */
@RequestMapping("/rest/export/context")
@RestController
public class ContextExportControl {

    private static Logger LOG = LoggerFactory.getLogger(ContextExportControl.class);

    private ScheduledContextService scheduledContextService;
    private SchedulerJobService schedulerJobService;

    public ContextExportControl(ScheduledContextService scheduledContextService, SchedulerJobService schedulerJobService) {
        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
    }

    /**
     * To use on command line in unix via curl:
     * curl -u username:password http://{dashboardUrl}/rest/export/context/{contextName} > {Filename.zip}
     *
     * Will throw Error 500 if the payload is empty or if there was something wrong in extracting the Context.
     *
     * @param contextName Name of the context to be exported
     * @return Zip file that contains the contents of the exported Context.
     */
    @RequestMapping(method = RequestMethod.GET, path = {"/{contextName}"})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<StreamingResponseBody> getContextExport(@PathVariable String contextName) {

        LOG.info("Start creating export for Context {}", contextName);
        try {
            ByteArrayOutputStream byteArrayOutputStream = ContextExportZipUtils.createZipFile(
                scheduledContextService.findByName(contextName).getContext(),
                System.currentTimeMillis() + "-", // make sure directory is unique due to same request running at same time
                schedulerJobService,
                50);

            // If nothing found throw null pointer exception.
            if (byteArrayOutputStream == null) {
                LOG.error("Zip File extraction return nothing!");
                throw new NullPointerException("Zip File extraction return nothing!");
            }

            // Sanitise unsafe characters for filename returning.
            String contextFileName = StringUtils.replaceEach(contextName,
                ContextExportZipUtils.UNSAFE_FILENAME_CHAR, ContextExportZipUtils.REPLACE_UNSAFE_FILENAME_CHAR);

            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment;filename=" + contextFileName + ".zip")
                .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(outputStream -> {
                    outputStream.write(byteArrayOutputStream.toByteArray());
                    byteArrayOutputStream.close();
                    outputStream.close();
                });

        } catch (Exception e) {
            LOG.error("Something has gone wrong when extracting the Context [{}]", contextName);
            return ResponseEntity
                .internalServerError()
                .header("Content-Disposition", "attachment;filename=error.zip")
                .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(outputStream -> {
                    String errorMsg = "There has been an error generating the file for the context: " + contextName;
                    outputStream.write(errorMsg.getBytes());
                    outputStream.close();
                });
        }
    }
}
