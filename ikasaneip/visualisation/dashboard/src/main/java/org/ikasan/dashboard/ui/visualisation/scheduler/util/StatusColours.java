package org.ikasan.dashboard.ui.visualisation.scheduler.util;

import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

import java.util.HashMap;

public class StatusColours {

    private static HashMap<InstanceStatus, String> STATUS_COLOURS = new HashMap<>();

    static {
        STATUS_COLOURS.put(InstanceStatus.COMPLETE, IkasanColours.SCHEDULER_COMPLETE);
        STATUS_COLOURS.put(InstanceStatus.ERROR, IkasanColours.SCHEDULER_ERROR);
        STATUS_COLOURS.put(InstanceStatus.RUNNING, IkasanColours.SCHEDULER_RUNNING);
        STATUS_COLOURS.put(InstanceStatus.WAITING, IkasanColours.SCHEDULER_WAITING);
        STATUS_COLOURS.put(InstanceStatus.ON_HOLD, IkasanColours.SCHEDULER_ON_HOLD);
        STATUS_COLOURS.put(InstanceStatus.SKIPPED, IkasanColours.SCHEDULER_SKIPPED);
        STATUS_COLOURS.put(InstanceStatus.SKIPPED_RUNNING, IkasanColours.SCHEDULER_SKIPPED);
        STATUS_COLOURS.put(InstanceStatus.SKIPPED_COMPLETE, IkasanColours.SCHEDULER_SKIPPED);
        STATUS_COLOURS.put(InstanceStatus.RELEASED, IkasanColours.SCHEDULER_RELEASED);
    }

    public static String getInstanceStatusColour(InstanceStatus status) {
        return STATUS_COLOURS.get(status);
    }
}
