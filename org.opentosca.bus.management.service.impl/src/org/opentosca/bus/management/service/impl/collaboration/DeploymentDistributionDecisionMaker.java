package org.opentosca.bus.management.service.impl.collaboration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.opentosca.bus.management.collaboration.model.BodyType;
import org.opentosca.bus.management.collaboration.model.CollaborationMessage;
import org.opentosca.bus.management.collaboration.model.DiscoveryRequest;
import org.opentosca.bus.management.collaboration.model.KeyValueMap;
import org.opentosca.bus.management.collaboration.model.KeyValueType;
import org.opentosca.bus.management.collaboration.model.NodeTemplate;
import org.opentosca.bus.management.collaboration.model.RemoteOperations;
import org.opentosca.bus.management.collaboration.model.ServiceTemplateFragmentType;
import org.opentosca.bus.management.discovery.plugin.IManagementBusDiscoveryPluginService;
import org.opentosca.bus.management.header.MBHeader;
import org.opentosca.bus.management.service.impl.Activator;
import org.opentosca.bus.management.service.impl.servicehandler.ServiceHandler;
import org.opentosca.container.core.common.Settings;
import org.opentosca.container.core.next.model.NodeTemplateInstance;
import org.opentosca.container.core.next.model.RelationshipTemplateInstance;
import org.opentosca.container.core.tosca.convention.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class determines on which OpenTOSCA Container node an Implementation Artifact for a certain
 * service instance has to be deployed. It returns the host name of this Container node which can
 * then be used by the deployment and invocation plug-ins to perform operations with the
 * Implementation Artifact.<br>
 * <br>
 *
 * {@link Settings#OPENTOSCA_COLLABORATION_MODE} and the respective config.ini entry can be used to
 * control the device/service discovery. If the property is <tt>true</tt>, the discovery is
 * performed. If it is set to <tt>false</tt>, all IA deployments will be performed locally.
 * Therefore, the performance can be increased by disabling this setting if distributed IA
 * deployment is not needed.<br>
 * <br>
 *
 * Copyright 2018 IAAS University of Stuttgart
 */
public class DeploymentDistributionDecisionMaker {

    private final static Logger LOG = LoggerFactory.getLogger(DeploymentDistributionDecisionMaker.class);

    /**
     * Get the deployment location for IAs which are attached to a NodeTemplateInstance. If the
     * collaboration mode is turned on, this method performs a device/service discovery to determine
     * the deployment location. If the discovery is not successful, the local OpenTOSCA Container is
     * returned as default deployment location.
     *
     * @param nodeTemplateInstance the NodeTemplateInstance for which the IAs have to be deployed
     * @return the location where the IAs should be deployed
     */
    public static String getDeploymentLocation(final NodeTemplateInstance nodeTemplateInstance) {

        if (nodeTemplateInstance != null) {

            // only perform matching if collaboration mode is turned on
            if (Boolean.parseBoolean(Settings.OPENTOSCA_COLLABORATION_MODE)) {

                LOG.debug("Deployment distribution decision for IAs from NodeTemplateInstance with ID: {}",
                          nodeTemplateInstance.getId());

                // create DiscoveryRequest containing the topology fragments which represent the
                // searched device/service
                final DiscoveryRequest discoveryRequest = createDiscoveryRequest(nodeTemplateInstance);
                final Optional<IManagementBusDiscoveryPluginService> op = getDiscoveryPlugin(discoveryRequest);

                // if there is no suited discovery plug-in, local deployment has to be performed
                if (op.isPresent()) {
                    LOG.debug("Found suited discovery plug-in.");
                    final IManagementBusDiscoveryPluginService plugin = op.get();

                    // perform local device/service discovery
                    if (plugin.invokeDiscovery(discoveryRequest)) {
                        LOG.debug("Local discovery was successful. Performing local deployment...");
                        return Settings.OPENTOSCA_CONTAINER_HOSTNAME;
                    } else {
                        // no matching local device/service --> perform remote discovery
                        LOG.debug("Local discovery was not successful. Performing remote discovery...");
                        final Optional<String> deploymentLocationOptional = performRemoteDiscovery(discoveryRequest);

                        if (deploymentLocationOptional.isPresent()) {
                            LOG.debug("Found matching device/service remotely. Deployment will be done on OpenTOSCA Container with host name: {}",
                                      deploymentLocationOptional.get());
                            return deploymentLocationOptional.get();
                        } else {
                            LOG.warn("Remote discovery was not successful. Performing local deployment as default.");
                            return Settings.OPENTOSCA_CONTAINER_HOSTNAME;
                        }
                    }
                } else {
                    LOG.warn("No suited discovery plug-in available. Performing local deployment.");
                    return Settings.OPENTOSCA_CONTAINER_HOSTNAME;
                }
            } else {
                LOG.debug("Distributed IA deployment disabled. Performing local deployment.");
                return Settings.OPENTOSCA_CONTAINER_HOSTNAME;
            }
        } else {
            LOG.error("NodeTemplateInstance object is null. Performing local deployment.");
            return Settings.OPENTOSCA_CONTAINER_HOSTNAME;
        }
    }

    /**
     * Send a discovery request to all remote OpenTOSCA Containers and wait for a reply, containing
     * the host name of the Container which is responsible for this request. If no reply is received
     * an empty optional is returned.
     *
     * @param discoveryRequest the request for the remote device/service discovery
     * @return an optional containing the host name of the OpenTOSCA Container which found a
     *         matching device/service if one is found, an empty optional otherwise.
     */
    private static Optional<String> performRemoteDiscovery(final DiscoveryRequest discoveryRequest) {

        // create collaboration message
        final CollaborationMessage collaborationMessage =
            new CollaborationMessage(new KeyValueMap(), new BodyType(discoveryRequest));

        // create an unique correlation ID for the request
        final String correlationID = UUID.randomUUID().toString();

        LOG.debug("Publishing discovery request to MQTT broker at {} with topic {} and correlation ID {}",
                  Constants.LOCAL_MQTT_BROKER, Constants.REQUEST_TOPIC, correlationID);

        // create exchange containing the collaboration message and the needed headers
        final Exchange request =
            new ExchangeBuilder(Activator.camelContext).withBody(collaborationMessage)
                                                       .withHeader(MBHeader.MQTTBROKERHOSTNAME_STRING.toString(),
                                                                   Constants.LOCAL_MQTT_BROKER)
                                                       .withHeader(MBHeader.MQTTTOPIC_STRING.toString(),
                                                                   Constants.REQUEST_TOPIC)
                                                       .withHeader(MBHeader.REPLYTOTOPIC_STRING.toString(),
                                                                   Constants.RESPONSE_TOPIC)
                                                       .withHeader(MBHeader.CORRELATIONID_STRING.toString(),
                                                                   correlationID)
                                                       .withHeader(MBHeader.REMOTEOPERATION_STRING.toString(),
                                                                   RemoteOperations.invokeDiscovery)
                                                       .build();

        // publish the exchange over the camel route
        final Thread thread = new Thread(() -> {

            // By using an extra thread and waiting some time before sending the request, the
            // consumer can be started in time to avoid loosing replies.
            try {
                Thread.sleep(300);
            }
            catch (final InterruptedException e) {
            }

            Activator.producer.send("direct:SendMQTT", request);
        });
        thread.start();

        final String callbackEndpoint = "direct:Callback-" + correlationID;
        LOG.debug("Waiting for response at endpoint: {}", callbackEndpoint);

        // wait for a response and consume it or timeout after 30s
        final ConsumerTemplate consumer = Activator.camelContext.createConsumerTemplate();
        final Exchange response = consumer.receive(callbackEndpoint, 30000);

        // release resources
        try {
            consumer.stop();
        }
        catch (final Exception e) {
            LOG.warn("Unable to stop consumer: {}", e.getMessage());
        }

        if (Objects.nonNull(response)) {
            LOG.debug("Received a response in time.");

            // read the deployment location from the response
            return Optional.ofNullable(response.getIn().getHeader(MBHeader.DEPLOYMENTLOCATION_STRING.toString(),
                                                                  String.class));
        } else {
            LOG.warn("No response received within the timeout interval.");
            return Optional.empty();
        }
    }

    /**
     * Creates a DiscoveryRequest containing the ServiceTemplate fragment on which the given
     * NodeTemplateInstance is hosted. To create the fragment, the hostedOn RelationshipTemplate
     * path is followed from the given NodeTemplateInstance to the bottom one. For all found
     * NodeTemplateInstance objects the properties, the id, the type and the id of the
     * NodeTemplateInstance on which it is hosted are stored.
     *
     * @param node the NodeTemplateInstance for which the deployment distribution decision has to be
     *        made
     * @return a discovery request for the given NodeTemplateInstance which can be used by the
     *         discovery plug-ins
     */
    private static DiscoveryRequest createDiscoveryRequest(NodeTemplateInstance node) {
        final DiscoveryRequest discoveryRequest = new DiscoveryRequest(new ServiceTemplateFragmentType());
        final List<NodeTemplate> nodeTemplates = discoveryRequest.getServiceTemplateFragment().getNodeTemplate();

        // create ServiceTemplate fragment consisting of all NodeTemplates from the given
        // NodeTemplate downwards connected via hostedOn RelationshipTemplates
        List<RelationshipTemplateInstance> outgoingHostedOn = getOutgoingHostedOn(node);
        NodeTemplateInstance upperNode;
        while (!outgoingHostedOn.isEmpty()) {
            upperNode = node;
            node = outgoingHostedOn.get(0).getTarget();

            // create NodeTemplate corresponding to the upperNode NodeTemplateInstance
            nodeTemplates.add(new NodeTemplate(parsePropertiesToKeyValueMap(upperNode.getPropertiesAsMap()),
                upperNode.getTemplateId(), upperNode.getTemplateType(), node.getTemplateId()));

            outgoingHostedOn = getOutgoingHostedOn(node);
        }

        // create bottom NodeTemplate of the fragment
        nodeTemplates.add(new NodeTemplate(parsePropertiesToKeyValueMap(node.getPropertiesAsMap()),
            node.getTemplateId(), node.getTemplateType(), null));

        return discoveryRequest;
    }

    /**
     * Parse properties of type Map to the KeyValueMap type of the collaboration model.
     *
     * @param properties the properties as Map
     * @return the properties as KeyValueMap
     */
    private static KeyValueMap parsePropertiesToKeyValueMap(final Map<String, String> properties) {
        final KeyValueMap kvProperties = new KeyValueMap();
        if (Objects.nonNull(properties)) {
            final List<KeyValueType> propertyList = kvProperties.getKeyValuePair();
            properties.entrySet()
                      .forEach((entry) -> propertyList.add(new KeyValueType(entry.getKey(), entry.getValue())));
        } else {
            LOG.warn("Properties are null. Unable to parse them.");
        }
        return kvProperties;
    }

    /**
     * Get all outgoing hostedOn RelationshipTemplateInstances of a NodeTemplateInstance.
     *
     * @param node the NodeTemplateInstance to check
     * @return a list with all outgoing RelationshipTemplateInstances
     */
    private static List<RelationshipTemplateInstance> getOutgoingHostedOn(final NodeTemplateInstance node) {
        return node.getOutgoingRelations().stream()
                   .filter(rel -> rel.getTemplateType().equals(Types.hostedOnRelationType))
                   .collect(Collectors.toList());
    }

    /**
     * Search for a discovery plug-in that can handle the given request.
     *
     * @param request the request containing all information needed to check if discovery can be
     *        performed by a plug-in
     * @return an optional containing the plug-in if a suited is found, an empty optional otherwise
     */
    public static Optional<IManagementBusDiscoveryPluginService> getDiscoveryPlugin(final DiscoveryRequest request) {
        for (final IManagementBusDiscoveryPluginService plugin : ServiceHandler.discoveryPluginServices) {
            if (plugin.canHandle(request)) {
                return Optional.of(plugin);
            }
        }
        return Optional.empty();
    }
}
