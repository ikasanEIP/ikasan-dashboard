package org.ikasan.dashboard.ui.scheduler.listener;

public interface JobPlanSaveRequiredListener {

    /**
     * Notifies listeners that a save action is required for the job plan.
     *
     * @param isSavedRequired a boolean indicating whether a save action is required.
     *                        {@code true} if a save is required; {@code false} otherwise.
     */
    void jobPlanSaveRequired(boolean isSavedRequired);
}
