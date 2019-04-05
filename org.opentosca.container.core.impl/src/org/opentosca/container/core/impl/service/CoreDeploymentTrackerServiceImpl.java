package org.opentosca.container.core.impl.service;

import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.model.deployment.process.DeploymentProcessState;
import org.opentosca.container.core.service.ICoreDeploymentTrackerService;
import org.opentosca.container.core.service.internal.ICoreInternalDeploymentTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation currently acts as a Proxy to the Core Internal Deployment Tracker Service. It
 * can in future be used to modify the incoming parameters to fit another back end interface /
 * implementation.
 *
 * @see ICoreInternalDeploymentTrackerService
 */
public class CoreDeploymentTrackerServiceImpl implements ICoreDeploymentTrackerService {

    ICoreInternalDeploymentTrackerService deploymentTrackerService;

    final private static Logger LOG = LoggerFactory.getLogger(CoreDeploymentTrackerServiceImpl.class);


    @Override
    /**
     * {@inheritDoc}
     *
     * This currently acts as a proxy.
     */
    public boolean storeDeploymentState(final CSARID csarID, final DeploymentProcessState deploymentState) {
        return this.deploymentTrackerService.storeDeploymentState(csarID, deploymentState);
    }

    @Override
    /**
     * {@inheritDoc}
     *
     * This currently acts as a proxy.
     */
    public DeploymentProcessState getDeploymentState(final CSARID csarID) {
        return this.deploymentTrackerService.getDeploymentState(csarID);
    }

    @Override
    public void deleteDeploymentState(final CSARID csarId) {
        this.deploymentTrackerService.deleteDeploymentState(csarId);
    }

    /**
     * Binds the Core Internal Deployment Tracker.
     *
     * @param deploymentTrackerService to bind
     */
    public void bindCoreInternalDeploymentTrackerService(final ICoreInternalDeploymentTrackerService deploymentTrackerService) {
        if (deploymentTrackerService == null) {
            CoreDeploymentTrackerServiceImpl.LOG.error("Can't bind Core Internal Deployment Tracker Service.");
        } else {
            this.deploymentTrackerService = deploymentTrackerService;
            CoreDeploymentTrackerServiceImpl.LOG.debug("Core Internal Deployment Tracker Service bound.");
        }
    }

    /**
     * Unbinds the Core Internal Deployment Tracker.
     *
     * @param deploymentTrackerService to unbind
     */
    public void unbindCoreInternalDeploymentTrackerService(final ICoreInternalDeploymentTrackerService deploymentTrackerService) {
        this.deploymentTrackerService = null;
        CoreDeploymentTrackerServiceImpl.LOG.debug("Core Internal Deployment Tracker Service unbound.");
    }
}
