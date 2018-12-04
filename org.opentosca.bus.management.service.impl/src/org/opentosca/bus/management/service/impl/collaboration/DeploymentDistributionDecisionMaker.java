package org.opentosca.bus.management.service.impl.collaboration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.opentosca.bus.management.discovery.plugin.IManagementBusDiscoveryPluginService;
import org.opentosca.bus.management.header.MBHeader;
import org.opentosca.bus.management.service.impl.Activator;
import org.opentosca.bus.management.service.impl.collaboration.model.BodyType;
import org.opentosca.bus.management.service.impl.collaboration.model.CollaborationMessage;
import org.opentosca.bus.management.service.impl.collaboration.model.DiscoveryRequest;
import org.opentosca.bus.management.service.impl.collaboration.model.KeyValueMap;
import org.opentosca.bus.management.service.impl.collaboration.model.KeyValueType;
import org.opentosca.bus.management.service.impl.collaboration.model.RemoteOperations;
import org.opentosca.bus.management.service.impl.servicehandler.ServiceHandler;
import org.opentosca.container.core.common.Settings;
import org.opentosca.container.core.next.model.NodeTemplateInstance;
import org.opentosca.container.core.next.model.RelationshipTemplateInstance;
import org.opentosca.container.core.next.repository.NodeTemplateInstanceRepository;
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
 * To determine the responsible OpenTOSCA Container, a device/service discovery at the distributed
 * Containers is performed. Therefore, the infrastructure NodeTemplateInstance of the topology stack
 * of the IA is retrieved. Afterwards, its NodeType and properties are used to detect a matching
 * device/service locally. If this is not successful, the discovery is distributed to other
 * Containers via MQTT. In case there is also no matching device/service, the local Container is
 * used as default deployment location.<br>
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

    // repository to access instance data via NodeTemplate identifiers
    private final static NodeTemplateInstanceRepository nodeTemplateInstanceRepository =
        new NodeTemplateInstanceRepository();

    /**
     * Get the deployment location for IAs which are attached to the NodeTemplateInstance. If the
     * collaboration mode is turned on, this method performs a device/service discovery to determine
     * the deployment location. Therefore, the infrastructure NodeTemplateInstance is searched in
     * the topology. Afterwards, its NodeType and properties are used to detect a matching
     * device/service locally and remotely. If the matching is not successful, the local OpenTOSCA
     * Container is returned as default deployment location.
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

                // get infrastructure NodeTemplate
                final NodeTemplateInstance infrastructureNodeTemplateInstance =
                    searchInfrastructureNode(nodeTemplateInstance);

                LOG.debug("Infrastructure NodeTemplateInstance has ID: {} and NodeType: {}",
                          infrastructureNodeTemplateInstance.getId(),
                          infrastructureNodeTemplateInstance.getTemplateType());

                // check if "managingContainer" is already set for the infrastructure
                // NodeTemplateInstance
                if (infrastructureNodeTemplateInstance.getManagingContainer() != null) {
                    LOG.debug("Infrastructure NodeTemplateInstance has set managingContainer attribute.");

                    // no instance data matching needed, as it was already performed for the
                    // infrastructure NodeTemplateInstance
                    final String managingContainer = infrastructureNodeTemplateInstance.getManagingContainer();

                    LOG.debug("Result of deployment distribution decision: {}", managingContainer);
                    return managingContainer;
                } else {
                    final String deploymentLocation;
                    final QName infrastructureNodeType = infrastructureNodeTemplateInstance.getTemplateType();

                    // if there is no discovery plug-in for the type, local deployment has to be
                    // performed
                    if (ServiceHandler.discoveryPluginServices.containsKey(infrastructureNodeType)) {
                        final IManagementBusDiscoveryPluginService plugin =
                            ServiceHandler.discoveryPluginServices.get(infrastructureNodeType);

                        final Map<String, String> infrastructureProperties =
                            infrastructureNodeTemplateInstance.getPropertiesAsMap();

                        // perform local device/service discovery
                        if (plugin.invokeNodeTemplateDiscovery(infrastructureNodeType, infrastructureProperties)) {
                            LOG.debug("Local discovery was successful. Performing local deployment...");
                            deploymentLocation = Settings.OPENTOSCA_CONTAINER_HOSTNAME;
                        } else {
                            // no matching local device/service --> perform remote discovery
                            LOG.debug("Local discovery was not successful. Performing remote discovery...");
                            final Optional<String> deploymentLocationOptional =
                                performRemoteDiscovery(infrastructureNodeType, infrastructureProperties);

                            if (deploymentLocationOptional.isPresent()) {
                                deploymentLocation = deploymentLocationOptional.get();
                                LOG.debug("Found matching device/service remotely. Deployment will be done on OpenTOSCA Container with host name: {}",
                                          deploymentLocation);
                            } else {
                                LOG.warn("Remote discovery was not successful. Performing local deployment as default.");
                                deploymentLocation = Settings.OPENTOSCA_CONTAINER_HOSTNAME;
                            }
                        }
                    } else {
                        LOG.warn("No discovery plug-in for this NodeType available. Performing local deployment.");
                        deploymentLocation = Settings.OPENTOSCA_CONTAINER_HOSTNAME;
                    }

                    // set property to speed up future distribution decisions
                    infrastructureNodeTemplateInstance.setManagingContainer(deploymentLocation);
                    nodeTemplateInstanceRepository.update(infrastructureNodeTemplateInstance);

                    return deploymentLocation;
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
     * Search for the infrastructure NodeTemplateInstance on which the given NodeTemplateInstance is
     * hosted/deployed/based. In the context of device discovery the infrastructure Node should
     * always be the Node at the bottom of a stack in the topology. If an OpenTOSCA Container
     * manages this bottom Node, it can be used to deploy all IAs attached to Nodes that are above
     * the infrastructure Node in the topology.
     *
     * @param nodeTemplateInstance the NodeTemplateInstance for which the infrastructure is searched
     * @return the infrastructure NodeTemplateInstance
     */
    private static NodeTemplateInstance searchInfrastructureNode(final NodeTemplateInstance nodeTemplateInstance) {
        LOG.debug("Looking for infrastructure NodeTemplate at NodeTemplate {} and below...",
                  nodeTemplateInstance.getTemplateId());

        final Collection<RelationshipTemplateInstance> outgoingRelationships =
            nodeTemplateInstance.getOutgoingRelations();

        // terminate search if bottom NodeTemplate is found
        if (outgoingRelationships.isEmpty()) {
            LOG.debug("NodeTemplate {} is the infrastructure NodeTemplate", nodeTemplateInstance.getTemplateId());
            return nodeTemplateInstance;
        } else {
            LOG.debug("NodeTemplate {} has outgoing RelationshipTemplates...", nodeTemplateInstance.getTemplateId());

            for (final RelationshipTemplateInstance relation : outgoingRelationships) {
                final QName relationType = relation.getTemplateType();
                LOG.debug("Found outgoing RelationshipTemplate of type: {}", relationType);

                // traverse topology stack downwards
                if (isInfrastructureRelationshipType(relationType)) {
                    LOG.debug("Continue search with the target of the RelationshipTemplate...");
                    return searchInfrastructureNode(relation.getTarget());
                } else {
                    LOG.debug("RelationshipType is not valid for infrastructure search (e.g. hostedOn).");
                }
            }
        }

        // if all outgoing relationships are not of the searched types, the NodeTemplate is the
        // bottom one
        return nodeTemplateInstance;
    }

    /**
     * The method sends a request to discover a certain device/service via MQTT to all subscribed
     * OpenTOSCA Container nodes. Afterwards, it waits for a reply which contains the host name of
     * the OpenTOSCA Container that found the device/service locally. If it receives a reply in
     * time, it returns the host name. Otherwise, it returns null.
     *
     * @param infrastructureNodeType the NodeType of the NodeTemplate which has to be discovered
     * @param infrastructureProperties the set of properties of the NodeTemplate which has to be
     *        discovered
     * @return an optional containing the host name of the OpenTOSCA Container which found a
     *         matching device/service if one is found, an empty optional otherwise.
     */
    private static Optional<String> performRemoteDiscovery(final QName infrastructureNodeType,
                                                           final Map<String, String> infrastructureProperties) {

        LOG.debug("Creating collaboration message for remote device/service discovery...");

        // transform infrastructureProperties for the message body
        final KeyValueMap properties = new KeyValueMap();
        final List<KeyValueType> propertyList = properties.getKeyValuePair();
        infrastructureProperties.entrySet().forEach((entry) -> propertyList.add(new KeyValueType(entry.getKey(),
            entry.getValue())));

        // create collaboration message
        final BodyType content = new BodyType(new DiscoveryRequest(infrastructureNodeType, properties));
        final CollaborationMessage collaborationMessage = new CollaborationMessage(new KeyValueMap(), content);

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

        // wait for a response and consume it or timeout after 45s
        final ConsumerTemplate consumer = Activator.camelContext.createConsumerTemplate();
        final Exchange response = consumer.receive(callbackEndpoint, 45000);

        // release resources
        try {
            consumer.stop();
        }
        catch (final Exception e) {
            LOG.warn("Unable to stop consumer: {}", e.getMessage());
        }

        if (response != null) {
            LOG.debug("Received a response in time.");

            // read the deployment location from the reply
            return Optional.ofNullable(response.getIn().getHeader(MBHeader.DEPLOYMENTLOCATION_STRING.toString(),
                                                                  String.class));
        } else {
            LOG.warn("No response received within the timeout interval.");
            return Optional.empty();
        }
    }

    /**
     * Check whether a given Relationship Type is used to connect parts of a topology stack
     * (infrastructure type) or different topology stacks.
     *
     * @param relationType The Relationship Type to check
     * @return <tt>true</tt> if the Relationship Type is hostedOn, deployedOn or dependsOn and
     *         <tt>false</tt> otherwise
     */
    private static boolean isInfrastructureRelationshipType(final QName relationType) {
        return relationType.equals(Types.hostedOnRelationType) || relationType.equals(Types.deployedOnRelationType)
            || relationType.equals(Types.dependsOnRelationType);
    }
}
