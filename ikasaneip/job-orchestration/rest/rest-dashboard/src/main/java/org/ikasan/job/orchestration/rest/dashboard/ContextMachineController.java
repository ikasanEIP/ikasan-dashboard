package org.ikasan.job.orchestration.rest.dashboard;

import org.ikasan.job.orchestration.broadcast.ContextInstanceSavedEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.rest.dashboard.model.dto.*;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/rest/contextMachine")
public class ContextMachineController {

    private static final Logger LOG = LoggerFactory.getLogger(ContextMachineController.class);

    @PutMapping("/{contextInstanceId}/holdAllJobs")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity holdAllJobs(@PathVariable("contextInstanceId") String contextInstanceId,
                                      @RequestBody ContextMachineJobActionDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            LOG.debug("holdJobs: contextInstanceId [{}] not in local cache", contextInstanceId);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            contextMachine.holdJobs(dto.getChildContextName());
            ContextInstanceSavedEventBroadcaster.broadcast(contextMachine.getContext());
            LOG.info("holdJobs: contextInstanceId [{}], childContext [{}]", contextInstanceId, dto.getChildContextName());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("holdJobs failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/releaseAllJobs")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity releaseAllJobs(@PathVariable("contextInstanceId") String contextInstanceId,
                                         @RequestBody ContextMachineJobActionDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            LOG.debug("releaseJobs: contextInstanceId [{}] not in local cache", contextInstanceId);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            contextMachine.releaseJobs(dto.getChildContextName());
            ContextInstanceSavedEventBroadcaster.broadcast(contextMachine.getContext());
            LOG.info("releaseJobs: contextInstanceId [{}], childContext [{}]", contextInstanceId, dto.getChildContextName());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("releaseJobs failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/skipAllJobs")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity skipAllJobs(@PathVariable("contextInstanceId") String contextInstanceId,
                                      @RequestBody ContextMachineJobActionDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            LOG.debug("skipAllJobs: contextInstanceId [{}] not in local cache", contextInstanceId);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            contextMachine.skipJobs(dto.getChildContextName(), dto.isSkipFlag());
            ContextInstanceSavedEventBroadcaster.broadcast(contextMachine.getContext());
            LOG.info("skipAllJobs: contextInstanceId [{}], childContext [{}], skip [{}]",
                contextInstanceId, dto.getChildContextName(), dto.isSkipFlag());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("skipAllJobs failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{contextInstanceId}/context")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<?> getContext(@PathVariable("contextInstanceId") String contextInstanceId) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(contextInstanceId)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        ContextMachine contextMachine = ContextMachineCache.instance().getLocalByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(contextMachine.getContext(), HttpStatus.OK);
    }

    @GetMapping("/{contextInstanceId}/context/{contextName}")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<?> getChildContext(@PathVariable("contextInstanceId") String contextInstanceId,
                                             @PathVariable("contextName") String contextName) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(contextInstanceId)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        ContextMachine contextMachine = ContextMachineCache.instance().getLocalByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(contextMachine.getContext(contextName), HttpStatus.OK);
    }

    @GetMapping("/{contextInstanceId}/contextStatus/{contextName}")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<?> getContextStatus(@PathVariable("contextInstanceId") String contextInstanceId,
                                              @PathVariable("contextName") String contextName) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(contextInstanceId)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        ContextMachine contextMachine = ContextMachineCache.instance().getLocalByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(contextMachine.getContextStatus(contextName), HttpStatus.OK);
    }

