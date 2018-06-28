package org.opentosca.planbuilder.tests.plugin.scripttest.bpel;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.model.tosca.AbstractArtifactReference;
import org.opentosca.planbuilder.model.tosca.AbstractImplementationArtifact;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTypeImplementation;
import org.opentosca.planbuilder.model.tosca.AbstractOperation;
import org.opentosca.planbuilder.model.tosca.AbstractParameter;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.plugins.context.Variable;
import org.opentosca.planbuilder.provphase.plugin.invoker.bpel.BPELInvokerPlugin;
import org.opentosca.planbuilder.tests.plugin.scripttest.core.ScriptTestPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BPELScriptTestPlugin extends ScriptTestPlugin<BPELPlanContext> {

	private static final Logger LOGGER = LoggerFactory.getLogger(BPELScriptTestPlugin.class);
	private final BPELInvokerPlugin invokerPlugin = new BPELInvokerPlugin();
	private static final String CSAR_STORE_URL = "http://localhost:1337/csars/";
	private static final String CONTAINER_MANAGEMENT_INTERFACE = "ContainerManagementInterface";
	private static final String OPERATING_SYSTEM_INTERFACE = "OperatingSystemInterface";

	@Override
	public boolean handle(BPELPlanContext bpelPlanContext, AbstractNodeTemplate nodeTemplate,
			AbstractPolicy testPolicy) {
		final AbstractNodeTemplate runScriptNode = findRunScriptInfrastructureNode(nodeTemplate);
		final AbstractImplementationArtifact scriptArtifact = findScriptTestIA(nodeTemplate, testPolicy);
		final Map<String, String> policyTemplateProps = testPolicy.getTemplate().getProperties().asMap();

		final List<AbstractArtifactReference> references = scriptArtifact.getArtifactRef().getArtifactReferences();

		if (references.size() > 1) {
			LOGGER.warn("More than one IA referenced for Node {} and Policy {}", nodeTemplate.getName(),
					testPolicy.getName());
			return false;
		}
		final String testScriptReference = references.get(0).getReference();

		// crawl the script content from the container api
		final String csarName = bpelPlanContext.getCSARFileName();
		String scriptFileContent = retrieveScriptContent(csarName, testScriptReference);

		// replace all variables in the string by their value from the policy template
		for (final String key : policyTemplateProps.keySet()) {
			if (policyTemplateProps.get(key) != null) {
				scriptFileContent = scriptFileContent.replace("$" + key, policyTemplateProps.get(key));
			}
		}

		// Find all input params for the runScript Operation
		final Map<String, String> inputParamsMap = new HashMap<>();
		final AbstractOperation runScriptOperation = findRunScriptOperation(runScriptNode);
		for (final AbstractParameter parameter : runScriptOperation.getInputParameters()) {
			if (parameter.getName().equals("Script")) {
				// put script content
				inputParamsMap.put(parameter.getName(), scriptFileContent);
			} else {
				// has to be retrieved later
				inputParamsMap.put(parameter.getName(), null);
			}
		}

		// find all output params for the runScript Operation
		final Map<String, String> outputParamsMap = new HashMap<>();
		for (final AbstractParameter parameter : runScriptOperation.getOutputParameters()) {
			outputParamsMap.put(parameter.getName(), null);
		}

		// create the maps from the params that the invoker needs
		final Map<AbstractParameter, Variable> inputPar2VarMap = createParam2VarMapFromProps(inputParamsMap,
				runScriptNode, bpelPlanContext);
		final Map<AbstractParameter, Variable> outputPar2VarMap = createParam2VarMapFromProps(outputParamsMap,
				runScriptNode, bpelPlanContext);

		final AbstractImplementationArtifact runScriptIA = findRunScriptIA(runScriptNode);

		// TODO: This is a hack to replace the nodeTemplate of the plan context so the
		// invoker invokes the right node
		final AbstractNodeTemplate originalNodeTemplate = bpelPlanContext.getNodeTemplate();
		bpelPlanContext.setNodeTemplate(runScriptNode);
		this.invokerPlugin.handle(bpelPlanContext, runScriptOperation, runScriptIA, inputPar2VarMap, outputPar2VarMap);
		bpelPlanContext.setNodeTemplate(originalNodeTemplate);

		return true;
	}

	/**
	 * Finds the IA that implements the runScript Operation
	 *
	 * @param runScriptNode
	 * @return
	 */
	private AbstractImplementationArtifact findRunScriptIA(AbstractNodeTemplate runScriptNode) {
		final List<AbstractNodeTypeImplementation> nodeImplementations = runScriptNode.getImplementations();
		for (final AbstractNodeTypeImplementation nodeImpl : nodeImplementations) {
			final List<AbstractImplementationArtifact> nodeIAs = nodeImpl.getImplementationArtifacts();
			for (final AbstractImplementationArtifact nodeIA : nodeIAs) {
				final String nodeOperationName = nodeIA.getOperationName();
				if (nodeOperationName == null) {
					final String nodeInterfaceName = nodeIA.getInterfaceName();
					if (nodeInterfaceName.equalsIgnoreCase(CONTAINER_MANAGEMENT_INTERFACE)
							|| nodeInterfaceName.equalsIgnoreCase(OPERATING_SYSTEM_INTERFACE)) {
						// nodeIA implements interface containing runScript Operation
						return nodeIA;
					}
				} else if (nodeOperationName.equalsIgnoreCase(RUN_SCRIPT_OPERATION_NAME)) {
					// nodeIA implements runScript Operation
					return nodeIA;
				}
			}
		}
		return null;
	}

	/**
	 * Creates a map for the invoker from a common <String, String> parameter map
	 *
	 * @param props
	 * @param runScriptNode
	 * @return
	 */
	private static Map<AbstractParameter, Variable> createParam2VarMapFromProps(Map<String, String> props,
			AbstractNodeTemplate runScriptNode, BPELPlanContext planContext) {
		final Map<AbstractParameter, Variable> param2VarMap = new HashMap<>();
		for (final String key : props.keySet()) {
			final AbstractParameter par = new AbstractParameter() {

				@Override
				public boolean isRequired() {
					return false;
				}

				@Override
				public String getType() {
					return "xsd:string";
				}

				@Override
				public String getName() {
					return key;
				}
			};
			Variable var;
			if (key.equals("Script") || key.equals("ScriptResult")) {
				// needs a new variable
				var = planContext.createGlobalStringVariable(runScriptNode.getName() + "_" + key, props.get(key));
			} else {
				// retrieve from existing properties
				var = planContext.getPropertyFromAllNodeTemplates(key);
			}
			if (var == null) {
				var = new Variable(runScriptNode.getId(), "prop_" + runScriptNode.getId() + "_" + key);
			}
			param2VarMap.put(par, var);
		}
		return param2VarMap;
	}

	/**
	 * Retrieves the content of the referenced file
	 *
	 * @param relativePath
	 *            relative to [CSARNAME]/content/
	 * @return
	 */
	private static String retrieveScriptContent(String csarName, String relativePath) {
		String script = null;
		try {
			final URL url = new URL(CSAR_STORE_URL + csarName + "/content/" + relativePath);
			final Scanner scanner = new Scanner(url.openStream());
			final StringBuffer scriptReader = new StringBuffer();
			while (scanner.hasNext()) {
				final String nextLine = scanner.nextLine();
				scriptReader.append(nextLine + "\n");
			}
			script = scriptReader.toString();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return script;
	}

}
