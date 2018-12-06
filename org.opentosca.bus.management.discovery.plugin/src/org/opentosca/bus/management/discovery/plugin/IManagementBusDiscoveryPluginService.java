package org.opentosca.bus.management.discovery.plugin;

import org.opentosca.bus.management.collaboration.model.DiscoveryRequest;

/**
 * Interface of the Management Bus Discovery Plug-ins.<br>
 * <br>
 *
 * Copyright 2018 IAAS University of Stuttgart <br>
 * <br>
 *
 * The discovery plug-in interface is used if a set of OpenTOSCA Container nodes build a distributed
 * TOSCA runtime. It specifies two methods to help the Container nodes to decide which node is
 * responsible for which parts of an application topology. One method can be used to check whether a
 * certain plug-in can handle a given topology fragment. If this is the case the second method can
 * be called to discover whether a matching device/service can be found in the local network of the
 * OpenTOSCA Container node. Whenever a node finds a matching device/service it is responsible for
 * all operations on it and therefore also for the deployment of the corresponding IAs.
 */
public interface IManagementBusDiscoveryPluginService {

    /**
     * Perform device/service discovery to detect a device or service which corresponds to the
     * topology fragment defined in the DiscoveryRequest.
     *
     * @param discoveryRequest the request containing the topology fragment which has to be
     *        discovered
     * @return <tt>true</tt> if a matching device/service is discovered, <tt>false</tt> otherwise
     */
    public boolean invokeDiscovery(DiscoveryRequest discoveryRequest);

    /**
     * Checks whether this plug-in can handle the given DiscoveryRequest.
     *
     * @param discoveryRequest the request which has to be handled
     * @return <tt>true</tt> if the plug-in can handle the request, <tt>false</tt> otherwise
     */
    public boolean canHandle(DiscoveryRequest discoveryRequest);
}
