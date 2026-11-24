package org.ikasan.dashboard.ui.scheduler.component;

import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;

/**
 * Records who submitted a job by hand ("Manually Submitted By").
 * <p>
 * Call it <em>before</em> the job is sent. Once the job is sent, its agent reports it starting (and maybe completing)
 * within milliseconds, and the context machine saves the job's record for each report. A record saved by the UI
 * after that, from the copy the grid or dialog holds, would either be overwritten without the submitter or put an
 * out-of-date status back. The context machine keeps the field when it updates the record.
 */
final class ManualJobSubmission {

    private ManualJobSubmission() {
    }

    /**
     * Set the submitter on the job's current record, save it, and keep the caller's copy in step.
     *
     * @param service the scheduler job instance service
     * @param record  the caller's copy of the job's record (from a grid or dialog; may be out of date)
     * @param user    who submitted the job
     */
    static void recordSubmitter(SchedulerJobInstanceService service, SchedulerJobInstanceRecord record, String user) {
        SchedulerJobInstanceRecord current = service.findByContextIdJobNameChildContextName(record.getContextInstanceId(),
            record.getJobName(), record.getChildContextName());
        if (current == null) {
            current = record;
        }
        current.setManuallySubmittedBy(user);
        service.save(current);
        record.setManuallySubmittedBy(user);
    }
}
