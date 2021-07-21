package org.ikasan.rest.client.dto;

import java.io.Serializable;

public class ModuleActivationDto implements Serializable {
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
