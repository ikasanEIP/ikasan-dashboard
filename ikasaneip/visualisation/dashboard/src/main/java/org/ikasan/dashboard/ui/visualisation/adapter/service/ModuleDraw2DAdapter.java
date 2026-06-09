package org.ikasan.dashboard.ui.visualisation.adapter.service;

import org.apache.commons.text.WordUtils;
import org.ikasan.dashboard.ui.visualisation.model.flow.*;
import org.ikasan.dashboard.ui.visualisation.model.flow.Module;
import org.ikasan.designer.pallet.DesignerItemIdentifier;
import org.ikasan.spec.component.endpoint.Broker;
import org.ikasan.spec.component.endpoint.Producer;
import org.ikasan.spec.component.filter.Filter;
import org.ikasan.spec.component.routing.MultiRecipientRouter;
import org.ikasan.spec.component.routing.SingleRecipientRouter;
import org.ikasan.spec.component.splitting.Splitter;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.Translator;
import org.ikasan.spec.metadata.model.*;
import org.ikasan.spec.module.StartupType;
import org.ikasan.spec.trigger.TriggerJobType;
import org.ikasan.spec.trigger.TriggerRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ModuleDraw2DAdapter
{
    Logger logger = LoggerFactory.getLogger(ModuleDraw2DAdapter.class);

    private int identifier;
    private HashMap<String, String> fromTransitionLabelMap = new HashMap<>();
    private HashMap<String, String> toTransitionLabelMap = new HashMap<>();
    private HashMap<String, ConfigurationMetaData> configurationMetaDataHashMap;
    private HashMap<String, FlowElementMetaData> componentMap;


    /**
     * Adapts the provided module metadata and configuration metadata into a Module instance.
     *
     * @param moduleMetaData The metadata that describes the module to be adapted.
     * @param configurationMetaData A list of configuration metadata used to further define the module structure.
     * @return A fully configured Module instance based on the provided metadata.
     */
    public Module adapt(ModuleMetaData moduleMetaData, List<ConfigurationMetaData> configurationMetaData)
    {
        this.configurationMetaDataHashMap = new HashMap<>();
        this.componentMap = new HashMap<>();
        Module module = new Module(moduleMetaData.getUrl(), moduleMetaData.getName(), moduleMetaData.getDescription()
            , moduleMetaData.getVersion(), configurationMetaDataHashMap, componentMap);

        Map<String, ConfigurationMetaData> configurationMetaDataMap = new HashMap<>();

        if(configurationMetaData != null) {
            configurationMetaDataMap = configurationMetaData.stream().
                collect(Collectors.toMap(metaData -> metaData.getConfigurationId(), metaData -> metaData));
        }

        identifier = 0;

        for(FlowMetaData flowMetaData: moduleMetaData.getFlows())
        {
            module.addFlow(this.manageFlow(flowMetaData, configurationMetaDataMap));
        }

        return module;
    }

    /**
     * Manages the creation of a Flow instance based on the provided metadata and configuration.
     *
     * @param flowMetaData The metadata containing all details about the flow, including flow elements, transitions, consumer, and startup type.
     * @param configurationMetaDataMap A map of configuration metadata keyed by configuration name, used to configure flow elements.
     * @return A fully constructed Flow instance based on the input metadata and configurations.
     */
    protected Flow manageFlow(FlowMetaData flowMetaData, Map<String, ConfigurationMetaData> configurationMetaDataMap)
    {
        Map<String, FlowElementMetaData> flowElements = flowMetaData.getFlowElements().stream().collect(
            Collectors.toMap(FlowElementMetaData::getComponentName, flowElementMetaData -> flowElementMetaData, (key1, key2) -> key1));

        this.buildFromTransitionLabelMap(flowMetaData.getTransitions());
        this.buildToTransitionLabelMap(flowMetaData.getTransitions());

        List<Transition> uniqueTransitions = distinctList(flowMetaData.getTransitions(), Transition::getFrom, Transition::getTo);

        Consumer consumer = (Consumer) manageFlowElement(flowMetaData.getConsumer(), uniqueTransitions, flowElements, configurationMetaDataMap);

        StartupType startupType = null;

        if(flowMetaData.getFlowStartupType() != null && flowMetaData.getFlowStartupType().equalsIgnoreCase(StartupType.AUTOMATIC.name())) {
            startupType = StartupType.AUTOMATIC;
        }
        else if(flowMetaData.getFlowStartupType() != null && flowMetaData.getFlowStartupType().equalsIgnoreCase(StartupType.DISABLED.name())) {
            startupType = StartupType.DISABLED;
        }
        else if(flowMetaData.getFlowStartupType() != null && flowMetaData.getFlowStartupType().equalsIgnoreCase(StartupType.MANUAL.name())) {
            startupType = StartupType.MANUAL;
        }
        else {

        }

        return new Flow(flowMetaData.getName(), flowMetaData.getConfigurationId(), consumer, startupType, flowMetaData.getFlowStartupComment());
    }

    /**
     * Returns a list containing distinct elements from the provided list based on
     * the specified key extractors. The distinctiveness is determined using the keys
     * extracted from the elements by the provided functions.
     *
     * @param <T> the type of elements in the list
     * @param list the list of elements to process for distinct elements
     * @param keyExtractors the functions used to extract keys for determining distinctiveness
     * @return a list containing distinct elements based on the extracted keys
     */
    private static <T> List<T> distinctList(List<T> list, Function<? super T, ?>... keyExtractors)
    {
        return list
            .stream()
            .filter(distinctByKeys(keyExtractors))
            .collect(Collectors.toList());
    }

    /**
     * Returns a predicate that maintains state to ensure that each combination of keys extracted
     * by the provided key extractors is encountered only once. This can be used to filter a stream
     * of objects based on distinct combinations of properties.
     *
     * @param <T> the type of input to the predicate
     * @param keyExtractors one or more functions to extract keys from the input object
     * @return a predicate that returns true if the combination of keys has not been encountered before,
     *         otherwise false
     */
    private static <T> Predicate<T> distinctByKeys(Function<? super T, ?>... keyExtractors)
    {
        final Map<List<?>, Boolean> seen = new ConcurrentHashMap<>();

        return t ->
        {

            final List<?> keys = Arrays.stream(keyExtractors)
                .map(ke -> ke.apply(t))
                .collect(Collectors.toList());

            return seen.putIfAbsent(keys, Boolean.TRUE) == null;
        };
    }

    /**
     * Manages the creation and decoration of a wiretap node for a given flow element based on its component type.
     * Handles single and multi-transition flow elements, applying appropriate processing for each type.
     *
     * @param flowElement the flow element metadata object that represents the component within the flow
     * @param transitions the list of transitions associated with the flow element
     * @param flowElements a map of flow element names to flow element metadata, representing all elements in the flow
     * @param configurationMetaDataMap a map of configuration names to configuration metadata objects, providing configuration details
     *                                  for the components within the flow
     * @return an instance of {@link AbstractWiretapNode} representing the wiretap node for the given flow element
     * @throws IllegalArgumentException if the component type of the flow element is unknown or unsupported
     */
    protected AbstractWiretapNode manageFlowElement(FlowElementMetaData flowElement, List<Transition> transitions,
                                     Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap)
    {
        if (flowElement.getComponentType().equals(org.ikasan.spec.component.endpoint.Consumer.class.getName())
            || flowElement.getComponentType().equals(Converter.class.getName())
            || flowElement.getComponentType().equals(Translator.class.getName())
            || flowElement.getComponentType().equals(Splitter.class.getName())
            || flowElement.getComponentType().equals(Filter.class.getName())
            || flowElement.getComponentType().equals(Broker.class.getName())
            || flowElement.getComponentType().equals(Producer.class.getName()))
        {
            AbstractWiretapNode node =  manageSingleTransition(flowElement, transitions, flowElements, configurationMetaDataMap);
            this.decorateWiretap(flowElement, node);
            node.setName(flowElement.getComponentName());
            return node;
        }
        else if (flowElement.getComponentType().equals(SingleRecipientRouter.class.getName())||
            flowElement.getComponentType().equals(MultiRecipientRouter.class.getName()))
        {
            AbstractWiretapNode node = manageMultiTransition(flowElement, transitions, flowElements, configurationMetaDataMap);
            this.decorateWiretap(flowElement, node);
            node.setName(flowElement.getComponentName());
            return node;
        }
        else
        {
            throw new IllegalArgumentException("Unknown component type encountered");
        }
    }

    /**
     * Decorates the given wiretap node based on the decorators present in the flow element metadata.
     * It checks for specific decorator types and names and updates the wiretap node's status accordingly.
     *
     * @param flowElement the metadata of the flow element containing decorators to be evaluated
     * @param node the wiretap node to be decorated and updated based on the matched decorators
     */
    private void decorateWiretap(FlowElementMetaData flowElement, AbstractWiretapNode node) {
        if(flowElement.getDecorators() != null) {
            flowElement.getDecorators().forEach(decoratorMetaData -> {
                if (decoratorMetaData.getType().equals(TriggerJobType.WIRETAP.getDescription()) && decoratorMetaData.getName()
                    .startsWith(TriggerRelationship.BEFORE.getDescription().toUpperCase())) {
                    node.setWiretapBeforeStatus(NodeFoundStatus.FOUND);
                    node.setDecoratorMetaDataList(flowElement.getDecorators());
                }
                else if (decoratorMetaData.getType().equals(TriggerJobType.WIRETAP.getDescription()) && decoratorMetaData.getName()
                    .startsWith(TriggerRelationship.AFTER.getDescription().toUpperCase())) {
                    node.setWiretapAfterStatus(NodeFoundStatus.FOUND);
                    node.setDecoratorMetaDataList(flowElement.getDecorators());
                }
                else if (decoratorMetaData.getType().equals(TriggerJobType.LOG_WIRETAP.getDescription()) && decoratorMetaData.getName()
                    .startsWith(TriggerRelationship.BEFORE.getDescription().toUpperCase())) {
                    node.setLogWiretapBeforeStatus(NodeFoundStatus.FOUND);
                    node.setDecoratorMetaDataList(flowElement.getDecorators());
                }
                else if (decoratorMetaData.getType().equals(TriggerJobType.LOG_WIRETAP.getDescription()) && decoratorMetaData.getName()
                    .startsWith(TriggerRelationship.AFTER.getDescription().toUpperCase())) {
                    node.setLogWiretapAfterStatus(NodeFoundStatus.FOUND);
                    node.setDecoratorMetaDataList(flowElement.getDecorators());
                }
            });
        }
    }

    /**
     * Retrieves the list of transition target flow elements originating from the given flow element.
     *
     * @param flowElement the current flow element for which transitions are being determined
     * @param transitions the list of transitions connecting flow elements
     * @param flowElements a mapping of flow element names to their metadata
     * @return a list of flow element metadata objects representing the target elements of transitions
     */
    protected List<FlowElementMetaData> getTransitions(FlowElementMetaData flowElement, List<Transition> transitions,
                                                       Map<String, FlowElementMetaData> flowElements)
    {
        return transitions.stream()
                .filter(transition -> transition.getFrom().equals(flowElement.getComponentName()))
                .map(transition -> flowElements.get(transition.getTo()))
                .collect(Collectors.toList());
    }

    /**
     * Populates the fromTransitionLabelMap with concatenated transition names associated
     * with their respective "from" states. If a "from" state already exists as a key in
     * the map, the transition name is appended to the existing value, separated by a comma.
     * If the "from" state does not exist in the map, a new key-value pair is added.
     *
     * @param transitions A list of Transition objects from which to build the
     *                    fromTransitionLabelMap. Each Transition contains "from" and
     *                    "name" attributes used for constructing the map.
     */
    protected void buildFromTransitionLabelMap(List<Transition> transitions)
    {
        for(Transition transition: transitions)
        {
            if (this.fromTransitionLabelMap.containsKey(transition.getFrom()))
            {
                String label = fromTransitionLabelMap.get(transition.getFrom());

                if(!label.contains(transition.getName()))
                {
                    label = label + ", " + transition.getName();
                    fromTransitionLabelMap.put(transition.getFrom(), label);
                }
            }
            else
            {
                String label = transition.getName();
                fromTransitionLabelMap.put(transition.getFrom(), label);
            }
        }
    }

    /**
     * Builds a mapping of transition targets to their corresponding labels.
     * For each transition, if its target already exists in the map, its name is appended
     * to the existing label for that target. If the target does not exist in the map,
     * it is added with its current transition name as the label. The labels are
     * wrapped to a maximum length of 15 characters.
     *
     * @param transitions the list of transitions to be processed and added to the map
     */
    protected void buildToTransitionLabelMap(List<Transition> transitions)
    {
        for(Transition transition: transitions)
        {
            if (this.toTransitionLabelMap.containsKey(transition.getTo()))
            {
                String label = toTransitionLabelMap.get(transition.getTo());

                if(!label.contains(transition.getName()))
                {
                    label = label + ", " + transition.getName();
                    toTransitionLabelMap.put(transition.getTo(), WordUtils.wrap(label, 15));
                }
            }
            else
            {
                String label = transition.getName();
                toTransitionLabelMap.put(transition.getTo(), label);
            }
        }
    }

    /**
     * Manages the transition logic for a single transition scenario in a flow element.
     * Based on the type of the flow element (e.g., Producer, Consumer, Converter, etc.),
     * this method delegates the processing to the corresponding handler method.
     *
     * @param flowElement the metadata of the flow element being processed
     * @param transitions the list of transitions available for the flow
     * @param flowElements a map of flow element names to their metadata
     * @param configurationMetaDataMap a map of configuration names to their metadata
     * @return an instance of {@code AbstractWiretapNode} after processing the flow element
     * @throws IllegalArgumentException if the component type of the flow element is unknown
     */
    protected AbstractWiretapNode manageSingleTransition(FlowElementMetaData flowElement, List<Transition> transitions,
                                                      Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap)
    {

        if (flowElement.getComponentType().equals(Producer.class.getName()))
        {
            return this.manageProducers(flowElement, configurationMetaDataMap);
        }

        // As the name of this method implies, this method only deals with components that have a single transition so get the first.
        FlowElementMetaData flowElementMetaData = this.getTransitions(flowElement, transitions, flowElements).get(0);

        if (flowElement.getComponentType().equals(org.ikasan.spec.component.endpoint.Consumer.class.getName()))
        {
            return this.manageConsumers(flowElement, flowElementMetaData, transitions, flowElements, configurationMetaDataMap);
        }
        else if (flowElement.getComponentType().equals(Converter.class.getName()))
        {
            return this.manageConverter(flowElement, flowElementMetaData, transitions, flowElements, configurationMetaDataMap);
        }
        else if (flowElement.getComponentType().equals(Translator.class.getName()))
        {
            return this.manageTranslator(flowElement, flowElementMetaData, transitions, flowElements, configurationMetaDataMap);
        }
        else if (flowElement.getComponentType().equals(Splitter.class.getName()))
        {
            return this.manageSplitter(flowElement, flowElementMetaData, transitions, flowElements, configurationMetaDataMap);
        }
        else if (flowElement.getComponentType().equals(Filter.class.getName()))
        {
            return this.manageFilter(flowElement, flowElementMetaData, transitions, flowElements, configurationMetaDataMap);
        }
        else if (flowElement.getComponentType().equals(Broker.class.getName()))
        {
            return this.manageBroker(flowElement, flowElementMetaData, transitions, flowElements, configurationMetaDataMap);
        }
        else
        {
            throw new IllegalArgumentException("Unknown component type encountered");
        }

    }

    /**
     * Manages transitions for a flow element and determines the appropriate processing
     * logic based on the component type of the flow element.
     *
     * @param flowElement the flow element whose transitions need to be managed.
     * @param transitions the list of transitions associated with the flow element.
     * @param flowElements a map of all flow elements, where the keys are their identifiers.
     * @param configurationMetaDataMap a map of configuration metadata, where the keys are configuration names.
     * @return an instance of {@code AbstractWiretapNode} resulting from the processing of the flow element's transitions.
     * @throws IllegalArgumentException if the component type of the flow element is unknown.
     */
    protected AbstractWiretapNode manageMultiTransition(FlowElementMetaData flowElement, List<Transition> transitions,
                                                    Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap)
        {
        List<FlowElementMetaData> flowElementMetaDataTransitions
            = this.getTransitions(flowElement, transitions, flowElements);

        if (flowElement.getComponentType().equals(SingleRecipientRouter.class.getName()))
        {
            return this.manageSingleRecipientRouter(flowElement, transitions, flowElements, configurationMetaDataMap, flowElementMetaDataTransitions);
        }
        else if (flowElement.getComponentType().equals(MultiRecipientRouter.class.getName()))
        {
            return this.manageMultiRecipientRouter(flowElement, transitions, flowElements, configurationMetaDataMap, flowElementMetaDataTransitions);
        }
        else
        {
            throw new IllegalArgumentException("Unknown component type encountered");
        }
    }

    /**
     * Manages the creation of producer nodes in the application flow based on the
     * metadata of the provided flow element. Determines the type of producer and
     * constructs the corresponding node, handling specific implementations such as
     * JMS producers, FTP/SFTP producers, or other endpoints.
     *
     * @param flowElement the flow element metadata representing the producer in the flow.
     * @param configurationMetaDataMap a map of configuration metadata, keyed by configuration ID,
     *                                  providing additional configuration details for components.
     * @return an instance of AbstractWiretapNode representing the configured producer node for the flow.
     */
    private AbstractWiretapNode manageProducers(FlowElementMetaData flowElement, Map<String, ConfigurationMetaData> configurationMetaDataMap)
    {
        DesignerItemIdentifier nodeId = new DesignerItemIdentifier(flowElement.getComponentType(), flowElement.getComponentName(), flowElement.getComponentName() + identifier++);
        this.manageModuleMaps(nodeId, configurationMetaDataMap, flowElement);

        if(flowElement.getImplementingClass().equals("org.ikasan.component.endpoint.util.producer.DevNull"))
        {
            return DeadEndPoint.deadEndPointBuilder()
                .withId(nodeId)
                .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
                .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
                .build();
        }
        else if(flowElement.getImplementingClass().equals("org.ikasan.component.endpoint.jms.spring.producer.ArjunaJmsTemplateProducer") ||
            flowElement.getImplementingClass().equals("org.ikasan.component.endpoint.jms.spring.producer.JmsTemplateProducer"))
        {
            ConfigurationMetaData configurationMetaData = configurationMetaDataMap.get(flowElement.getConfigurationId());
            String destinationName = this.getConfigurationParameterMetaData("destinationJndiName", configurationMetaData);

            DesignerItemIdentifier messageChannelId = new DesignerItemIdentifier("MESSAGE_CHANNEL"
                , destinationName, "channel" + identifier++);

            return MessageProducer.messageProducerBuilder()
                .withId(nodeId)
                .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
                .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
                .withTransition(new MessageChannel(messageChannelId, WordUtils.wrap(destinationName, 25, "\n", true, "\\."), false))
                .build();
        }
        else if(flowElement.getImplementingClass().equals("org.ikasan.endpoint.ftp.producer.FtpProducer"))
        {
            ConfigurationMetaData configurationMetaData = configurationMetaDataMap.get(flowElement.getConfigurationId());
            String remoteHost = this.getConfigurationParameterMetaData("remoteHost", configurationMetaData);

            DesignerItemIdentifier ftpId = new DesignerItemIdentifier("FTP_LOCATION"
                , remoteHost, "channel" + identifier++);

            return MessageEndPoint.messageEndPointBuilder()
                .withId(nodeId)
                .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
                .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
                .withTransition(new FtpLocation(ftpId, remoteHost))
                .build();
        }
        else if(flowElement.getImplementingClass().equals("org.ikasan.endpoint.sftp.producer.SftpProducer"))
        {
            ConfigurationMetaData configurationMetaData = configurationMetaDataMap.get(flowElement.getConfigurationId());
            String remoteHost = this.getConfigurationParameterMetaData("remoteHost", configurationMetaData);

            DesignerItemIdentifier sftpId = new DesignerItemIdentifier("SFTP_LOCATION"
                , remoteHost, "channel" + identifier++);

            return MessageEndPoint.messageEndPointBuilder()
                .withId(nodeId)
                .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
                .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
                .withTransition(new SftpLocation(sftpId, remoteHost))
                .build();
        }

        DesignerItemIdentifier fileLocationId = new DesignerItemIdentifier("FILE_LOCATION"
            , "default", "fileLocation" + identifier++);

        return MessageEndPoint.messageEndPointBuilder()
            .withId(nodeId)
            .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
            .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
            .withTransition(new FileLocation(fileLocationId, ""))
            .build();
    }

    /**
     * Manages the creation and configuration of consumer nodes within a flow based on the provided metadata,
     * transitions, and configuration data. Determines the appropriate consumer type to use (FTP, SFTP, polling,
     * or event-driven) and builds the corresponding node with its associated parameters.
     *
     * @param flowElement the metadata for the current flow element being processed
     * @param flowElementMetaData the metadata for the parent or associated flow element
     * @param transitions the list of transitions associated with the current flow element
     * @param flowElements a map of all flow elements within the flow, keyed by their identifiers
     * @param configurationMetaDataMap a map of configuration metadata, keyed by configuration IDs
     * @return the constructed consumer node of type {@code AbstractWiretapNode} configured based on the type
     *         and parameters of the flow element
     */
    private AbstractWiretapNode manageConsumers(FlowElementMetaData flowElement, FlowElementMetaData flowElementMetaData, List<Transition> transitions,
                                 Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap)
    {
        ConfigurationMetaData configurationMetaData = configurationMetaDataMap.get(flowElement.getConfigurationId());

        DesignerItemIdentifier nodeId = new DesignerItemIdentifier(flowElement.getComponentType()
            , flowElement.getComponentName(), flowElement.getComponentName() + identifier++);
        this.manageModuleMaps(nodeId, configurationMetaDataMap, flowElement);

        if(flowElement.getImplementingClass().startsWith("org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumer"))
        {
            if(configurationMetaData != null && configurationMetaData.getImplementingClass().equals("org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration"))
            {
                String remoteHost = this.getConfigurationParameterMetaData("remoteHost", configurationMetaData);

                DesignerItemIdentifier ftpLocationId = new DesignerItemIdentifier("FTP_REMOTE_HOST"
                    , remoteHost, "ftpLocation" + identifier);

                return FtpConsumer.ftpConsumerBuilder()
                    .withId(nodeId)
                    .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
                    .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
                    .withTransition(manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap))
                    .withSource(new FtpLocation(ftpLocationId, remoteHost))
                    .build();

            }
            else if(configurationMetaData != null && configurationMetaData.getImplementingClass().equals("org.ikasan.endpoint.sftp.consumer.SftpConsumerConfiguration"))
            {
                String remoteHost = this.getConfigurationParameterMetaData("remoteHost", configurationMetaData);

                DesignerItemIdentifier sftpLocationId = new DesignerItemIdentifier("SFTP_REMOTE_HOST"
                    , remoteHost, "sftpLocation" + identifier);

                return SftpConsumer.sftpConsumerBuilder()
                    .withId(nodeId)
                    .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
                    .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
                    .withTransition(manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap))
                    .withSource(new SftpLocation(sftpLocationId, remoteHost))
                    .build();
            }
            else
            {
                DesignerItemIdentifier fileLocationId = new DesignerItemIdentifier("POLLING_CONSUMER"
                    , flowElement.getComponentType(), "fileLocation" + identifier);

                return PollingConsumer.pollingConsumerBuilder()
                    .withId(nodeId)
                    .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
                    .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
                    .withTransition(manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap))
                    .withSource(new FileLocation(fileLocationId, ""))
                    .build();
            }
        }

        String destinationName = this.getConfigurationParameterMetaData("destinationJndiName", configurationMetaData);

        DesignerItemIdentifier eventDrivenConsumerId = new DesignerItemIdentifier("EVENT_DRIVEN_CONSUMER"
            , destinationName, "destination" + identifier);

        return EventDrivenConsumer.eventDrivenConsumerBuilder()
            .withId(nodeId)
            .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
            .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
            .withTransition(manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap))
            .withSource(new MessageChannel(eventDrivenConsumerId, WordUtils.wrap(destinationName
                , 25, "\n", true, "\\."), false))
            .build();
    }

    /**
     * Manages the creation and configuration of a message converter for a specific flow element.
     *
     * @param flowElement the metadata of the flow element for which the converter is being managed
     * @param flowElementMetaData additional metadata of the flow element for detailed processing
     * @param transitions a list of transitions associated with the flow element
     * @param flowElements a map containing all flow elements in the process, keyed by their identifiers
     * @param configurationMetaDataMap a map of configuration metadata for the associated elements
     * @return an AbstractWiretapNode configured with the specified properties and transitions
     */
    private AbstractWiretapNode manageConverter(FlowElementMetaData flowElement, FlowElementMetaData flowElementMetaData, List<Transition> transitions, Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap)
    {
        DesignerItemIdentifier nodeId = new DesignerItemIdentifier(flowElement.getComponentType()
            , flowElement.getComponentName(), flowElement.getComponentName() + identifier++);
        this.manageModuleMaps(nodeId, configurationMetaDataMap, flowElement);

        return MessageConverter.messageConverterBuilder()
            .withId(nodeId)
            .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
            .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
            .withTransition(manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap))
            .build();

    }

    /**
     * Manages the creation and configuration of a translator node for wiretap purposes in a flow.
     *
     * @param flowElement              The metadata of the current flow element for which the translator is being managed.
     * @param flowElementMetaData      The metadata of the next flow element connected to the current flow element.
     * @param transitions              A list of transitions associated with the flow elements.
     * @param flowElements             A map containing flow element metadata keyed by their unique identifiers.
     * @param configurationMetaDataMap A map of configuration metadata keyed by their unique identifiers.
     * @return An {@code AbstractWiretapNode} that represents the configured wiretap translator node.
     */
    private AbstractWiretapNode manageTranslator(FlowElementMetaData flowElement, FlowElementMetaData flowElementMetaData, List<Transition> transitions,
                                 Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap)
    {
        DesignerItemIdentifier nodeId = new DesignerItemIdentifier(flowElement.getComponentType()
            , flowElement.getComponentName(), flowElement.getComponentName() + identifier++);
        this.manageModuleMaps(nodeId, configurationMetaDataMap, flowElement);

        return MessageTranslator.messageConverterBuilder()
            .withId(nodeId)
            .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
            .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
            .withTransition(manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap))
            .build();
    }

    /**
     * Manages the creation of an AbstractWiretapNode for a splitter within the flow
     * visualization model. This method handles the module mappings and builds the
     * splitter node with the specified properties from the provided metadata and transitions.
     *
     * @param flowElement the metadata of the current flow element representing the splitter
     * @param flowElementMetaData the metadata of the parent or related flow element
     * @param transitions the list of transitions associated with the flow elements
     * @param flowElements a map containing all flow elements in the current visualization, keyed by their identifiers
     * @param configurationMetaDataMap a map of configuration metadata required for managing module-specific mappings
     * @return an instance of AbstractWiretapNode representing the configured splitter in the visualization model
     */
    private AbstractWiretapNode manageSplitter(FlowElementMetaData flowElement, FlowElementMetaData flowElementMetaData, List<Transition> transitions,
                                  Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap)
    {
        DesignerItemIdentifier nodeId = new DesignerItemIdentifier(flowElement.getComponentType()
            , flowElement.getComponentName(), flowElement.getComponentName() + identifier++);
        this.manageModuleMaps(nodeId, configurationMetaDataMap, flowElement);

        return org.ikasan.dashboard.ui.visualisation.model.flow.Splitter.splitterBuilder()
            .withId(nodeId)
            .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
            .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
            .withTransition(manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap))
            .build();
    }

    /**
     * Manages the creation of a filter node in the flow visualization representation.
     *
     * @param flowElement The metadata of the flow element defining the current component.
     * @param flowElementMetaData The metadata of the adjoining flow element.
     * @param transitions A list of transitions associated with the flow element.
     * @param flowElements A map of all flow elements keyed by their identifiers.
     * @param configurationMetaDataMap A map of configuration metadata keyed by their identifiers.
     * @return An AbstractWiretapNode representing the filter node in the flow visualization.
     */
    private AbstractWiretapNode manageFilter(FlowElementMetaData flowElement, FlowElementMetaData flowElementMetaData, List<Transition> transitions,
                                Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap)
    {
        DesignerItemIdentifier nodeId = new DesignerItemIdentifier(flowElement.getComponentType()
            , flowElement.getComponentName(), flowElement.getComponentName() + identifier++);
        this.manageModuleMaps(nodeId, configurationMetaDataMap, flowElement);

        return org.ikasan.dashboard.ui.visualisation.model.flow.Filter.filterBuilder()
            .withId(nodeId)
            .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
            .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
            .withTransition(manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap))
            .build();
    }

    /**
     * Manages the creation and configuration of a broker node in the flow diagram.
     *
     * @param flowElement the metadata of the current flow element being processed
     * @param flowElementMetaData the metadata of the corresponding flow element connected to this flow element
     * @param transitions the list of transitions associated with the flow element
     * @param flowElements a map of all flow elements by their IDs
     * @param configurationMetaDataMap a map of configuration metadata by their IDs
     * @return the constructed wiretap node representing the broker in the flow diagram
     */
    private AbstractWiretapNode manageBroker(FlowElementMetaData flowElement, FlowElementMetaData flowElementMetaData, List<Transition> transitions,
                              Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap)
    {
        DesignerItemIdentifier nodeId = new DesignerItemIdentifier(flowElement.getComponentType()
            , flowElement.getComponentName(), flowElement.getComponentName() + identifier++);
        this.manageModuleMaps(nodeId, configurationMetaDataMap, flowElement);

        return org.ikasan.dashboard.ui.visualisation.model.flow.Broker.brokerBuilder()
            .withId(nodeId)
            .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
            .withTransitionLabel(this.fromTransitionLabelMap.get(flowElement.getComponentName()))
            .withTransition(manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap))
            .build();
    }

    /**
     * Manages the creation and configuration of a single recipient router node in a flow visualization.
     *
     * @param flowElement The metadata of the flow element that represents the single recipient router.
     * @param transitions The list of transitions between flow elements in the flow.
     * @param flowElements A map of flow element IDs to their metadata within the overall flow configuration.
     * @param configurationMetaDataMap A map of configuration identifiers to their associated metadata.
     * @param flowElementMetaDataTransitions The list of metadata for flow elements that represent transitions originating from the single recipient router.
     * @return An instance of {@code AbstractWiretapNode} representing the configured single recipient router in the visualization model.
     */
    private AbstractWiretapNode manageSingleRecipientRouter(FlowElementMetaData flowElement, List<Transition> transitions,
                                             Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap,
                                             List<FlowElementMetaData> flowElementMetaDataTransitions)
    {
        DesignerItemIdentifier nodeId = new DesignerItemIdentifier(flowElement.getComponentType()
            , flowElement.getComponentName(), flowElement.getComponentName() + identifier++);
        this.manageModuleMaps(nodeId, configurationMetaDataMap, flowElement);

        org.ikasan.dashboard.ui.visualisation.model.flow.SingleRecipientRouter router =
            org.ikasan.dashboard.ui.visualisation.model.flow.SingleRecipientRouter.singleRecipientRouterBuilder()
                .withId(nodeId)
                .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
                .build();

        flowElementMetaDataTransitions.stream().forEach(flowElementMetaData ->
            router.addTransition(Optional.ofNullable(this.toTransitionLabelMap.get(flowElementMetaData.getComponentName())).orElse(flowElementMetaData.getComponentName())
                , manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap)));

        return router;
    }

    /**
     * Manages the construction and configuration of a multi-recipient router node in the flow,
     * including the establishment of transitions to subsequent flow elements.
     *
     * @param flowElement The meta-data associated with the flow element representing the router.
     * @param transitions The list of transitions in the flow configuration that connect flow elements.
     * @param flowElements A map containing the meta-data of all flow elements in the flow, keyed by their identifiers.
     * @param configurationMetaDataMap A map representing the configurations associated with individual flow elements, keyed by configuration identifiers.
     * @param flowElementMetaDataTransitions A list of meta-data objects representing the transitions for the current flow element.
     * @return An instance of {@code AbstractWiretapNode} representing the configured multi-recipient router.
     */
    private AbstractWiretapNode manageMultiRecipientRouter(FlowElementMetaData flowElement, List<Transition> transitions,
                                             Map<String, FlowElementMetaData> flowElements, Map<String, ConfigurationMetaData> configurationMetaDataMap,
                                             List<FlowElementMetaData> flowElementMetaDataTransitions)
    {
        DesignerItemIdentifier nodeId = new DesignerItemIdentifier(flowElement.getComponentType()
            , flowElement.getComponentName(), flowElement.getComponentName() + identifier++);
        this.manageModuleMaps(nodeId, configurationMetaDataMap, flowElement);

        org.ikasan.dashboard.ui.visualisation.model.flow.RecipientListRouter router =
            org.ikasan.dashboard.ui.visualisation.model.flow.RecipientListRouter.recipientRouterBuilder()
                .withId(nodeId)
                .withName(WordUtils.wrap(flowElement.getComponentName(), 25))
                .build();

        flowElementMetaDataTransitions.stream().forEach(flowElementMetaData ->
            router.addTransition(Optional.ofNullable(this.toTransitionLabelMap.get(flowElementMetaData.getComponentName())).orElse(flowElementMetaData.getComponentName())
                , manageFlowElement(flowElementMetaData, transitions, flowElements, configurationMetaDataMap)));

        return router;
    }

    /**
     * Retrieves the metadata value for a specific configuration parameter from the provided {@code ConfigurationMetaData}.
     *
     * @param parameter The name of the configuration parameter whose metadata value is to be retrieved.
     * @param configurationMetaData An instance of {@code ConfigurationMetaData} containing a list of parameter metadata.
     * @return The metadata value of the specified configuration parameter as a {@code String}, or an empty string
     *         if the parameter does not exist, its metadata is undefined, or its value is null.
     */
    protected String getConfigurationParameterMetaData(String parameter, ConfigurationMetaData configurationMetaData)
    {
        if(configurationMetaData == null)
        {
            return "";
        }

        ConfigurationParameterMetaData parameterMetaData = ((List<ConfigurationParameterMetaData>)configurationMetaData.getParameters()).stream()
            .filter(configurationParameterMetaData -> parameter.equals(configurationParameterMetaData.getName()))
            .findAny()
            .orElse(null);

        if(parameterMetaData == null || parameterMetaData.getValue() == null)
        {
            return "";
        }

        return String.valueOf(parameterMetaData.getValue());
    }

    /**
     * Manages the mapping of module data by associating flow elements with their configuration metadata
     * and storing them in internal maps.
     *
     * @param nodeId the identifier of the designer item, used as a key for mapping flow elements
     * @param configurationMetaDataMap the map containing configuration metadata, where keys are configuration IDs
     * @param flowElement the flow element metadata associated with the given designer item identifier
     */
    private void manageModuleMaps(DesignerItemIdentifier nodeId, Map<String, ConfigurationMetaData> configurationMetaDataMap, FlowElementMetaData flowElement)
    {
        this.componentMap.put(nodeId.getName(), flowElement);

        ConfigurationMetaData configurationMetaData = configurationMetaDataMap.get(flowElement.getConfigurationId());

        if(configurationMetaData != null)
        {
            this.configurationMetaDataHashMap.put(nodeId.getUuid(), configurationMetaData);
        }
    }
}
