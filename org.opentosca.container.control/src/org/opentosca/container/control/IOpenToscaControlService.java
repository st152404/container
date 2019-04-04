package org.opentosca.container.control;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.xml.namespace.QName;

import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.model.deployment.process.DeploymentProcessState;
import org.opentosca.container.core.tosca.extension.TPlanDTO;

/**
 * Interface of the control of the OpenTosca Container.<br>
 * <br>
 *
 * The instance of this interface is used by org.opentosca.container.api which invokes each step in
 * the deployment process. For handling the states of processing of each CSAR, this component uses
 * the org.opentosca.core.deployment.tracker.service.ICoreDeploymentTrackerService to read and set
 * the current state of a certain CSAR and provides a HashSet with the possible process invocations
 * for a certain CSAR.
 */
public interface IOpenToscaControlService {

    /**
     * This method invokes the processing of the TOSCA content of a certain CSAR.
     *
     * @param csarID ID which uniquely identifies a CSAR file.
     * @return Returns true for success, false for one or more errors.
     */
    public Boolean invokeTOSCAProcessing(CSARID csarID);

    /**
     * This method deletes the stored contents of a certain CSAR inside of the container.
     *
     * @param csarID the ID of the CSAR which shall be deleted.
     * @return List of errors, if list is empty, no error occurred
     */
    public List<String> deleteCSAR(CSARID csarID);

    /**
     * Sets the deployment state of a CSAR to STORED.
     *
     * @param csarID ID which uniquely identifies a CSAR file.
     * @return Returns true, if setting was successful, otherwise false.
     */
    public Boolean setDeploymentProcessStateStored(CSARID csarID);

    /**
     * Returns the current state of a deployment process of a CSAR.
     *
     * @param csarID ID which uniquely identifies a CSAR file.
     * @return Returns true for success, false for one or more errors.
     */
    public DeploymentProcessState getDeploymentProcessState(CSARID csarID);

    /**
     * Invokes the a process described due the parameter PublicPlan for the given CSAR.
     *
     * @param csarID the ID of the CSAR
     *
     * @param serviceTemplateInstanceID the instance id, or -1 if the plan is a build plan
     * @param plan which containes the data which with the process is invoked (including the message
     *        values).
     * @return
     * @throws UnsupportedEncodingException
     */
    public String invokePlanInvocation(CSARID csarID, QName serviceTemplateId, long serviceTemplateInstanceID,
                                       TPlanDTO plan) throws UnsupportedEncodingException;
}
