/**
 *
 */
package org.opentosca.container.engine.plan;

import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.tosca.model.TPlan;

/**
 * This interface defines highlevel methods for deploying, undeploying of plans specified in the
 * Topology and Orchestration Specification for Cloud Applications
 *
 * A plan is an object of class TPlan, which (with a given id specifying the CSAR, where the plan/s
 * is/are declared) has to be deployable to a fitting execution engine (e.g. WS-BPEL 2.0 on Apache
 * ODE).
 *
 * Examples:
 * <li>If the plan is a WS-BPEL 2.0 Process and the implementation of this interface provides access
 * to a workflow engine (e.g. WSO2 BPS), which can execute it, the plan must be packaged beforehand
 * that it could be deployed by hand on it.
 * <li>If the plan is a bash script, the implementation must provide a way to execute the script on
 * an appropriate environment (e.g. linux server)
 */
public interface IPlanEngineService {

    /**
     * Deploys the given TPlan
     *
     *
     * @param plan The TPlan to deploy
     * @param targetNamespace the namespace of the given plan element. It must come either from a
     *        wrapping Plans element (targetNamespace attribute) or the ServiceTemplate itself
     * @param csarId The id of CSAR file where this TPlan is contained
     * @return true if deployment was successful, else false
     */
    public boolean deployPlan(TPlan plan, String targetNamespace, CSARID csarId);

    /**
     * Undeploys the given TPlan
     *
     * @param plan The TPlan to undeploy
     * @param planTargetNamespace the namespace of the given plan element. It must come either from
     *        a wrapping Plans element (targetNamespace attribute) or the ServiceTemplate itself
     * @param csarId The id of CSAR file where this TPlan is contained
     * @return true if undeployment was successful, else false
     */
    public boolean undeployPlan(TPlan plan, String targetNamspace, CSARID csarId);

}
