package org.ikasan.rest.dashboard;

import org.ikasan.rest.dashboard.model.dto.BigQueueModuleDto;
import org.ikasan.rest.dashboard.model.dto.MetadataModuleDto;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.module.client.BigQueueModuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Rest Controller that allows access to administer the Ikasan Modules Big Queue
 * @author Ikasan Development Team
 */
@RequestMapping("/rest/module/bigQueue")
@RestController
public class BigQueueModuleController {

    private static final Logger LOG = LoggerFactory.getLogger(BigQueueModuleController.class);

    private BigQueueModuleService bigQueueModuleService;
    private ModuleMetaDataService moduleMetaDataService;

    public BigQueueModuleController(BigQueueModuleService bigQueueModuleService, ModuleMetaDataService moduleMetaDataService) {
        this.bigQueueModuleService = bigQueueModuleService;
        this.moduleMetaDataService = moduleMetaDataService;
    }

    /**
     * Helper method to get all the modules that is known to the dashboard
     * @return List of MetadataModuleDto
     */
    public List<MetadataModuleDto> getModules() {
        return moduleMetaDataService.findAll().stream().map(
            moduleMetaData -> new MetadataModuleDto(moduleMetaData.getName(), moduleMetaData.getUrl(), moduleMetaData.getType(),
                moduleMetaData.getFlows().stream().map(FlowMetaData::getName).collect(Collectors.toList())
            )).collect(Collectors.toList());
    }

