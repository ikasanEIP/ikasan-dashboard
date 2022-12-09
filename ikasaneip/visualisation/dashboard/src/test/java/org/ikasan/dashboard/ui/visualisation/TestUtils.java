package org.ikasan.dashboard.ui.visualisation;

import java.util.HashMap;
import java.util.Map;

public class TestUtils {

    public static Map<String, String> getSchedulerJobExecutionEnvironmentLabel() {
        Map<String, String> values = new HashMap<>();
        values.put("CMD", "cmd.exe|/c");
        values.put("BASH", "/bin/bash|-c");
        values.put("POWERSHELL", "powershell.exe|-Command");
        return values;
    }
}
