package org.ikasan.dashboard.ui.visualisation.model.flow;

import org.ikasan.dashboard.broadcast.State;
import org.ikasan.dashboard.ui.visualisation.component.FlowOptionsDialog;
import org.ikasan.spec.module.StartupType;


/**
 * Represents a flow with configuration, state, dimensions, and control-related properties.
 * This class is used to define and manage the properties and behavior of a flow during runtime.
 */
public class Flow
{
	private String name;
    private String configurationId;
	private Consumer consumer;
	private State status = State.RUNNING_STATE;
	private StartupType startupType;
    private String startupComment;
    private boolean isRecording;

	// flow border values
	private int x = 0,y = 0,w = 0,h = 0;

	private int controlRelativeX = -137;
	private int controlRelativeY = 0;
	private int controlImageW = 75;
	private int controlImageH = 75;

    /**
     * Constructs a new Flow instance with the provided parameters.
     *
     * @param name the name of the flow
     * @param configurationId the unique identifier for the configuration
     * @param consumer the consumer associated with this flow
     * @param startupType the type of startup for this flow
     * @param startupComment an optional comment regarding the startup of the flow
     */
	public Flow(String name, String configurationId, Consumer consumer, StartupType startupType, String startupComment)
	{
		this.name = name;
		this.configurationId = configurationId;
		this.consumer = consumer;
		this.startupType = startupType;
		this.startupComment = startupComment;
	}

	/**
     * Retrieves the name.
     *
     * @return the name as a string.
     */
    public String getName()
	{
		return name;
	}

    /**
     * Retrieves the unique configuration ID associated with the flow.
     *
     * @return the configuration ID as a string
     */
    public String getConfigurationId()
    {
        return configurationId;
    }

    /**
     * Retrieves the consumer associated with the current flow.
     *
     * @return the {@link Consumer} object associated with this flow.
     */
    public Consumer getConsumer()
	{
		return consumer;
	}

	/**
     * Sets the dimensions and position of the border for this element.
     *
     * @param x the x-coordinate of the top-left corner of the border
     * @param y the y-coordinate of the top-left corner of the border
     * @param w the width of the border
     * @param h the height of the border
     */
    public void setBorder(int x, int y, int w, int h)
    {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /**
     * Retrieves the x-coordinate of the flow.
     *
     * @return the current x-coordinate as an integer.
     */
    public int getX()
    {
        return x;
    }

    /**
     * Retrieves the y-coordinate of the Flow object.
     *
     * @return the y-coordinate as an integer
     */
    public int getY()
    {
        return y;
    }

    /**
     * Retrieves the width of the flow.
     *
     * @return the width value represented by the field 'w'.
     */
    public int getW()
    {
        return w;
    }

    /**
     * Retrieves the height value of the Flow.
     *
     * @return the height of the Flow as an integer
     */
    public int getH()
    {
        return h;
    }

    /**
     * Computes the absolute X coordinate of the control element based on the X coordinate of the flow
     * and the relative X coordinate of the control.
     *
     * @return the absolute X coordinate of the control element
     */
    public int getControlX() {
        return this.getX() + controlRelativeX;
    }

    /**
     * Calculates and returns the Y-coordinate of the control by adding the
     * control's relative Y offset to the current Y-coordinate of the object.
     *
     * @return The Y-coordinate of the control as an integer.
     */
    public int getControlY() {
        return this.getY() + controlRelativeY;
    }

    /**
     * Retrieves the horizontal position of the control relative to its parent container.
     *
     * @return the x-coordinate of the control relative to its parent as an integer.
     */
    public int getControlRelativeX() {
        return controlRelativeX;
    }

    /**
     * Sets the relative X coordinate of the control element within the flow.
     *
     * @param controlRelativeX the X coordinate relative to the flow's position
     */
    public void setControlRelativeX(int controlRelativeX) {
        this.controlRelativeX = controlRelativeX;
    }

    /**
     * Retrieves the relative Y-coordinate of the control element.
     *
     * @return the Y-coordinate relative to the control.
     */
    public int getControlRelativeY() {
        return controlRelativeY;
    }

    /**
     * Sets the relative Y position of the control within the flow.
     *
     * @param controlRelativeY the relative Y-coordinate to be set for the control
     */
    public void setControlRelativeY(int controlRelativeY) {
        this.controlRelativeY = controlRelativeY;
    }

    /**
     * Retrieves the width of the control image.
     *
     * @return the width of the control image as an integer
     */
    public int getControlImageW() {
        return controlImageW;
    }

    /**
     * Sets the width of the control image.
     *
     * @param controlImageW the width of the control image to be set
     */
    public void setControlImageW(int controlImageW) {
        this.controlImageW = controlImageW;
    }

    /**
     * Retrieves the height of the control image associated with this Flow.
     *
     * @return the height of the control image as an integer
     */
    public int getControlImageH() {
        return controlImageH;
    }

    /**
     * Sets the height of the control image.
     *
     * @param controlImageH the height of the control image to set
     */
    public void setControlImageH(int controlImageH) {
        this.controlImageH = controlImageH;
    }

    /**
     * Retrieves the current status of the flow.
     *
     * @return the current {@code State} of the flow, representing its operational status.
     */
    public State getStatus()
    {
        return status;
    }

    /**
     * Updates the status of the flow.
     *
     * @param status the new state to set for the flow
     */
    public void setStatus(State status)
    {
        this.status = status;
    }

    /**
     * Retrieves the current startup type of the flow.
     *
     * @return the startup type associated with the flow.
     */
    public StartupType getStartupType() {
        return startupType;
    }

    /**
     * Sets the startup type for this Flow.
     *
     * @param startupType the {@code StartupType} to be assigned to this Flow
     */
    public void setStartupType(StartupType startupType) {
        this.startupType = startupType;
    }

    /**
     * Retrieves the startup comment associated with the flow instance.
     *
     * @return the startup comment as a string
     */
    public String getStartupComment() {
        return startupComment;
    }

    /**
     * Sets the startup comment for the flow.
     *
     * @param startupComment the startup comment to set
     */
    public void setStartupComment(String startupComment) {
        this.startupComment = startupComment;
    }

    /**
     * Indicates whether the flow is currently in a recording state.
     *
     * @return {@code true} if the flow is recording; {@code false} otherwise.
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Sets the recording status of the flow.
     *
     * @param recording a boolean value indicating whether recording is enabled (true) or disabled (false)
     */
    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer("Flow{");
        sb.append("name='").append(name).append('\'');
        sb.append(", configurationId='").append(configurationId).append('\'');
        sb.append(", consumer=").append(consumer);
        sb.append(", status=").append(status);
        sb.append(", x=").append(x);
        sb.append(", y=").append(y);
        sb.append(", w=").append(w);
        sb.append(", h=").append(h);
        sb.append('}');
        return sb.toString();
    }
}
