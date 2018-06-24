package org.opentosca.planbuilder.plugins;

import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.plugins.context.PlanContext;

public interface IPlanBuilderTestPlugin<T extends PlanContext> extends IPlanBuilderPlugin {

	/**
	 * Handles the execution of a test defined as a test policy on a node
	 *
	 * @param planContext
	 *            the context of the plan the plugin should be used for
	 * @param nodeTemplate
	 * @param policyTemplate
	 * @return true iff successful
	 */
	public boolean handle(T planContext, AbstractNodeTemplate nodeTemplate, AbstractPolicy testPolicyTemplate);

	/**
	 * Returns whether the plugin can handle the execution of a specific test policy
	 * on a specific node
	 *
	 * @param nodeTemplate
	 * @param testPolicyTemplate
	 * @return true iff plugin can handle the nodeTemplate/testPolicy combination
	 */
	public boolean canHandle(AbstractNodeTemplate nodeTemplate, AbstractPolicy testPolicyTemplate);

}
