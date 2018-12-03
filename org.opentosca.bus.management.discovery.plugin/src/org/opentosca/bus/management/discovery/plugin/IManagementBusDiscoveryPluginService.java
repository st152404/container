package org.opentosca.bus.management.discovery.plugin;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Interface of the Management Bus Discovery Plug-ins.<br>
 * <br>
 *
 * Copyright 2018 IAAS University of Stuttgart <br>
 * <br>
 *
 * This interface defines two methods. One can be used to get the QNames of all NodeTypes which can
 * be discovered by a certain plug-in. The other method is intended to start the device/service
 * discovery for the supported NodeTypes at the local OpenTOSCA Container node. The detected
 * devices/services are compared with the given Properties of a NodeTemplate to determine if this
 * NodeTemplate is managed by the local OpenTOSCA Container node.
 *
 */
public interface IManagementBusDiscoveryPluginService {

    /**
     * Invokes the discovery of available devices and services to check if a matching one for the
     * given NodeTemplate can be found.
     *
     * @param nodeType the NodeType of the NodeTemplate to discover
     * @param properties the Properties of the NodeTemplate to discover
     * @return <tt>true</tt> if a matching device or service is found, <tt>false</tt> otherwise.
     */
    public boolean invokeNodeTemplateDiscovery(QName nodeType, Map<String, String> properties);

    /**
     * Returns the supported NodeTypes of the plug-in.
     *
     */
    public List<QName> getSupportedNodeTypes();
}
