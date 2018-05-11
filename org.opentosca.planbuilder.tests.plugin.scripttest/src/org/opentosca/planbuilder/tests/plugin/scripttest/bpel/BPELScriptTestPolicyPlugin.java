package org.opentosca.planbuilder.tests.plugin.scripttest.bpel;

import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.provphase.plugin.invoker.bpel.BPELInvokerPlugin;
import org.opentosca.planbuilder.tests.plugin.scripttest.core.ScriptTestPolicyPlugin;

public class BPELScriptTestPolicyPlugin extends ScriptTestPolicyPlugin<BPELPlanContext> {

	private final BPELInvokerPlugin invokerPlugin = new BPELInvokerPlugin();

	@Override
	public boolean handle(BPELPlanContext bpelPlanContext, AbstractNodeTemplate nodeTemplate,
			AbstractPolicy testPolicyTemplate) {
		// TODO: Create runScript invoker call and fetch needed properties + script
		// content +
		// policy properties
		return false;
	}

}
