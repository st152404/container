package org.opentosca.bus.management.invocation.plugin;

import java.util.List;

import org.apache.camel.Exchange;

/**
 * Interface of the Management Bus Invocation Plug-ins.<br>
 * <br>
 *
 * Copyright 2019 IAAS University of Stuttgart <br>
 * <br>
 *
 * The interface specifies two methods. One for invoking a service like an operation of an
 * Implementation Artifact or a plan and one method that returns the supported invocation-types of
 * the plug-in.
 */
public interface IManagementBusInvocationPluginService {

    /**
     * Invokes a service like an Implementation Artifact or a plan.
     *
     * @param exchange contains all needed information like endpoint of the service, the operation
     *        to invoke and the data to be transferred.
     *
     * @return the response of the invoked service as body of the exchange message.
     *
     */
    public Exchange invoke(Exchange exchange);

    /**
     * Returns the supported invocation-types of the plug-in.
     *
     */
    public List<String> getSupportedTypes();

}