    /**
     * Get all the Big Queue that is setup for a given module
     * /rest/module/bigQueue/{moduleName}
     * @param moduleName name of the module
     * @return list of queues for the module
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{moduleName}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity getQueues(@PathVariable("moduleName") String moduleName) {

        try {
            List<MetadataModuleDto> metadataModuleDtoList = getModules();
            MetadataModuleDto metadataModuleDto = metadataModuleDtoList.stream().filter(dto -> moduleName.equals(dto.getName())).findAny().orElse(null);

            if (metadataModuleDto != null) {
                String url = metadataModuleDto.getUrl();
                return new ResponseEntity(bigQueueModuleService.listQueues(url), HttpStatus.OK);
            } else {
                throw new NullPointerException("The module was not found in the Ikasan Dashboard");
            }
        } catch (Exception e) {
            String message = String.format("Got exception trying to list queues for the module [%s]. Error [%s]", moduleName, e.getMessage());
            LOG.warn(message);
            return new ResponseEntity(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get the size of a given queue that is setup for a given module
     * /rest/module/bigQueue/size/{queueName}/{moduleName}
     * @param moduleName name of the module
     * @param queueName name of the queue we looking to check the size for
     * @return the number of messages on the queue
     */
    @RequestMapping(method = RequestMethod.GET, value = "/size/{queueName}/{moduleName}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity getQueueSize(@PathVariable("moduleName") String moduleName, @PathVariable("queueName") String queueName) {

        try {
            List<MetadataModuleDto> metadataModuleDtoList = getModules();
            MetadataModuleDto metadataModuleDto = metadataModuleDtoList.stream().filter(dto -> moduleName.equals(dto.getName())).findAny().orElse(null);

            if (metadataModuleDto != null) {
                String url = metadataModuleDto.getUrl();
                return new ResponseEntity(bigQueueModuleService.size(url, queueName), HttpStatus.OK);
            } else {
                throw new NullPointerException("The module was not found in the Ikasan Dashboard");
            }
        } catch (Exception e) {
            String message = String.format("Got exception trying to get size of queue [%s] for the module [%s]. Error [%s]", queueName, moduleName, e.getMessage());
            LOG.warn(message);
            return new ResponseEntity(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get the size of all the queues that is setup for a given module
     * /rest/module/bigQueue/size/module/{includeZeros}/{moduleName}
     *
     * @param moduleName name of the module
     * @param includeZeros Set to false to only return back queues that has a queue depth greater than 0. Set to true to return all queue depth even if it is 0.
     * @return a map of queue and sizes in a json for example:
     *   {
     *      "big-queue-name-1": 10,
     *      "big-queue-name-2": 0
     *   }
     */
    @RequestMapping(method = RequestMethod.GET, value = "/size/module/{includeZeros}/{moduleName}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity getAllQueueSize(@PathVariable("moduleName") String moduleName, @PathVariable("includeZeros") boolean includeZeros) {

        try {
            List<MetadataModuleDto> metadataModuleDtoList = getModules();
            MetadataModuleDto metadataModuleDto = metadataModuleDtoList.stream().filter(dto -> moduleName.equals(dto.getName())).findAny().orElse(null);

            if (metadataModuleDto != null) {
                String url = metadataModuleDto.getUrl();
                return new ResponseEntity(bigQueueModuleService.size(url, includeZeros), HttpStatus.OK);
            } else {
                throw new NullPointerException("The module was not found in the Ikasan Dashboard");
            }
        } catch (Exception e) {
            String message = String.format("Got exception trying to get all sizes of the queues for the module [%s] with setting includeZeros = [%s]. Error [%s]",
                moduleName, includeZeros, e.getMessage());
            LOG.warn(message);
            return new ResponseEntity(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get the size of all the queues that is setup for all modules based on a moduleType
     * /rest/module/bigQueue/size/all/{includeZeros}/{moduleType}
     *
     * @param includeZeros Set to false to only return back queues that has a queue depth greater than 0.
     *                     Set to true to return all queue depth even if it is 0.
     * @param moduleType Allowed values are:
     *                   SCHEDULER_AGENT
     *                   INTEGRATION_MODULE
     *                   ALL
     * @return a List of BigQueueModuleDto object, represented in JSON.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/size/all/{includeZeros}/{moduleType}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity getAllModuleQueueSize(@PathVariable("includeZeros") boolean includeZeros, @PathVariable("moduleType") String moduleType) {

        ModuleType moduleEnum = null;
        switch (moduleType) {
            case "INTEGRATION_MODULE":
                moduleEnum = ModuleType.INTEGRATION_MODULE;
                break;
            case "SCHEDULER_AGENT":
                moduleEnum = ModuleType.SCHEDULER_AGENT;
                break;
            case "ALL":
                break;
            default:
                String errorMessage = "Module Type must one of, [INTEGRATION_MODULE], [SCHEDULER_AGENT] or [ALL]";
                LOG.warn(errorMessage);
                return new ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST);
        }

        List<BigQueueModuleDto> bigQueueModuleDtoList = new ArrayList<>();
        List<MetadataModuleDto> metadataModuleDtoList = getModules();
        // For Each Module register to the ikasan dashboard, get the biq queue with it size
        for (MetadataModuleDto module : metadataModuleDtoList) {
            if (moduleType.equals("ALL") || module.getModuleType() == moduleEnum) {
                try {
                    LOG.info("Getting all queue sizes for the module [{}]", module.getName());
                    Map<String, Long> queueMap = bigQueueModuleService.size(module.getUrl(), includeZeros);
                    if (queueMap.size() > 0) {
                        bigQueueModuleDtoList.add(new BigQueueModuleDto(module.getName(), queueMap, true));
                    }
                } catch (Exception e1) {
                    // May get errors due to the module not supporting BigQueue
                    LOG.warn("Unable to get Big Queue details for module [{}], with error [{}]", module.getName(), e1.getMessage());
                    bigQueueModuleDtoList.add(new BigQueueModuleDto(module.getName(), new HashMap<>(), false));
                }
            }
        }
        return new ResponseEntity(bigQueueModuleDtoList, HttpStatus.OK);
    }

    /**
     * Get the first message from the given queue
     * /rest/module/bigQueue/peek/{queueName}/{moduleName}
     *
     * @param moduleName name of the module
     * @param queueName name of queue to peek
     * @return BigQueueMessage in Json.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/peek/{queueName}/{moduleName}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity peek(@PathVariable("moduleName") String moduleName, @PathVariable("queueName") String queueName) {

        try {
            List<MetadataModuleDto> metadataModuleDtoList = getModules();
            MetadataModuleDto metadataModuleDto = metadataModuleDtoList.stream().filter(dto -> moduleName.equals(dto.getName())).findAny().orElse(null);

            if (metadataModuleDto != null) {
                String url = metadataModuleDto.getUrl();
                return new ResponseEntity(bigQueueModuleService.peek(url, queueName), HttpStatus.OK);
            } else {
                throw new NullPointerException("The module was not found in the Ikasan Dashboard");
            }
        } catch (Exception e) {
            String message = String.format("Got exception trying to get the first message from the queue [%s] for the module [%s]. Error [%s]",
                queueName, moduleName, e.getMessage());
            LOG.warn(message);
            return new ResponseEntity(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get all the messages from the given queue
     * /rest/module/bigQueue/messages/{queueName}/{moduleName}
     *
     * @param moduleName name of the module
     * @param queueName name of queue to get all messages from
     * @return List of BigQueueMessage in Json.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/messages/{queueName}/{moduleName}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity getMessages(@PathVariable("moduleName") String moduleName, @PathVariable("queueName") String queueName) {

        try {
            List<MetadataModuleDto> metadataModuleDtoList = getModules();
            MetadataModuleDto metadataModuleDto = metadataModuleDtoList.stream().filter(dto -> moduleName.equals(dto.getName())).findAny().orElse(null);

            if (metadataModuleDto != null) {
                String url = metadataModuleDto.getUrl();
                return new ResponseEntity(bigQueueModuleService.getMessages(url, queueName), HttpStatus.OK);
            } else {
                throw new NullPointerException("The module was not found in the Ikasan Dashboard");
            }
        } catch (Exception e) {
            String message = String.format("Got exception trying to get all messages from the queue [%s] for the module [%s]. Error [%s]",
                queueName, moduleName, e.getMessage());
            LOG.warn(message);
            return new ResponseEntity(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Delete a message from the queue by a given messageId
     * /rest/module/bigQueue/delete/{queueName}/{messageId}/{moduleName}
     *
     * @param moduleName name of the module
     * @param queueName name of queue to delete a message from
     * @param messageId if of the message to delete
     * @return true if it was success, false if there was something wrong
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/delete/{queueName}/{messageId}/{moduleName}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity deleteMessage(@PathVariable("moduleName") String moduleName, @PathVariable("queueName") String queueName, @PathVariable("messageId") String messageId) {

        try {
            List<MetadataModuleDto> metadataModuleDtoList = getModules();
            MetadataModuleDto metadataModuleDto = metadataModuleDtoList.stream().filter(dto -> moduleName.equals(dto.getName())).findAny().orElse(null);

            if (metadataModuleDto != null) {
                String url = metadataModuleDto.getUrl();
                return new ResponseEntity(bigQueueModuleService.deleteMessage(url, queueName, messageId), HttpStatus.OK);
            } else {
                throw new NullPointerException("The module was not found in the Ikasan Dashboard");
            }
        } catch (Exception e) {
            String message = String.format("Got exception trying to delete the messagesId [%s] from the queue [%s] for the module [%s]. Error [%s]",
                messageId, queueName, moduleName, e.getMessage());
            LOG.warn(message);
            return new ResponseEntity(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Delete all messages from the queue
     * /rest/module/bigQueue/delete/allMessages/{queueName}/{moduleName}
     *
     * @param moduleName name of the module
     * @param queueName name of queue to delete a message from
     * @return true if it was success, false if there was something wrong
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/delete/allMessages/{queueName}/{moduleName}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity deleteAllMessages(@PathVariable("moduleName") String moduleName, @PathVariable("queueName") String queueName) {

        try {
            List<MetadataModuleDto> metadataModuleDtoList = getModules();
            MetadataModuleDto metadataModuleDto = metadataModuleDtoList.stream().filter(dto -> moduleName.equals(dto.getName())).findAny().orElse(null);

            if (metadataModuleDto != null) {
                String url = metadataModuleDto.getUrl();
                return new ResponseEntity(bigQueueModuleService.deleteAllMessage(url, queueName), HttpStatus.OK);
            } else {
                throw new NullPointerException("The module was not found in the Ikasan Dashboard");
            }
        } catch (Exception e) {
            String message = String.format("Got exception trying to delete all messages from the queue [%s] for the module [%s]. Error [%s]",
                queueName, moduleName, e.getMessage());
            LOG.warn(message);
            return new ResponseEntity(message, HttpStatus.BAD_REQUEST);
        }
    }
}
