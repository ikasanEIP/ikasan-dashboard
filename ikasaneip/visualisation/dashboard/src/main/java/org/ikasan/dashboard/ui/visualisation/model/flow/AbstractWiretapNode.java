package org.ikasan.dashboard.ui.visualisation.model.flow;

import org.ikasan.designer.pallet.DesignerItemIdentifier;
import org.ikasan.spec.metadata.model.DecoratorMetaData;

import java.util.List;

/**
 * An abstract class representing a wiretap node that extends the functionality of a Node.
 * This node includes visual and status properties for wiretaps and log wiretaps
 * before and after a specific process or action.
 *
 * The class provides mechanisms to configure and check "clickable" areas for wiretap
 * and log wiretap features. It also stores metadata relevant to decorators.
 */
public abstract class AbstractWiretapNode extends Node {
    public static final String WIRETAP_IMAGE = "frontend/images/wiretap.png";
    public static final String LOG_WIRETAP_IMAGE = "frontend/images/log-wiretap.png";

    private String wiretapBeforeStatus = "NodeFoundStatus.EMPTY";
    private String wiretapAfterStatus = "NodeFoundStatus.EMPTY";
    private String logWiretapBeforeStatus = "NodeFoundStatus.EMPTY";
    private String logWiretapAfterStatus = "NodeFoundStatus.EMPTY";

    protected double wiretapBeforeImageX = -35;
    protected double wiretapBeforeImageY = -30;
    protected int wiretapBeforeImageH = 30;
    protected int wiretapBeforeImageW = 30;

    protected double wiretapAfterImageX = 95;
    protected double wiretapAfterImageY = -30;
    protected int wiretapAfterImageH = 30;
    protected int wiretapAfterImageW = 30;

    protected double logWiretapBeforeImageX = -35;
    protected double logWiretapBeforeImageY = -70;
    protected int logWiretapBeforeImageH = 30;
    protected int logWiretapBeforeImageW = 30;

    protected double logWiretapAfterImageX = 95;
    protected double logWiretapAfterImageY = -70;
    protected int logWiretapAfterImageH = 30;
    protected int logWiretapAfterImageW = 30;


    private List<DecoratorMetaData> decoratorMetaDataList;

    /**
     * Constructs an AbstractWiretapNode with the specified identifier, name, and image.
     *
     * @param id the unique identifier for this wiretap node
     * @param name the name of this wiretap node
     * @param image the image path associated with this wiretap node
     */
    public AbstractWiretapNode(DesignerItemIdentifier id, String name, String image) {
        super(id, 0, 0, image);
    }

    /**
     * Retrieves the wiretap before status.
     *
     * @return the status of the wiretap before operation as a String.
     */
    public String getWiretapBeforeStatus() {
        return wiretapBeforeStatus;
    }

    /**
     * Sets the status of the wiretap operation before a specific event.
     *
     * @param wiretapBeforeStatus The status to assign to the wiretap operation prior to the event.
     */
    public void setWiretapBeforeStatus(String wiretapBeforeStatus) {
        this.wiretapBeforeStatus = wiretapBeforeStatus;
    }

    /**
     * Retrieves the status of the wiretap after an operation.
     *
     * @return the wiretap after status as a String.
     */
    public String getWiretapAfterStatus() {
        return wiretapAfterStatus;
    }

    /**
     * Sets the status associated with the wiretap that is applied after a specific operation.
     *
     * @param wiretapAfterStatus the status to set for the wiretap after the operation
     */
    public void setWiretapAfterStatus(String wiretapAfterStatus) {
        this.wiretapAfterStatus = wiretapAfterStatus;
    }

    /**
     * Retrieves the X-coordinate of the wiretap image before a certain operation.
     *
     * @return the X-coordinate of the wiretap image before the operation as*/
    public double getWiretapBeforeImageX() {
        return wiretapBeforeImageX;
    }

    /**
     * Sets the X-coordinate for the wiretap before image.
     *
     * @param wiretapBeforeImageX the X-coordinate to set for the wiretap before image
     */
    public void setWiretapBeforeImageX(double wiretapBeforeImageX) {
        this.wiretapBeforeImageX = wiretapBeforeImageX;
    }

    /**
     * Retrieves the Y-coordinate of the wiretap before image.
     *
     * @return the Y-coordinate as a double value representing the vertical position
     *         of the wiretap before image.
     */
    public double getWiretapBeforeImageY() {
        return wiretapBeforeImageY;
    }

    /**
     * Sets the Y-coordinate for the "wiretap before" image.
     *
     * @param wiretapBeforeImageY the Y-coordinate of the "wiretap before" image
     */
    public void setWiretapBeforeImageY(double wiretapBeforeImageY) {
        this.wiretapBeforeImageY = wiretapBeforeImageY;
    }

