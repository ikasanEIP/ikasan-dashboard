package org.ikasan.dashboard.ui.visualisation.model.flow;

/**
 * Represents a destination that can be associated with various types of nodes or entities.
 *
 * Implementations of this interface define specific behaviors for different types of destinations,
 * such as file locations, FTP/SFTP destinations, or message channels.
 *
 * This interface provides a method to set the X-coordinate of the destination.
 */
public interface Destination {

    /**
     * Sets the X-coordinate of the destination.
     *
     * @param x the X-coordinate to set, represented as an Integer
     */
    void setX(final Integer x);
}
