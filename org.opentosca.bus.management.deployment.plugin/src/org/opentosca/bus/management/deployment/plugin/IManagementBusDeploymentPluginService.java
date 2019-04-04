package org.opentosca.bus.management.deployment.plugin;

import java.util.List;

import org.apache.camel.Exchange;
import org.opentosca.bus.management.header.MBHeader;

/**
 * Interface of the Management Bus Deployment Plug-ins.<br>
 * <br>
 *
 * Copyright 2019 IAAS University of Stuttgart <br>
 * <br>
 *
 * The interface specifies four methods. One for invoking the deployment of an Implementation
 * Artifact or plan, another for invoking the undeployment of a previously deployed Implementation
 * Artifact or plan and two methods that return the supported deployment types and the capabilities
 * of the specific plug-in.
 */
public interface IManagementBusDeploymentPluginService {

    /**
     * Invokes the deployment of an Implementation Artifact or plan into the suited environment.
     *
     * @param exchange contains all needed information for the deployment as header fields.
     *
     * @return the endpoint of the deployed Implementation Artifact or plan as header field (see
     *         {@link MBHeader#ENDPOINT_URI}) of the exchange message or null if the deployment
     *         failed.
     *
     */
    public Exchange invokeDeployment(Exchange exchange);

    /**
     * Invokes the undeployment of an Implementation Artifact or plan.
     *
     * @param exchange contains all needed information like the endpoint of the deployed
     *        Implementation Artifact or plan.
     *
     * @return the result of the undeployment process as header field (see
     *         {@link MBHeader#OPERATIONSTATE_BOOLEAN}) of the exchange message.
     *
     */
    public Exchange invokeUndeployment(Exchange exchange);

    /**
     * Returns the supported deployment-types of the plug-in such as the Artifact Types of IAs or
     * languages of plans.
     *
     * @return list of strings each representing one supported deployment type of the plug-in.
     *
     */
    public List<String> getSupportedTypes();

    /**
     * Returns the provided capabilities of the plug-in.
     *
     * @return list of strings each representing one capability of the plug-in.
     *
     */
    public List<String> getCapabilties();
}