    /**
     * Returns the height of the "wiretap before" image.
     *
     * @return the height of the "wiretap before" image as an integer.
     */
    public int getWiretapBeforeImageH() {
        return wiretapBeforeImageH;
    }

    /**
     * Sets the height of the wiretap "before" image.
     *
     * @param wiretapBeforeImageH the height of the wiretap "before" image to be set
     */
    public void setWiretapBeforeImageH(int wiretapBeforeImageH) {
        this.wiretapBeforeImageH = wiretapBeforeImageH;
    }

    /**
     * Retrieves the width of the "wiretap before" image associated with this node.
     *
     * @return the width of the "wiretap before" image as an integer.
     */
    public int getWiretapBeforeImageW() {
        return wiretapBeforeImageW;
    }

    /**
     * Sets the width of the wiretap before image.
     *
     * @param wiretapBeforeImageW the width of the wiretap before image to set
     */
    public void setWiretapBeforeImageW(int wiretapBeforeImageW) {
        this.wiretapBeforeImageW = wiretapBeforeImageW;
    }

    /**
     * Retrieves the X-coordinate of the wiretap after image.
     *
     * @return the X-coordinate of the wiretap after image as a double
     */
    public double getWiretapAfterImageX() {
        return wiretapAfterImageX;
    }

    /**
     * Sets the X-coordinate for the wiretap after the image.
     *
     * @param wiretapAfterImageX the X-coordinate value to be set for the wiretap after the image
     */
    public void setWiretapAfterImageX(double wiretapAfterImageX) {
        this.wiretapAfterImageX = wiretapAfterImageX;
    }

    /**
     * Retrieves the Y-coordinate of the "wiretap after image".
     *
     * @return the Y-coordinate of the wiretap after image as a double.
     */
    public double getWiretapAfterImageY() {
        return wiretapAfterImageY;
    }

    /**
     * Sets the Y-coordinate of the wiretap after image.
     *
     * @param wiretapAfterImageY the Y-coordinate to set for the wiretap after image
     */
    public void setWiretapAfterImageY(double wiretapAfterImageY) {
        this.wiretapAfterImageY = wiretapAfterImageY;
    }

    /**
     * Retrieves the height of the wiretap after image.
     *
     * @return the height of the wiretap after image as an integer.
     */
    public int getWiretapAfterImageH() {
        return wiretapAfterImageH;
    }

    /**
     * Sets the height of the wiretap after image.
     *
     * @param wiretapAfterImageH the height of the wiretap after image in pixels
     */
    public void setWiretapAfterImageH(int wiretapAfterImageH) {
        this.wiretapAfterImageH = wiretapAfterImageH;
    }

    /**
     * Retrieves the width of the after-image for the wiretap.
     *
     * @return the width of the wiretap after-image as an integer.
     */
    public int getWiretapAfterImageW() {
        return wiretapAfterImageW;
    }

    /**
     * Sets the width of the "wiretap after" image.
     *
     * @param wiretapAfterImageW the width of the wiretap after image to set, in pixels
     */
    public void setWiretapAfterImageW(int wiretapAfterImageW) {
        this.wiretapAfterImageW = wiretapAfterImageW;
    }

    /**
     * Retrieves the status of the log wiretap operation before a specific event.
     *
     * @return the status of the log wiretap before operation as a String.
     */
    public String getLogWiretapBeforeStatus() {
        return logWiretapBeforeStatus;
    }

    /**
     * Sets the log wiretap status for operations that occur before a specific event.
     *
     * @param logWiretapBeforeStatus the status to assign to the log wiretap before the event
     */
    public void setLogWiretapBeforeStatus(String logWiretapBeforeStatus) {
        this.logWiretapBeforeStatus = logWiretapBeforeStatus;
    }

    /**
     * Retrieves the log wiretap status after a specific operation.
     *
     * @return the status of the log wiretap after the operation as a String.
     */
    public String getLogWiretapAfterStatus() {
        return logWiretapAfterStatus;
    }

    /**
     * Sets the status of the log wiretap that is applied after a specific operation.
     *
     * @param logWiretapAfterStatus the status to set for the log wiretap after the operation
     */
    public void setLogWiretapAfterStatus(String logWiretapAfterStatus) {
        this.logWiretapAfterStatus = logWiretapAfterStatus;
    }

    /**
     * Retrieves the X-coordinate of the "log wiretap before" image.
     *
     * @return the X-coordinate of the "log wiretap before" image as a double.
     */
    public double getLogWiretapBeforeImageX() {
        return logWiretapBeforeImageX;
    }

    /**
     * Sets the X-coordinate for the "log wiretap before" image.
     *
     * @param logWiretapBeforeImageX the X-coordinate to set for the "log wiretap before" image
     */
    public void setLogWiretapBeforeImageX(double logWiretapBeforeImageX) {
        this.logWiretapBeforeImageX = logWiretapBeforeImageX;
    }

