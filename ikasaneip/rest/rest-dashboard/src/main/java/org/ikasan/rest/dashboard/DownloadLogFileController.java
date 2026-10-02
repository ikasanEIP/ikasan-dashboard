package org.ikasan.rest.dashboard;

import org.ikasan.esb.service.support.DirectoryZipUtil;
import org.ikasan.esb.service.support.LogFileDownloadAndZipUtil;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.nio.file.Path;

@RequestMapping("/rest/logs")
@RestController
public class DownloadLogFileController {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadLogFileController.class);

    @Autowired
    @Qualifier("moduleMetadataEntityService")
    ModuleMetaDataService moduleMetaDataService;

    @Value("${solr.install.dir:}")
    String solrInstallDirectory;

    @Value("${dashboard.log.dir:}")
    String dashboardLogDirectory;

    @Value("${rest.module.username}")
    String restUserName;

    @Value("${rest.module.password}")
    String restPassword;

    @Value("${max.download.bytes:50000000}")
    private int maxDownloadBytes;

    @RequestMapping(method = RequestMethod.GET, path = {"/dashboard/zip"}, produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<StreamingResponseBody> dashboardZip() {
        Path path;
        if(this.dashboardLogDirectory == null || this.dashboardLogDirectory.isEmpty()) {
            path = Path.of(System.getProperty("user.dir"), "logs");
        }
        else {
            path = Path.of(this.dashboardLogDirectory);
        }

        try {
            if(new File(path.toUri()).exists()) {
                StreamingResponseBody body = outputStream ->
                    DirectoryZipUtil.zipDirectory(path.toString(), outputStream);

                return ResponseEntity
                    .ok()
                    .header("Content-Disposition", "attachment;filename=dashboardLogs"
                        + System.currentTimeMillis() + ".zip")
                    .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(body);
            } else {
                String errorMsg = "Not able to download the zipped dashboard logs for [" + path + "]. The directory does not exist!";

                LOG.error("Something has gone wrong when trying to download the zipped log directory [{}], error[{}]!"
                    , path, errorMsg);
                return ResponseEntity
                    .internalServerError()
                    .header("Content-Disposition", "attachment;filename=dashboardLogs"
                        + System.currentTimeMillis() + ".zip")
                    .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(outputStream -> {
                        outputStream.write(errorMsg.getBytes());
                        outputStream.close();
                    });
            }
        } catch (Exception e) {
            LOG.error("A general error has occurred when trying to download the zipped log directory [{}]", path, e);
            return ResponseEntity
                .internalServerError()
                .header("Content-Disposition", "attachment;filename=error.txt")
                .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(outputStream -> {
                    String errorMsg = "Something has gone wrong when trying to download the zipped log directory [" + path + "]";
                    outputStream.write(errorMsg.getBytes());
                    outputStream.close();
                });
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = {"/module/zip"}, produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<StreamingResponseBody> moduleZip(@RequestParam("moduleName") String moduleName) {
        try {
            ModuleMetaData moduleMetaData = this.moduleMetaDataService.findById(moduleName);

            if(moduleMetaData != null) {
                LogFileDownloadAndZipUtil logFileDownloadAndZipUtil
                    = new LogFileDownloadAndZipUtil();

                StreamingResponseBody body = outputStream ->
                    logFileDownloadAndZipUtil.downloadAndZipLogFiles(moduleMetaData.getUrl(), this.maxDownloadBytes
                        , outputStream, restUserName, restPassword);

                return ResponseEntity
                    .ok()
                    .header("Content-Disposition", "attachment;filename=dashboardLogs"
                        + System.currentTimeMillis() + ".zip")
                    .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(body);
            } else {
                String errorMsg = "Not able to download the zipped modules logs for [" + moduleName + "]. " +
                    "The module meta data does not exist in the database!";

                LOG.error(errorMsg);
                return ResponseEntity
                    .internalServerError()
                    .header("Content-Disposition", "attachment;filename=dashboardLogs"
                        + System.currentTimeMillis() + ".zip")
                    .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(outputStream -> {
                        outputStream.write(errorMsg.getBytes());
                        outputStream.close();
                    });
            }
        } catch (Exception e) {
            LOG.error("A general error has occurred when trying to download the zipped module logs for [{}]", moduleName, e);
            return ResponseEntity
                .internalServerError()
                .header("Content-Disposition", "attachment;filename=error.txt")
                .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(outputStream -> {
                    String errorMsg = "Something has gone wrong when trying to download the zipped module logs for [" + moduleName + "]";
                    outputStream.write(errorMsg.getBytes());
                    outputStream.close();
                });
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = {"/solr/zip"}, produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<StreamingResponseBody> solrZip() {
        Path path;
        if(this.solrInstallDirectory == null || this.solrInstallDirectory.isEmpty()) {
            path = Path.of(System.getProperty("user.dir"), "solr", "server", "logs");
        }
        else {
            path = Path.of(this.solrInstallDirectory, "server", "logs");
        }

        try {
            if(new File(path.toUri()).exists()) {
                StreamingResponseBody body = outputStream ->
                    DirectoryZipUtil.zipDirectory(path.toString(), outputStream);

                return ResponseEntity
                    .ok()
                    .header("Content-Disposition", "attachment;filename=solrLogs-"
                        + System.currentTimeMillis() + ".zip")
                    .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(body);
            } else {
                String errorMsg = "Not able to download the zipped solr logs for [" + path + "]. The directory does not exist!";

                LOG.error("Something has gone wrong when trying to download the zipped solr log directory [{}], error[{}]!"
                    , path, errorMsg);
                return ResponseEntity
                    .internalServerError()
                    .header("Content-Disposition", "attachment;filename=dashboardLogs"
                        + System.currentTimeMillis() + ".zip")
                    .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(outputStream -> {
                        outputStream.write(errorMsg.getBytes());
                        outputStream.close();
                    });
            }
        } catch (Exception e) {
            LOG.error("A general error has occurred when trying to download the zipped solr log directory [{}]", path, e);
            return ResponseEntity
                .internalServerError()
                .header("Content-Disposition", "attachment;filename=error.txt")
                .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(outputStream -> {
                    String errorMsg = "Something has gone wrong when trying to download the zipped log directory [" + path + "]";
                    outputStream.write(errorMsg.getBytes());
                    outputStream.close();
                });
        }
    }

}
