package org.opentosca.planbuilder.tests.plugin.scripttest.bpel;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.model.tosca.AbstractImplementationArtifact;
import org.opentosca.planbuilder.model.tosca.AbstractInterface;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractOperation;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.provphase.plugin.invoker.bpel.BPELInvokerPlugin;
import org.opentosca.planbuilder.tests.plugin.scripttest.core.ScriptTestPolicyPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BPELScriptTestPolicyPlugin extends ScriptTestPolicyPlugin<BPELPlanContext> {

	private static final Logger LOGGER = LoggerFactory.getLogger(BPELScriptTestPolicyPlugin.class);
	private final BPELInvokerPlugin invokerPlugin = new BPELInvokerPlugin();
	private static final String CSAR_STORE_URL = "http://localhost:1337/csars/";
	private static final String OPENTOSCA_TEST_INTERFACE = "http://opentosca.org/interfaces/test";

	@Override
	public boolean handle(BPELPlanContext bpelPlanContext, AbstractNodeTemplate nodeTemplate,
			AbstractPolicy testPolicy) {
		// final AbstractNodeTemplate runScriptNode =
		// findRunScriptInfrastructureNode(nodeTemplate);
		final AbstractImplementationArtifact scriptArtifact = findScriptTestIA(nodeTemplate, testPolicy);
		// final List<AbstractArtifactReference> references =
		// scriptArtifact.getArtifactRef().getArtifactReferences();
		// if (references.size() > 1) {
		// LOGGER.error("More than one IA referenced for Node {} and Policy {}",
		// nodeTemplate.getName(),
		// testPolicy.getName());
		// return false;
		// }
		// final String testScriptReference = references.get(0).getReference();
		// final String csarName = bpelPlanContext.getCSARFileName();
		//
		// final String scriptFileContent = retrieveScriptContent(csarName,
		// testScriptReference);
		final List<AbstractInterface> interfaces = nodeTemplate.getType().getInterfaces();
		for (final AbstractInterface aInterface : interfaces) {
			if (aInterface.getName().equals(OPENTOSCA_TEST_INTERFACE)) {
				final List<AbstractOperation> ops = aInterface.getOperations();
				for (final AbstractOperation op : ops) {
					if (op.getName().equalsIgnoreCase(testPolicy.getType().getName())) {
						this.invokerPlugin.handle(bpelPlanContext, op, scriptArtifact);
					}
				}
			}
		}

		// TODO: Create runScript invoker call and fetch needed properties + script
		// content +
		// policy properties
		return false;
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
				scriptReader.append(scanner.nextLine());
			}
			script = scriptReader.toString();
			System.out.println(script);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return script;
	}

}
