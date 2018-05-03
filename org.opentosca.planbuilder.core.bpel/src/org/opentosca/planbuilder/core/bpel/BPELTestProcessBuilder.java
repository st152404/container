package org.opentosca.planbuilder.core.bpel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.opentosca.planbuilder.AbstractTestPlanBuilder;
import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.core.bpel.handlers.BPELPlanHandler;
import org.opentosca.planbuilder.core.bpel.helpers.BPELFinalizer;
import org.opentosca.planbuilder.core.bpel.helpers.CorrelationIDInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.EmptyPropertyToInputInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyMappingsToOutputInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer.PropertyMap;
import org.opentosca.planbuilder.core.plugins.IPlanBuilderTestPolicyPlugin;
import org.opentosca.planbuilder.core.plugins.registry.PluginRegistry;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELScopeActivity;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BPELTestProcessBuilder extends AbstractTestPlanBuilder {

	final static Logger LOGGER = LoggerFactory.getLogger(BPELTestProcessBuilder.class);

	private static final String TEST_INPUT_OPERATION_NAME = "test";
	private static final String BPEL_REST_NAMESPACE = "http://iaas.uni-stuttgart.de/bpel/extensions/bpel4restlight";

	private BPELPlanHandler planHandler;
	private final PropertyMappingsToOutputInitializer propertyOutputInitializer;
	private final BPELFinalizer finalizer;
	private final PropertyVariableInitializer propertyInitializer;
	private final EmptyPropertyToInputInitializer emptyPropertyInitializer;
	private final CorrelationIDInitializer correlationIdInitializer;
	private final PluginRegistry pluginRegistry = new PluginRegistry();

	public BPELTestProcessBuilder() {
		try {
			this.planHandler = new BPELPlanHandler();
		} catch (final ParserConfigurationException e) {
			LOGGER.error("Error initializing BPELPlanHandler", e);
		}
		this.correlationIdInitializer = new CorrelationIDInitializer();
		this.emptyPropertyInitializer = new EmptyPropertyToInputInitializer();
		this.propertyInitializer = new PropertyVariableInitializer(this.planHandler);
		this.propertyOutputInitializer = new PropertyMappingsToOutputInitializer();
		this.finalizer = new BPELFinalizer();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.opentosca.planbuilder.AbstractPlanBuilder#buildPlan(java.lang.String,
	 * org.opentosca.planbuilder.model.tosca.AbstractDefinitions,
	 * javax.xml.namespace.QName)
	 */
	@Override
	public BPELPlan buildPlan(final String csarName, final AbstractDefinitions definitions,
			final QName serviceTemplateId) {
		final List<AbstractServiceTemplate> serviceTemplates = definitions.getServiceTemplates();
		for (final AbstractServiceTemplate serviceTemplate : serviceTemplates) {
			String namespace = serviceTemplate.getTargetNamespace();
			if (namespace == null) {
				// if
				namespace = definitions.getTargetNamespace();
			}
			if (!namespace.equals(serviceTemplateId.getNamespaceURI())
					|| !serviceTemplate.getId().equals(serviceTemplateId.getLocalPart())) {
				LOGGER.warn("Couldn't create TestPlan for ServiceTemplate {} in Definitions {} of CSAR {}",
						serviceTemplateId.toString(), definitions.getId(), csarName);
				return null;
			}
			final String processName = serviceTemplate.getId() + "_testPlan";
			final String processNamespace = namespace + "_testPlan";

			final AbstractPlan abstractTestPlan = generateTestOG(new QName(processNamespace, processName).toString(),
					definitions, serviceTemplate);

			LOGGER.debug("Generated the following abstract test plan: \n{}", abstractTestPlan.toString());

			final BPELPlan bpelTestPlan = this.planHandler.createEmptyBPELPlan(processNamespace, processName,
					abstractTestPlan, TEST_INPUT_OPERATION_NAME);

			bpelTestPlan.setTOSCAInterfaceName(TEST_INTERFACE_NAMESPACE);
			bpelTestPlan.setTOSCAOperationname(TEST_INPUT_OPERATION_NAME);

			this.planHandler.initializeBPELSkeleton(bpelTestPlan, csarName);

			this.planHandler.registerExtension(BPEL_REST_NAMESPACE, true, bpelTestPlan);

			runPlugins(bpelTestPlan);

			this.correlationIdInitializer.addCorrellationID(bpelTestPlan);

			this.finalizer.finalize(bpelTestPlan);

			LOGGER.debug(ModelUtils.getStringFromDoc(bpelTestPlan.getBpelDocument()));
			return bpelTestPlan;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.opentosca.planbuilder.AbstractPlanBuilder#buildPlans(java.lang.String,
	 * org.opentosca.planbuilder.model.tosca.AbstractDefinitions)
	 */
	@Override
	public List<AbstractPlan> buildPlans(final String csarName, final AbstractDefinitions definitions) {
		final List<AbstractPlan> testPlanList = new ArrayList<>();
		for (final AbstractServiceTemplate serviceTemplate : definitions.getServiceTemplates()) {
			QName serviceTemplateId;
			// targetNamespace attribute doesn't has to be set, so we check it
			if (serviceTemplate.getTargetNamespace() != null) {
				serviceTemplateId = new QName(serviceTemplate.getTargetNamespace(), serviceTemplate.getId());
			} else {
				serviceTemplateId = new QName(definitions.getTargetNamespace(), serviceTemplate.getId());
			}
			// count nodeTemplates and relationshipTemplates with assigned tests
			final long nodeTemplatesWithTests = serviceTemplate.getTopologyTemplate().getNodeTemplates().stream()
					.filter(this::nodeTemplateHasTests).count();

			if (!serviceTemplate.hasTestPlan() && nodeTemplatesWithTests > 0) {
				LOGGER.debug(
						"ServiceTemplate {} has no TestPlan and {} NodeTemplates with Tests assigned, generating TestPlan",
						serviceTemplateId.toString(), nodeTemplatesWithTests);
				final BPELPlan newTestPlan = buildPlan(csarName, definitions, serviceTemplateId);

				if (newTestPlan != null) {
					LOGGER.debug("Created TestPlan " + newTestPlan.getBpelProcessElement().getAttribute("name"));
					testPlanList.add(newTestPlan);
				}
			} else {
				LOGGER.debug(
						"ServiceTemplate {} has TestPlan assigned/ {} defined Tests were found, no generation needed",
						serviceTemplateId.toString(), nodeTemplatesWithTests);
			}
		}
		return testPlanList;
	}

	private void runPlugins(final BPELPlan bpelTestPlan) {
		final List<BPELScopeActivity> bpelScopes = bpelTestPlan.getTemplateBuildPlans();
		final AbstractServiceTemplate serviceTemplate = bpelTestPlan.getServiceTemplate();
		PropertyVariableInitializer initializer = null;
		try {
			initializer = new PropertyVariableInitializer(new BPELPlanHandler());
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
		}

		for (final BPELScopeActivity bpelScope : bpelScopes) {
			final PropertyMap dummyMap = initializer.new PropertyMap();
			// FIXME: this is a dirty hack, we don't need any "normal" variables for the
			// test execution (yet) so a workaround was made
			final BPELPlanContext context = new BPELPlanContext(bpelScope, dummyMap, serviceTemplate);
			final AbstractNodeTemplate nodeTemplate = bpelScope.getNodeTemplate();
			if (nodeTemplate == null) {
				// Tests for relationships are not planned
				continue;
			}
			for (final AbstractPolicy policy : nodeTemplate.getPolicies()) {
				final IPlanBuilderTestPolicyPlugin plugin = findPluginForTestPolicy(policy, nodeTemplate);
				plugin.handle(context, nodeTemplate, policy);
			}
		}

	}

	private IPlanBuilderTestPolicyPlugin findPluginForTestPolicy(AbstractPolicy policy,
			AbstractNodeTemplate nodeTemplate) {
		for (final IPlanBuilderTestPolicyPlugin plugin : this.pluginRegistry.getTestPlugins()) {
			if (plugin.canHandle(nodeTemplate, policy)) {
				return plugin;
			}
		}
		LOGGER.error("No TestPlugin was found for test policy {} on NodeTemplate {}", policy.getName(),
				nodeTemplate.getName());
		return null;
	}
}
