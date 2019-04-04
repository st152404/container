package org.opentosca.container.engine.plan;

import java.util.List;

import javax.xml.namespace.QName;

import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.tosca.model.TPlan.PlanModelReference;

/**
 * This is the interface for plugins which handle PlanReference elements inside a Plan element
 * specified in a TOSCA Service Template.<br>
 * <br>
 *
 * The plugin musn't resolve the {@link org.opentosca.model.tosca.TPlan.PlanModelReference}, a
 * service implementing {@link org.opentosca.core.file.service.ICoreFileService} should be called
 * for the raw data.
 */
public interface IPlanEnginePluginService {

    /**
     * <p>
     * Method allows deployment of PlanModelReferences.
     * </p>
     * <p>
     * The reference must be resolved in a service implementing
     * {@link org.opentosca.core.file.service.ICoreFileService}. In addition a service of
     * {@link org.opentosca.core.endpoint.service.ICoreEndpointService} must provide a suitable
     * endpoint.
     * </p>
     *
     *
     * @param planRef the PlanReference element under a Plan element of a ServiceTemplate Definition
     * @param csarId the identifier of the CSAR the PlanReference element belongs to
     * @return true if deployment was successful, else false
     */
    public boolean deployPlanReference(QName planId, PlanModelReference planRef, CSARID csarId);

    /**
     * <p>
     * Method allows undeployment of PlanModelReferences.
     * </p>
     * <p>
     * The reference must be resolved in a service implementing
     * {@link org.opentosca.core.file.service.ICoreFileService}. In addition a service of
     * {@link org.opentosca.core.endpoint.service.ICoreEndpointService} must provide a suitable
     * endpoint.
     * </p>
     *
     *
     * @param planRef the PlanReference element under a Plan element of a ServiceTemplate Definition
     * @param csarId the identifier of the CSAR the PlanReference element belongs to
     * @return true if undeployment was successful, else false
     */
    public boolean undeployPlanReference(QName planId, PlanModelReference planRef, CSARID csarId);

    /**
     * <p>
     * Returns the exact plan language understood by this plugin.
     * </p>
     * <p>
     * Example: if the plugin can process WS-BPEL 2.0 Processes it should return
     * "http://docs.oasis-open.org/wsbpel/2.0/process/executable"
     * <p>
     *
     * @return a string representation of the plan language understood by this plugin
     */
    public String getLanguageUsed();

    /**
     * Returns provided capabilities of this plugin.
     *
     * @return a list of strings denoting the capabilities of this plugin
     */
    public List<String> getCapabilties();
}
