package org.opentosca.planbuilder.tests.plugin.scripttest.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentosca.planbuilder.model.tosca.AbstractImplementationArtifact;
import org.opentosca.planbuilder.model.tosca.AbstractInterface;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTypeImplementation;
import org.opentosca.planbuilder.model.tosca.AbstractOperation;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.plugins.IPlanBuilderTestPlugin;
import org.opentosca.planbuilder.plugins.context.PlanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScriptTestPlugin<T extends PlanContext> implements IPlanBuilderTestPlugin<T> {

	final static Logger LOGGER = LoggerFactory.getLogger(ScriptTestPlugin.class);

	private static final String PLUGIN_ID = "Script Test Policy Plugin";
	protected static final String RUN_SCRIPT_OPERATION_NAME = "runScript";
	private static final String[] SUPPORTED_ARTIFACT_TYPES = { "ScriptArtifact" };

	@Override
	public String getID() {
		return PLUGIN_ID;
	}

	@Override
	public boolean canHandle(AbstractNodeTemplate nodeTemplate, AbstractPolicy testPolicy) {
		final AbstractNodeTemplate infrastructureNode = findRunScriptInfrastructureNode(nodeTemplate);
		final AbstractImplementationArtifact ia = findScriptTestIA(nodeTemplate, testPolicy);

		if (infrastructureNode != null && ia != null && isSupportedIAType(ia)) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the TestIA for the TestPolicy
	 *
	 * @param nodeTemplate
	 * @param testPolicy
	 * @return null if not found
	 */
	protected static AbstractImplementationArtifact findScriptTestIA(AbstractNodeTemplate nodeTemplate,
			AbstractPolicy testPolicy) {
		final List<AbstractNodeTypeImplementation> nodeImpls = nodeTemplate.getImplementations();
		for (final AbstractNodeTypeImplementation nodeImpl : nodeImpls) {
			final List<AbstractImplementationArtifact> nodeIAs = nodeImpl.getImplementationArtifacts();
			for (final AbstractImplementationArtifact nodeIA : nodeIAs) {
				final String nodeIAOperationName = nodeIA.getOperationName();
				if (nodeIAOperationName != null && nodeIAOperationName.length() > 0
						&& testPolicy.getType().getName().toLowerCase().startsWith(nodeIAOperationName.toLowerCase())) {
					return nodeIA;
				}
			}
		}
		return null;

	}

	/**
	 * Finds the closest InfrastructureNode which offers a runScript method to
	 * execute the script test on
	 *
	 * @param nodeTemplate
	 * @return null if not found
	 */
	protected static AbstractNodeTemplate findRunScriptInfrastructureNode(AbstractNodeTemplate nodeTemplate) {
		final Set<AbstractNodeTemplate> toCheck = new HashSet<>();
		final Set<AbstractNodeTemplate> toCheckNextIteration = new HashSet<>();
		toCheck.add(nodeTemplate);
		while (true) {
			for (final AbstractNodeTemplate node : toCheck) {
				final AbstractOperation runScriptOperation = findRunScriptOperation(node);
				if (runScriptOperation != null) {
					// node has a runScript Operation
					return node;
				}
				final List<AbstractRelationshipTemplate> relations = node.getOutgoingRelations();
				for (final AbstractRelationshipTemplate relation : relations) {
					toCheckNextIteration.add(relation.getTarget());
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

	/**
	 * Returns the runScript Operation of a nodeTemplate
	 *
	 * @param nodeTemplate
	 * @return null if the nodeTemplate has no runScript Operation
	 */
	protected static AbstractOperation findRunScriptOperation(AbstractNodeTemplate nodeTemplate) {
		final List<AbstractInterface> interfaces = nodeTemplate.getType().getInterfaces();
		// add interfaces of super-node
		if (nodeTemplate.getType().getTypeRef() != null) {
			interfaces.addAll(nodeTemplate.getType().getTypeRef().getInterfaces());
		}
		for (final AbstractInterface nodeInterface : interfaces) {
			final List<AbstractOperation> operations = nodeInterface.getOperations();
			for (final AbstractOperation operation : operations) {
				if (operation.getName().equalsIgnoreCase(RUN_SCRIPT_OPERATION_NAME)) {
					return operation;
				}
			}
		}
		return null;
	}

	/**
	 * Returns true iff the iaType is supported by this plugin
	 *
	 * @param ia
	 * @return
	 */
	protected boolean isSupportedIAType(AbstractImplementationArtifact ia) {
		final String iaType = ia.getArtifactType().getLocalPart();
		for (final String supportedType : SUPPORTED_ARTIFACT_TYPES) {
			if (iaType.equalsIgnoreCase(supportedType)) {
				return true;
			}
		}
		LOGGER.warn("Found IA of Type {} but Plugin only supports {}", iaType, SUPPORTED_ARTIFACT_TYPES);
		return false;
	}

}
