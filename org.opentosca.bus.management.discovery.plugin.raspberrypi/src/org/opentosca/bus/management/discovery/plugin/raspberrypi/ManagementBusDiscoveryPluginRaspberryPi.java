package org.opentosca.bus.management.discovery.plugin.raspberrypi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.opentosca.bus.management.collaboration.model.DiscoveryRequest;
import org.opentosca.bus.management.collaboration.model.NodeTemplate;
import org.opentosca.bus.management.discovery.plugin.IManagementBusDiscoveryPluginService;
import org.opentosca.bus.management.discovery.plugin.raspberrypi.model.MacIpPair;
import org.opentosca.container.core.tosca.convention.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Management Bus Plug-in for the discovery of RaspberryPi devices.<br>
 * <br>
 *
 * This plug-in is capable of detecting RaspberryPi devices which correspond to topology fragments
 * of a ServiceTemplate. It can handle topology fragments which contain a RaspbianJessie
 * NodeTemplate hostedOn a RaspberryPI3 NodeTemplate. The RaspbianJessie NodeTemplate needs a
 * defined IP property and the RaspberryPI3 NodeTemplate a MAC property for the discovery. If the
 * plug-in detects a device, which has to be accessed to provision a ServiceTemplate, in his local
 * network, the local OpenTOSCA Container is responsible for the operations on this device.
 *
 * Copyright 2018 IAAS University of Stuttgart
 */
public class ManagementBusDiscoveryPluginRaspberryPi implements IManagementBusDiscoveryPluginService {

    static final private Logger LOG = LoggerFactory.getLogger(ManagementBusDiscoveryPluginRaspberryPi.class);

    // properties that have to be present for the discovery
    static final private String MAC_PROPERTY = "MAC";
    static final private String IP_PROPERTY = "IP";

    @Override
    public boolean invokeDiscovery(final DiscoveryRequest discoveryRequest) {
        LOG.debug("Invoking discovery with ManagementBusDiscoveryPluginRaspberryPi plugin");

        final Optional<MacIpPair> opt = getIdentifyingPropertiesFromRequest(discoveryRequest);
        if (opt.isPresent()) {
            final MacIpPair macIp = opt.get();
            LOG.debug("Discovery is performed for Raspbian-PI stack with IP: {} and MAC: {}", macIp.getIp(),
                      macIp.getMac());

            // TODO: perform discovery
            return false;
        } else {
            LOG.error("Unable to find valid NodeTemplates and needed properties in the request!");
            return false;
        }
    }

    @Override
    public boolean canHandle(final DiscoveryRequest discoveryRequest) {
        LOG.debug("Checking if ManagementBusDiscoveryPluginRaspberryPi plugin can handle the discovery");

        if (Objects.isNull(discoveryRequest)) {
            LOG.error("DiscoveryRequest is null");
            return false;
        }

        // check if the fragment from the request contains the needed templates and properties
        return getIdentifyingPropertiesFromRequest(discoveryRequest).isPresent();
    }

    /**
     * Check if the given topology fragment contains a stack with a Raspbian node hosted on a
     * RaspberryPi node. Such a stack is only considered as valid if the properties that are needed
     * for the device discovery are defined. If a valid stack is found, the IP and MAC properties
     * are returned in a special holder object.
     *
     * @param discoveryRequest the request containing the topology fragment to check for the
     *        existence of the needed NodeTemplates and properties
     * @return an optional containing a MacIpPair with the extracted IP and MAC address from the
     *         topology fragment or an empty optional if the topology fragment can not be handled
     */
    private Optional<MacIpPair> getIdentifyingPropertiesFromRequest(final DiscoveryRequest discoveryRequest) {

        if (Objects.nonNull(discoveryRequest.getServiceTemplateFragment())) {
            final List<NodeTemplate> nodeTemplates = discoveryRequest.getServiceTemplateFragment().getNodeTemplate();

            LOG.debug("Received topology fragment with {} NodeTemplates.", nodeTemplates.size());

            // get all RaspbianJessie NodeTemplates with defined IP property
            final List<NodeTemplate> raspbianNodes =
                nodeTemplates.stream().filter(node -> node.getTemplateType().equals(Types.raspbianJessieOSNodeType))
                             .filter(containsProperty(IP_PROPERTY)).collect(Collectors.toList());

            // get all RaspberryPi3 NodeTemplates with defined MAC property which are not hostedOn
            // another node
            final List<NodeTemplate> piNodes =
                nodeTemplates.stream().filter(node -> node.getTemplateType().equals(Types.raspberryPI3NodeType))
                             .filter(containsProperty(MAC_PROPERTY)).filter(pi -> Objects.isNull(pi.getHostedOn()))
                             .collect(Collectors.toList());

            LOG.debug("Topology fragment contains {} RaspbianJessie and {} RaspberryPI3 NodeTemplates with expcected properties.",
                      raspbianNodes.size(), piNodes.size());

            // check if one of the raspbian NodeTemplates is hosted on a pi NodeTemplate
            for (final NodeTemplate rasbian : raspbianNodes) {
                final Optional<String> ipOptional = getProperty(IP_PROPERTY).apply(rasbian);
                if (ipOptional.isPresent()) {

                    final QName hostedOn = rasbian.getHostedOn();
                    for (final NodeTemplate pi : piNodes) {
                        if (pi.getTemplateID().equals(hostedOn)) {

                            final Optional<String> macOptional = getProperty(MAC_PROPERTY).apply(pi);
                            if (ipOptional.isPresent()) {
                                LOG.debug("Found valid Radbian-Pi stack in the topology fragment.");
                                return Optional.of(new MacIpPair(macOptional.get(), ipOptional.get()));
                            }
                        }
                    }
                }
            }
            LOG.debug("No Radbian-Pi stack with needed properties found in the topology fragment.");
        } else {
            LOG.error("DiscoveryRequest contains no ServiceTemplateFragment");
        }

        return Optional.empty();
    }

    /**
     * Returns a functions which takes a NodeTemplate as input and returns the property identified
     * by the given property name from it, if it is present.
     *
     * @param propertyName the name of the property that shall be returned by the function
     * @return the function
     */
    private Function<NodeTemplate, Optional<String>> getProperty(final String propertyName) {
        return node -> node.getProperties().getKeyValuePair().stream()
                           .filter(pair -> pair.getKey().equals(propertyName)).findFirst().map(pair -> pair.getValue());
    }

    /**
     * Creates a predicate which checks if a NodeTemplate contains a property.
     *
     * @param propertyName the name of the property which shall be checked by the predicate
     * @return the predicate
     */
    private Predicate<NodeTemplate> containsProperty(final String propertyName) {
        return node -> node.getProperties().getKeyValuePair().stream()
                           .filter(pair -> pair.getKey().equals(propertyName)).findFirst().isPresent();
    }
}