    /**
     * Retrieves the Y-coordinate of the "log wiretap before" image.
     *
     * @return the Y-coordinate as a double value representing the vertical position
     *         of the "log wiretap before" image.
     */
    public double getLogWiretapBeforeImageY() {
        return logWiretapBeforeImageY;
    }

    /**
     * Sets the Y-coordinate for the log-based wiretap "before image".
     *
     * @param logWiretapBeforeImageY the Y-coordinate of the log-based wiretap "before image"
     *                               as a double value.
     */
    public void setLogWiretapBeforeImageY(double logWiretapBeforeImageY) {
        this.logWiretapBeforeImageY = logWiretapBeforeImageY;
    }

    /**
     * Retrieves the height of the "log wiretap before" image associated with this node.
     *
     * @return the height of the "log wiretap before" image as an integer.
     */
    public int getLogWiretapBeforeImageH() {
        return logWiretapBeforeImageH;
    }

    /**
     * Sets the height of the "log wiretap before" image.
     *
     * @param logWiretapBeforeImageH the height of the "log wiretap before" image in pixels
     */
    public void setLogWiretapBeforeImageH(int logWiretapBeforeImageH) {
        this.logWiretapBeforeImageH = logWiretapBeforeImageH;
    }

    /**
     * Retrieves the width of the "log wiretap before" image associated with this node.
     *
     * @return the width of the "log wiretap before" image as an integer.
     */
    public int getLogWiretapBeforeImageW() {
        return logWiretapBeforeImageW;
    }

    /**
     * Sets the width of the "log wiretap before" image.
     *
     * @param logWiretapBeforeImageW the width of the "log wiretap before" image to be set, in pixels
     */
    public void setLogWiretapBeforeImageW(int logWiretapBeforeImageW) {
        this.logWiretapBeforeImageW = logWiretapBeforeImageW;
    }

    /**
     * Retrieves the X-coordinate of the log wiretap after image.
     *
     * @return the X-coordinate of the log wiretap after image as a double.
     */
    public double getLogWiretapAfterImageX() {
        return logWiretapAfterImageX;
    }

    /**
     * Sets the X-coordinate for the log wiretap after image.
     *
     * @param logWiretapAfterImageX the X-coordinate value to be set for the log wiretap after image
     */
    public void setLogWiretapAfterImageX(double logWiretapAfterImageX) {
        this.logWiretapAfterImageX = logWiretapAfterImageX;
    }

    /**
     * Retrieves the Y-coordinate of the "log wiretap after" image.
     *
     * @return the Y-coordinate of the "log wiretap after" image as a double,
     *         representing its vertical position.
     */
    public double getLogWiretapAfterImageY() {
        return logWiretapAfterImageY;
    }

    /**
     * Sets the Y-coordinate for the log wiretap after image.
     *
     * @param logWiretapAfterImageY the Y-coordinate to set for the log wiretap after image as a double
     */
    public void setLogWiretapAfterImageY(double logWiretapAfterImageY) {
        this.logWiretapAfterImageY = logWiretapAfterImageY;
    }

    /**
     * Retrieves the height of the "log wiretap after" image.
     *
     * @return the height of the "log wiretap after" image as an integer.
     */
    public int getLogWiretapAfterImageH() {
        return logWiretapAfterImageH;
    }

    /**
     * Sets the height of the "log wiretap after" image.
     *
     * @param logWiretapAfterImageH the height of the "log wiretap after" image in pixels
     */
    public void setLogWiretapAfterImageH(int logWiretapAfterImageH) {
        this.logWiretapAfterImageH = logWiretapAfterImageH;
    }

    /**
     * Retrieves the width of the "log wiretap after" image.
     *
     * @return the width of the "log wiretap after" image as an integer.
     */
    public int getLogWiretapAfterImageW() {
        return logWiretapAfterImageW;
    }

    /**
     * Sets the width of the "log wiretap after" image.
     *
     * @param logWiretapAfterImageW the width of the "log wiretap after" image to set, specified in pixels
     */
    public void setLogWiretapAfterImageW(int logWiretapAfterImageW) {
        this.logWiretapAfterImageW = logWiretapAfterImageW;
    }

    /**
     * Retrieves the list of decorator metadata associated with this node.
     *
     * @return a list of {@code DecoratorMetaData} objects representing the metadata.
     */
    public List<DecoratorMetaData> getDecoratorMetaDataList() {
        return decoratorMetaDataList;
    }

    /**
     * Sets the list of decorator metadata associated with this node.
     *
     * @param decoratorMetaDataList the list of {@link DecoratorMetaData} objects to set
     *                              as the decorator metadata for this node
     */
    public void setDecoratorMetaDataList(List<DecoratorMetaData> decoratorMetaDataList) {
        this.decoratorMetaDataList = decoratorMetaDataList;
    }
}
