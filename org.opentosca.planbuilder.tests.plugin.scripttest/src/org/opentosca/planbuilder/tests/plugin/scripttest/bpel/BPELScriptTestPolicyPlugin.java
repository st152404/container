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
import org.opentosca.planbuilder.model.tosca.AbstractInterface;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractOperation;
import org.opentosca.planbuilder.model.tosca.AbstractParameter;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.plugins.context.Variable;
import org.opentosca.planbuilder.provphase.plugin.invoker.bpel.BPELInvokerPlugin;
import org.opentosca.planbuilder.tests.plugin.scripttest.core.ScriptTestPolicyPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BPELScriptTestPolicyPlugin extends ScriptTestPolicyPlugin<BPELPlanContext> {

	private static final Logger LOGGER = LoggerFactory.getLogger(BPELScriptTestPolicyPlugin.class);
	private final BPELInvokerPlugin invokerPlugin = new BPELInvokerPlugin();
	private static final String CSAR_STORE_URL = "http://localhost:1337/csars/";
	private static final String RUN_SCRIPT_OPERATION = "runScript";

	@Override
	public boolean handle(BPELPlanContext bpelPlanContext, AbstractNodeTemplate nodeTemplate,
			AbstractPolicy testPolicy) {
		final AbstractNodeTemplate runScriptNode = findRunScriptInfrastructureNode(nodeTemplate);
		final AbstractImplementationArtifact scriptArtifact = findScriptTestIA(nodeTemplate, testPolicy);
		final Map<String, String> policyTemplateProps = testPolicy.getTemplate().getProperties().asMap();

		final List<AbstractArtifactReference> references = scriptArtifact.getArtifactRef().getArtifactReferences();

		if (references.size() > 1) {
			LOGGER.warn("More than one IA referenced for Node {} and Policy {} which might lead to inconsistency",
					nodeTemplate.getName(), testPolicy.getName());
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

		final Map<String, String> inputParamsMap = new HashMap<>();
		inputParamsMap.put("ContainerIP", null);
		inputParamsMap.put("SSHPort", null);
		inputParamsMap.put("Script", scriptFileContent);

		final Map<String, String> outputParamsMap = new HashMap<>();
		outputParamsMap.put("ScriptResult", null);

		final Map<AbstractParameter, Variable> inputPar2VarMap = createParam2VarMapFromProps(inputParamsMap,
				runScriptNode, bpelPlanContext);
		final Map<AbstractParameter, Variable> outputPar2VarMap = createParam2VarMapFromProps(outputParamsMap,
				runScriptNode, bpelPlanContext);

		this.invokerPlugin.handle(bpelPlanContext, findRunScriptOperation(runScriptNode), scriptArtifact,
				inputPar2VarMap, outputPar2VarMap);

		return false;
	}

	/**
	 * Finds the runScript operation on the InfrastructureNode implementing the
	 * runScript operation
	 *
	 * @param runScriptNode
	 *            the node implementing the runScript operation
	 * @return
	 */
	private static AbstractOperation findRunScriptOperation(AbstractNodeTemplate runScriptNode) {
		for (final AbstractInterface aInterface : runScriptNode.getType().getInterfaces()) {
			for (final AbstractOperation aOp : aInterface.getOperations()) {
				if (aOp.getName().equalsIgnoreCase(RUN_SCRIPT_OPERATION)) {
					return aOp;
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
				var = planContext.createGlobalStringVariable(runScriptNode.getName() + "_" + key, props.get(key));
			} else {
				var = planContext.getPropertyVariable(key);
			}
			if (var.getName() == null) {
				var = new Variable(var.getTemplateId(), "prop_" + var.getTemplateId() + "_" + key);
			}
			param2VarMap.put(par, var);
		}
		return param2VarMap;
	}

	/**
	 * Retrieves the content of the referenced file.
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
			System.out.println(script);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return script;
	}

}
