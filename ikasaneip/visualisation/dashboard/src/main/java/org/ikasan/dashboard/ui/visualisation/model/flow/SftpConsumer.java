package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a consumer specifically designed for handling SFTP transitions in a process flow.
 * This class extends the Consumer class and provides additional functionality specific to
 * SFTP operations.
 */
public class SftpConsumer extends Consumer
{
	public static final String IMAGE = "frontend/images/sftp-consumer.png";

    /**
     * Constructs an instance of SftpConsumer.
     *
     * @param id               the unique identifier for the SFTP consumer, used for design-time identification
     * @param name             the name of the SFTP consumer
     * @param transitionLabel  the label describing the transition this consumer handles
     * @param transition       the node representing the transition in the workflow
     * @param source           the source node from which the transition originates
     */
	private SftpConsumer(DesignerItemIdentifier id, String name, String transitionLabel, Node transition, Node source)
    {
        super(id, name, transitionLabel, transition, IMAGE, source);
    }

    /**
     * Creates and returns a new instance of the {@code SftpConsumerBuilder} class.
     * This builder facilitates the construction of {@code SftpConsumer} objects by providing
     * methods to set various properties required for its creation.
     *
     * @return a new instance of {@code SftpConsumerBuilder}, which can be used to build {@code SftpConsumer} instances
     */
    public static SftpConsumerBuilder sftpConsumerBuilder()
    {
        return new SftpConsumerBuilder();
    }

    /**
     * Builder class
     */
    public static class SftpConsumerBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;
        private Node source;

        /**
         * Assigns a unique identifier to the SftpConsumer being built.
         *
         * @param id the identifier of type {@link DesignerItemIdentifier} to set for the SftpConsumer
         * @return the current instance of {@code SftpConsumerBuilder} for method chaining
         */
        public SftpConsumerBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the SFTP consumer being built.
         *
         * @param name the name to assign to the SFTP consumer
         * @return the current instance of {@code SftpConsumerBuilder} for method chaining
         */
        public SftpConsumerBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the SFTP consumer and returns the builder instance.
         *
         * @param transitionLabel the transition label to associate with the SFTP consumer
         * @return the current instance of {@code SftpConsumerBuilder}
         */
        public SftpConsumerBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the SFTP consumer being built.
         *
         * @param transition the transition node to associate with the SFTP consumer
         * @return the current instance of {@code SftpConsumerBuilder} for method chaining
         */
        public SftpConsumerBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }

        /**
         * Sets the source Node for the SftpConsumerBuilder.
         *
         * @param source the Node object to set as the source
         * @return the current instance of SftpConsumerBuilder for method chaining
         */
        public SftpConsumerBuilder withSource(Node source)
        {
            this.source = source;
            return this;
        }

        /**
         * Builds and returns an instance of {@code SftpConsumer}.
         * Throws an {@code IllegalStateException} if any required fields
         * (id, name, or transition) are null.
         *
         * @return a newly constructed {@code SftpConsumer} instance
         * @throws IllegalStateException if id, name, or transition are null
         */
        public SftpConsumer build()
        {
            if (id == null || name == null || transition == null)
            {
                throw new IllegalStateException("Cannot create SftpConsumer. id, name and transition cannot be null!");
            }

            return new SftpConsumer(id, name, transitionLabel, transition, source);
        }
    }
}
