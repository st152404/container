package org.opentosca.planbuilder.core.bpel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.opentosca.planbuilder.AbstractTestPlanBuilder;
import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.core.bpel.handlers.BPELPlanHandler;
import org.opentosca.planbuilder.core.bpel.helpers.BPELFinalizer;
import org.opentosca.planbuilder.core.bpel.helpers.NodeRelationInstanceVariablesHandler;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer.PropertyMap;
import org.opentosca.planbuilder.core.bpel.helpers.ServiceInstanceVariablesHandler;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELScopeActivity;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.opentosca.planbuilder.plugins.IPlanBuilderTestPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BPELTestProcessBuilder extends AbstractTestPlanBuilder {

	final static Logger LOGGER = LoggerFactory.getLogger(BPELTestProcessBuilder.class);

	private static final String TEST_INPUT_OPERATION_NAME = "test";
	private static final String BPEL_REST_NAMESPACE = "http://iaas.uni-stuttgart.de/bpel/extensions/bpel4restlight";

	private BPELPlanHandler planHandler;
	private final BPELFinalizer finalizer;
	private final PropertyVariableInitializer propertyInitializer;
	private NodeRelationInstanceVariablesHandler instanceVarsHandler;
	private ServiceInstanceVariablesHandler serviceInstanceVarsHandler;

	public BPELTestProcessBuilder() {
		try {
			this.planHandler = new BPELPlanHandler();
			this.instanceVarsHandler = new NodeRelationInstanceVariablesHandler(this.planHandler);
			this.serviceInstanceVarsHandler = new ServiceInstanceVariablesHandler();
		} catch (final ParserConfigurationException e) {
			LOGGER.error("Error initializing BPELPlanHandler", e);
		}
		this.propertyInitializer = new PropertyVariableInitializer(this.planHandler);
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

			final AbstractPlan abstractTestPlan = generateTestDAG(new QName(processNamespace, processName).toString(),
					definitions, serviceTemplate);

			LOGGER.debug("Generated the following abstract test plan:\n{}", abstractTestPlan.toString());

			final BPELPlan bpelTestPlan = this.planHandler.createEmptyBPELPlan(processNamespace, processName,
					abstractTestPlan, TEST_INPUT_OPERATION_NAME);

			bpelTestPlan.setTOSCAInterfaceName(TEST_INTERFACE_NAMESPACE);
			bpelTestPlan.setTOSCAOperationname(TEST_INPUT_OPERATION_NAME);

			this.planHandler.initializeBPELSkeleton(bpelTestPlan, csarName);

			this.instanceVarsHandler.addInstanceURLVarToTemplatePlans(bpelTestPlan);
			this.instanceVarsHandler.addInstanceIDVarToTemplatePlans(bpelTestPlan);

			final PropertyMap propMap = this.propertyInitializer.initializePropertiesAsVariables(bpelTestPlan);

			this.serviceInstanceVarsHandler.addManagementPlanServiceInstanceVarHandlingFromInput(bpelTestPlan);
			this.serviceInstanceVarsHandler.initPropertyVariablesFromInstanceData(bpelTestPlan, propMap);

			// find all running node instances
			this.instanceVarsHandler.addNodeInstanceFindLogic(bpelTestPlan,
					"?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED");

			this.instanceVarsHandler.addPropertyVariableUpdateBasedOnNodeInstanceID(bpelTestPlan, propMap);

			this.planHandler.registerExtension(BPEL_REST_NAMESPACE, true, bpelTestPlan);

			runTestPlugins(bpelTestPlan, propMap);

			this.serviceInstanceVarsHandler.appendSetServiceInstanceState(bpelTestPlan,
					bpelTestPlan.getBpelMainSequenceOutputAssignElement(), "TESTED");

			this.serviceInstanceVarsHandler.addCorrellationID(bpelTestPlan);

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

	private void runTestPlugins(final BPELPlan bpelTestPlan, PropertyMap propMap) {
		final List<BPELScopeActivity> bpelScopes = bpelTestPlan.getTemplateBuildPlans();
		final AbstractServiceTemplate serviceTemplate = bpelTestPlan.getServiceTemplate();

		for (final BPELScopeActivity bpelScope : bpelScopes) {
			final BPELPlanContext context = new BPELPlanContext(bpelScope, propMap, serviceTemplate);
			final AbstractNodeTemplate nodeTemplate = bpelScope.getNodeTemplate();
			if (nodeTemplate == null) {
				// Tests for relationships are not planned
				continue;
			}
			for (final AbstractPolicy policy : nodeTemplate.getPolicies()) {
				final IPlanBuilderTestPlugin plugin = findTestPlugin(policy, nodeTemplate);
				plugin.handle(context, nodeTemplate, policy);
			}
		}

	}
}
