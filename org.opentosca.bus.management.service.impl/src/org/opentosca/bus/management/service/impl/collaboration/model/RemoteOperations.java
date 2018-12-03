package org.opentosca.bus.management.service.impl.collaboration.model;

/**
 * Enum which contains all possible operations which can currently be requested on other OpenTOSCA
 * Containers by sending a collaboration message via MQTT.<br>
 * <br>
 *
 * Copyright 2018 IAAS University of Stuttgart <br>
 * <br>
 */
public enum RemoteOperations {

    /**
     * Requests the conduct of device/service discovery for the NodeType and properties contained in
     * the collaboration message.
     */
    invokeDiscovery,

    /**
     * Requests the deployment of a certain IA.
     */
    invokeIADeployment,

    /**
     * Requests the undeployment of a certain IA.
     */
    invokeIAUndeployment,

    /**
     * Requests the operation on a deployed IA.
     */
    invokeIAOperation
}
