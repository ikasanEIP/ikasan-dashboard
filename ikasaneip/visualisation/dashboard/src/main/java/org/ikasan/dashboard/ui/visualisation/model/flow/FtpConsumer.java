package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a consumer that processes data from an FTP source.
 * Extends the {@code Consumer} class and provides specific behavior
 * and properties for FTP-based data consumption.
 */
public class FtpConsumer extends Consumer
{
	public static final String IMAGE = "frontend/images/ftp-consumer.png";

	/**
     * Constructs a new instance of the FtpConsumer class.
     *
     * @param id                The unique identifier for the designer item associated with this consumer.
     * @param name              The name of the FTP consumer.
     * @param transitionLabel   The label describing the transition associated with this consumer.
     * @param transition        The Node representing the transition process for the consumer.
     * @param source            The Node representing the source from which the data is consumed.
     */
    private FtpConsumer(DesignerItemIdentifier id, String name, String transitionLabel, Node transition, Node source)
    {
        super(id, name, transitionLabel, transition, IMAGE, source);
    }

    /**
     * Creates and returns a new instance of {@code FtpConsumerBuilder}.
     * The builder can be used to configure and construct an {@code FtpConsumer}.
     *
     * @return a new {@code FtpConsumerBuilder} instance
     */
    public static FtpConsumerBuilder ftpConsumerBuilder()
    {
        return new FtpConsumerBuilder();
    }

    /**
     * Builder class
     */
    public static class FtpConsumerBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;
        private Node source;

        /**
         * Sets the identifier for the FTP consumer being built.
         *
         * @param id the {@link DesignerItemIdentifier} representing the unique identifier for the FTP consumer
         * @return the current instance of {@link FtpConsumerBuilder} for method chaining
         */
        public FtpConsumerBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name of the FTP consumer.
         *
         * @param name the name to assign to the FTP consumer
         * @return the current instance of {@code FtpConsumerBuilder} for method chaining
         */
        public FtpConsumerBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the FTP consumer being built.
         *
         * @param transitionLabel the label to associate with the transition
         * @return the current instance of {@code FtpConsumerBuilder} for method chaining
         */
        public FtpConsumerBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the FTP consumer being built.
         *
         * @param transition the transition node to associate with the FTP consumer
         * @return the current instance of {@code FtpConsumerBuilder} for method chaining
         */
        public FtpConsumerBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }

        /**
         * Sets the source node for the FTP consumer being built.
         *
         * @param source the source node to be set for the FTP consumer
         * @return the current instance of {@code FtpConsumerBuilder} for method chaining
         */
        public FtpConsumerBuilder withSource(Node source)
        {
            this.source = source;
            return this;
        }

        /**
         * Builds and returns a new instance of {@code FtpConsumer}.
         * This method validates the required fields and ensures that all
         * mandatory properties are set before constructing the {@code FtpConsumer}.
         * If any required field (id, name, or transition) is null, an exception is thrown.
         *
         * @return a configured {@code FtpConsumer} instance
         * @throws IllegalStateException if any of the required fields (id, name, or transition) is null
         */
        public FtpConsumer build()
        {
            if(id == null || name == null || transition == null)
            {
                throw new IllegalStateException("Cannot create FtpConsumer. id, name and transition cannot be null!");
            }

            return new FtpConsumer(id, name, transitionLabel, transition, source);
        }
    }
}
