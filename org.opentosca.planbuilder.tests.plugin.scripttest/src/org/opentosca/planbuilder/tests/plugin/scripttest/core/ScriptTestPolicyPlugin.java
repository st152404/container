package org.opentosca.planbuilder.tests.plugin.scripttest.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentosca.planbuilder.core.plugins.IPlanBuilderTestPolicyPlugin;
import org.opentosca.planbuilder.core.plugins.context.PlanContext;
import org.opentosca.planbuilder.model.tosca.AbstractInterface;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractOperation;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScriptTestPolicyPlugin<T extends PlanContext> implements IPlanBuilderTestPolicyPlugin<T> {

	final static Logger LOGGER = LoggerFactory.getLogger(ScriptTestPolicyPlugin.class);

	private static final String PLUGIN_ID = "Script Test Policy Plugin";
	public static final String RUN_SCRIPT_OPERATION_NAME = "runScript";

	@Override
	public String getID() {
		return PLUGIN_ID;
	}

	@Override
	public boolean canHandle(AbstractNodeTemplate nodeTemplate, AbstractPolicy testPolicyTemplate) {

		return true;
	}

	protected AbstractNodeTemplate findRunScriptInfrastructureNode(AbstractNodeTemplate nodeTemplate) {
		final Set<AbstractNodeTemplate> toCheck = new HashSet<>();
		final Set<AbstractNodeTemplate> toCheckNextIteration = new HashSet<>();
		toCheck.add(nodeTemplate);
		while (true) {
			for (final AbstractNodeTemplate node : toCheck) {
				final List<AbstractInterface> interfaces = node.getType().getInterfaces();
				for (final AbstractInterface nodeInterface : interfaces) {
					final List<AbstractOperation> operations = nodeInterface.getOperations();
					for (final AbstractOperation operation : operations) {
						if (operation.getName().equals(RUN_SCRIPT_OPERATION_NAME)) {
							return node;
						}
					}
				}
				final List<AbstractRelationshipTemplate> relations = node.getIngoingRelations();
				for (final AbstractRelationshipTemplate relation : relations) {
					toCheckNextIteration.add(relation.getSource());
				}
			}
			toCheck.clear();
			toCheck.addAll(toCheckNextIteration);
			toCheckNextIteration.clear();
			if (toCheck.isEmpty()) {
				LOGGER.error("No node to execute the test script on was found");
				return null;
			}
		}
	}

}
