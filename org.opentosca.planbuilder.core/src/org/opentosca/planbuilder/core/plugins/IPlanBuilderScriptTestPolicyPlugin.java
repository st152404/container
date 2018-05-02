package org.opentosca.planbuilder.core.plugins;

import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractPolicyTemplate;

public interface IPlanBuilderScriptTestPolicyPlugin extends IPlanBuilderPlugin {

	/**
	 * Handles the execution of a test defined as a test policy on a node template
	 *
	 * @param nodeTemplate
	 * @param policyTemplate
	 * @return true iff successful
	 */
	public boolean handle(AbstractNodeTemplate nodeTemplate, AbstractPolicyTemplate testPolicyTemplate);

	/**
	 * Returns whether the plugin can handle the execution of a specific test policy
	 * on a node template
	 *
	 * @param nodeTemplate
	 * @param testPolicyTemplate
	 * @return true iff plugin can handle the nodeTemplate/testPolicy combination
	 */
	public boolean canHandle(AbstractNodeTemplate nodeTemplate, AbstractPolicyTemplate testPolicyTemplate);

}
