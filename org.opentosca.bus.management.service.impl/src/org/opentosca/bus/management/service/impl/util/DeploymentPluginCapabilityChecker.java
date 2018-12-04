package org.opentosca.bus.management.service.impl.util;

import java.util.Iterator;
import java.util.List;

import org.opentosca.bus.management.deployment.plugin.IManagementBusDeploymentPluginService;
import org.opentosca.bus.management.service.impl.servicehandler.ServiceHandler;
import org.opentosca.container.core.model.capability.provider.ProviderType;

/**
 * Analyzes if a given Implementation Artifact is deployable, meaning checking if the required
 * capabilities of the Implementation Artifact are met by the container and/or available plug-ins
 * (plan + deployment).<br>
 * <br>
 *
 * Copyright 2018 IAAS University of Stuttgart
 */

public class DeploymentPluginCapabilityChecker {

    /**
     * Checks if required features are met by chosen plug-in or container and plan.
     *
     * @param requiredFeatures the set of features to be satisfied
     * @param plugin the deployment plug-in
     * @return true if all requiredFeatures are met, false otherwise
     */
    public static boolean capabilitiesAreMet(final List<String> requiredFeatures,
                                             final IManagementBusDeploymentPluginService plugin) {

        if (!requiredFeatures.isEmpty()) {

            // get all provided capabilities
            final List<String> capabilities = getConAndPlanCaps();
            capabilities.addAll(plugin.getCapabilties());

            // remove all required features that are satisfied by a capability
            for (final Iterator<String> itReqCaps = requiredFeatures.iterator(); itReqCaps.hasNext();) {
                final String reqCap = itReqCaps.next();
                if (capabilities.contains(reqCap)) {
                    itReqCaps.remove();
                }
            }
        }

        // return true if no further requested feature exists
        return requiredFeatures.isEmpty();
    }

    /**
     * Returns container and plan capabilities from the CoreCapabilitiyService.
     *
     * @return container and plan capabilities in one merged list.
     */
    private static List<String> getConAndPlanCaps() {

        final List<String> conAndPlanCaps =
            ServiceHandler.capabilityService.getCapabilities(ProviderType.CONTAINER.toString(), ProviderType.CONTAINER);

        ServiceHandler.capabilityService.getCapabilities(ProviderType.PLAN_PLUGIN).values()
                                        .forEach(caps -> conAndPlanCaps.addAll(caps));

        return conAndPlanCaps;
    }
}
