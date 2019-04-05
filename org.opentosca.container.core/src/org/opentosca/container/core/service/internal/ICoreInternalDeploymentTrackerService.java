package org.opentosca.container.core.service.internal;

import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.model.deployment.process.DeploymentProcessState;

/**
 * Interface that provides methods for storing and getting the deployment states of CSAR files, IAs
 * and Plans.
 */
public interface ICoreInternalDeploymentTrackerService {

    /**
     * Stores the deployment state of a CSAR file.
     *
     * @param csarID that uniquely identifies a CSAR file
     * @param deploymentState to store
     * @return <code>true</code> if storing was successful, otherwise <code>false</code>
     */
    public boolean storeDeploymentState(CSARID csarID, DeploymentProcessState deploymentState);

    /**
     * @param csarID that uniquely identifies a CSAR file
     * @return the deployment state of the CSAR file; if CSAR file doesn't exist <code>null</code>
     */
    public DeploymentProcessState getDeploymentState(CSARID csarID);

    /**
     * Deletes all deployment information for the given CSAR id
     *
     * @param csarID the CSAR id whose deployment state should be deleted
     */
    public void deleteDeploymentState(CSARID csarID);

}