    @GetMapping("/{contextInstanceId}/jobStatus/{contextName}/{jobIdentifier}")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<?> getJobStatus(@PathVariable("contextInstanceId") String contextInstanceId,
                                          @PathVariable("contextName") String contextName,
                                          @PathVariable("jobIdentifier") String jobIdentifier) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(contextInstanceId)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        ContextMachine contextMachine = ContextMachineCache.instance().getLocalByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(contextMachine.getJobStatus(contextName, jobIdentifier), HttpStatus.OK);
    }

    @GetMapping("/{contextInstanceId}/instanceStatus")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<?> getContextInstanceStatus(@PathVariable("contextInstanceId") String contextInstanceId) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(contextInstanceId)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        ContextMachine contextMachine = ContextMachineCache.instance().getLocalByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(contextMachine.getContextInstanceStatus(), HttpStatus.OK);
    }

    @GetMapping("/{contextInstanceId}/isDryRun")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<?> isDryRun(@PathVariable("contextInstanceId") String contextInstanceId) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(contextInstanceId)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        ContextMachine contextMachine = ContextMachineCache.instance().getLocalByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(contextMachine.isDryRun(), HttpStatus.OK);
    }

    @PutMapping("/{contextInstanceId}/disableQuartzJobs")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity disableQuartzJobs(@PathVariable("contextInstanceId") String contextInstanceId) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            contextMachine.disableQuartzBasedJobs();
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("disableQuartzJobs failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/enableQuartzJobs")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity enableQuartzJobs(@PathVariable("contextInstanceId") String contextInstanceId) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            contextMachine.enableQuartzBasedJobs();
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("enableQuartzJobs failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/runUntilManuallyEnded")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity runUntilManuallyEnded(@PathVariable("contextInstanceId") String contextInstanceId) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            contextMachine.runContextUntilManuallyEnded();
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("runUntilManuallyEnded failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/releaseQueuedJobs")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity releaseQueuedJobs(@PathVariable("contextInstanceId") String contextInstanceId) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            contextMachine.releaseQueuedJobs();
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("releaseQueuedJobs failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/killRunningJobs")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity killRunningJobs(@PathVariable("contextInstanceId") String contextInstanceId) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            contextMachine.killRunningJobs();
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("killRunningJobs failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/saveContext")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity saveContext(@PathVariable("contextInstanceId") String contextInstanceId) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            contextMachine.saveContext();
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("saveContext failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/updateContextParameters")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity updateContextParameters(@PathVariable("contextInstanceId") String contextInstanceId,
                                                  @RequestBody List<ContextParameterInstanceImpl> contextParameters) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(contextInstanceId)) return new ResponseEntity(HttpStatus.NOT_FOUND);
        ContextMachine contextMachine = ContextMachineCache.instance().getLocalByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            contextMachine.updateContextParameters(new java.util.ArrayList<>(contextParameters));
            ContextInstanceSavedEventBroadcaster.broadcast(contextMachine.getContext());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("updateContextParameters failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/dryRunParameters")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity setDryRunParameters(@PathVariable("contextInstanceId") String contextInstanceId,
                                              @RequestBody org.ikasan.job.orchestration.model.event.DryRunParametersImpl dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            contextMachine.setDryRunParameters(dto);
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("setDryRunParameters failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/broadcastGlobalEvents")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity broadcastGlobalEvents(@PathVariable("contextInstanceId") String contextInstanceId,
                                                @RequestBody BroadcastGlobalEventsDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            SchedulerJobInitiationEventImpl event = new SchedulerJobInitiationEventImpl();
            event.setAgentName(dto.getAgentName());
            event.setJobName(dto.getJobName());
            event.setContextName(dto.getContextName());
            event.setContextInstanceId(dto.getContextInstanceId());
            event.setChildContextNames(dto.getChildContextNames());
            contextMachine.broadcastGlobalEvents(event, dto.isIgnoreEnvironmentGroup(), dto.isForceSending());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("broadcastGlobalEvents failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/publishJobInitiationEvent")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity publishJobInitiationEvent(@PathVariable("contextInstanceId") String contextInstanceId,
                                                    @RequestBody BroadcastLocalEventDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            SchedulerJobInitiationEventImpl event = new SchedulerJobInitiationEventImpl();
            event.setAgentName(dto.getAgentName());
            event.setJobName(dto.getJobName());
            event.setContextName(dto.getContextName());
            event.setContextInstanceId(dto.getContextInstanceId());
            event.setChildContextNames(dto.getChildContextNames());
            contextMachine.publishJobInitiationEvent(event);
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("publishJobInitiationEvent failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/addQueuedInitiationEvent")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity addQueuedInitiationEvent(@PathVariable("contextInstanceId") String contextInstanceId,
                                                   @RequestBody BroadcastLocalEventDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            SchedulerJobInitiationEventImpl event = new SchedulerJobInitiationEventImpl();
            event.setAgentName(dto.getAgentName());
            event.setJobName(dto.getJobName());
            event.setContextName(dto.getContextName());
            event.setContextInstanceId(dto.getContextInstanceId());
            event.setChildContextNames(dto.getChildContextNames());
            contextMachine.addQueuedSchedulerJobInitiationEvent(event);
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("addQueuedInitiationEvent failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/resubmitDlq")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity resubmitDlq(@PathVariable("contextInstanceId") String contextInstanceId,
                                      @RequestBody MessageIdDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            boolean result = contextMachine.resubmitMessageFromDeadLetterQueue(dto.getMessageId());
            return result ? new ResponseEntity(HttpStatus.OK) : new ResponseEntity(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            LOG.error("resubmitDlq failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/acknowledgeError")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity acknowledgeError(@PathVariable("contextInstanceId") String contextInstanceId,
                                           @RequestBody AcknowledgeJobDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            LOG.debug("acknowledgeError: contextInstanceId [{}] not in local cache", contextInstanceId);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            InternalEventDrivenJobInstanceImpl instance = new InternalEventDrivenJobInstanceImpl();
            instance.setIdentifier(dto.getIdentifier());
            instance.setTargetResidingContextOnly(dto.isTargetResidingContextOnly());
            instance.setChildContextName(dto.getChildContextName());
            instance.setChildContextNames(dto.getChildContextNames());
            contextMachine.acknowledgeSchedulerJobError(instance);
            ContextInstanceSavedEventBroadcaster.broadcast(contextMachine.getContext());
            LOG.info("acknowledgeError: contextInstanceId [{}], job [{}]", contextInstanceId, dto.getIdentifier());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("acknowledgeError failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/broadcastLocalEvent")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity broadcastLocalEvent(@PathVariable("contextInstanceId") String contextInstanceId,
                                              @RequestBody BroadcastLocalEventDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            LOG.debug("broadcastLocalEvent: contextInstanceId [{}] not in local cache", contextInstanceId);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            SchedulerJobInitiationEventImpl event = new SchedulerJobInitiationEventImpl();
            event.setAgentName(dto.getAgentName());
            event.setJobName(dto.getJobName());
            event.setContextName(dto.getContextName());
            event.setContextInstanceId(dto.getContextInstanceId());
            event.setChildContextNames(dto.getChildContextNames());
            contextMachine.broadcastLocalEvent(event);
            LOG.info("broadcastLocalEvent: contextInstanceId [{}], job [{}]", contextInstanceId, dto.getJobName());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("broadcastLocalEvent failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/hold")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity holdJob(@PathVariable("contextInstanceId") String contextInstanceId,
                                  @RequestBody ContextMachineJobActionDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            LOG.debug("holdJob: contextInstanceId [{}] not in local cache", contextInstanceId);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            contextMachine.holdJob(dto.getJobIdentifier(), dto.getChildContextName());
            LOG.info("holdJob: contextInstanceId [{}], job [{}], childContext [{}]",
                contextInstanceId, dto.getJobIdentifier(), dto.getChildContextName());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("holdJob failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/release")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity releaseJob(@PathVariable("contextInstanceId") String contextInstanceId,
                                     @RequestBody ContextMachineJobActionDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            LOG.debug("releaseJob: contextInstanceId [{}] not in local cache", contextInstanceId);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            contextMachine.releaseJob(dto.getJobIdentifier(), dto.getChildContextName());
            LOG.info("releaseJob: contextInstanceId [{}], job [{}], childContext [{}]",
                contextInstanceId, dto.getJobIdentifier(), dto.getChildContextName());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("releaseJob failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/reset")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity resetJob(@PathVariable("contextInstanceId") String contextInstanceId,
                                   @RequestBody ContextMachineJobActionDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            LOG.debug("resetJob: contextInstanceId [{}] not in local cache", contextInstanceId);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            contextMachine.resetJob(dto.getJobIdentifier(), dto.getChildContextName());
            LOG.info("resetJob: contextInstanceId [{}], job [{}], childContext [{}]",
                contextInstanceId, dto.getJobIdentifier(), dto.getChildContextName());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("resetJob failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{contextInstanceId}/skip")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity skipJob(@PathVariable("contextInstanceId") String contextInstanceId,
                                  @RequestBody ContextMachineJobActionDto dto) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            LOG.debug("skipJob: contextInstanceId [{}] not in local cache", contextInstanceId);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            contextMachine.skipJob(dto.getJobIdentifier(), dto.getChildContextName(), dto.isSkipFlag());
            LOG.info("skipJob: contextInstanceId [{}], job [{}], childContext [{}], skip [{}]",
                contextInstanceId, dto.getJobIdentifier(), dto.getChildContextName(), dto.isSkipFlag());
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("skipJob failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{contextInstanceId}/dlqMessages")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<?> getDlqMessages(@PathVariable("contextInstanceId") String contextInstanceId) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(contextInstanceId)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        ContextMachine contextMachine = ContextMachineCache.instance().getLocalByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        List<BigQueueMessage> messages = contextMachine.getDlqMessages();
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    @DeleteMapping("/{contextInstanceId}/dlqMessages/{messageId}")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity deleteDlqMessage(@PathVariable("contextInstanceId") String contextInstanceId,
                                           @PathVariable("messageId") String messageId) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(contextInstanceId)) return new ResponseEntity(HttpStatus.NOT_FOUND);
        ContextMachine contextMachine = ContextMachineCache.instance().getLocalByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            boolean found = contextMachine.deleteDlqMessage(messageId);
            return found ? new ResponseEntity(HttpStatus.OK) : new ResponseEntity(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            LOG.error("deleteDlqMessage failed for contextInstanceId [{}], messageId [{}]", contextInstanceId, messageId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{contextInstanceId}/dlqMessages")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity deleteAllDlqMessages(@PathVariable("contextInstanceId") String contextInstanceId) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(contextInstanceId)) return new ResponseEntity(HttpStatus.NOT_FOUND);
        ContextMachine contextMachine = ContextMachineCache.instance().getLocalByContextInstanceId(contextInstanceId);
        if (contextMachine == null) return new ResponseEntity(HttpStatus.NOT_FOUND);
        try {
            contextMachine.deleteAllDlqMessages();
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("deleteAllDlqMessages failed for contextInstanceId [{}]", contextInstanceId, e);
            return new ResponseEntity(new ErrorDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
