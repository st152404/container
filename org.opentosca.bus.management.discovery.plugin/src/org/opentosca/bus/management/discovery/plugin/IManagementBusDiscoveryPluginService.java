package org.opentosca.bus.management.discovery.plugin;

import org.opentosca.bus.management.collaboration.model.DiscoveryRequest;

/**
 * Interface of the Management Bus Discovery Plug-ins.<br>
 * <br>
 *
 * Copyright 2018 IAAS University of Stuttgart <br>
 * <br>
 *
 * TODO
 *
 */
public interface IManagementBusDiscoveryPluginService {

    /**
     * TODO
     *
     * @param discoveryRequest
     * @return
     */
    public boolean invokeDiscovery(DiscoveryRequest discoveryRequest);

    /**
     * TODO
     *
     * @param discoveryRequest
     * @return
     */
    public boolean canHandle(DiscoveryRequest discoveryRequest);
}
