package org.ikasan.dashboard.ui.visualisation.scheduler.util;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

import java.util.HashMap;

public class StatusColours {
    public static final String WAITING = "rgba(210, 215, 211, 1.0)";
    public static final String RUNNING = "rgba(133,181,225, 1.0)";
    public static final String ERROR = "rgba(239, 83, 80, 1.0)";
    public static final String COMPLETE = "rgba(102, 187, 106, 1.0)";

    private static HashMap<InstanceStatus, String> STATUS_COLOURS = new HashMap<>();

    static {
        STATUS_COLOURS.put(InstanceStatus.COMPLETE, COMPLETE);
        STATUS_COLOURS.put(InstanceStatus.ERROR, ERROR);
        STATUS_COLOURS.put(InstanceStatus.RUNNING, RUNNING);
        STATUS_COLOURS.put(InstanceStatus.WAITING, WAITING);
    }

    public static String getInstanceStatusColour(InstanceStatus status) {
        return STATUS_COLOURS.get(status);
    }
}
