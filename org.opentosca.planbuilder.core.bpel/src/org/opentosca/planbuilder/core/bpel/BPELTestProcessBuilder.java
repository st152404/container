package org.opentosca.planbuilder.core.bpel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.opentosca.planbuilder.AbstractTestPlanBuilder;
import org.opentosca.planbuilder.core.bpel.handlers.BPELPlanHandler;
import org.opentosca.planbuilder.core.bpel.helpers.BPELFinalizer;
import org.opentosca.planbuilder.core.bpel.helpers.CorrelationIDInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.EmptyPropertyToInputInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.NodeInstanceInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyMappingsToOutputInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.ServiceInstanceInitializer;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BPELTestProcessBuilder extends AbstractTestPlanBuilder {

    final static Logger LOGGER = LoggerFactory.getLogger(BPELTestProcessBuilder.class);

    private static final String TEST_INPUT_OPERATION_NAME = "test";

    private BPELPlanHandler planHandler;
    private final PropertyMappingsToOutputInitializer propertyOutputInitializer;
    private final BPELFinalizer finalizer;
    private final PropertyVariableInitializer propertyInitializer;
    private ServiceInstanceInitializer serviceInstanceInitializer;
    private NodeInstanceInitializer instanceInit;
    private final EmptyPropertyToInputInitializer emptyPropertyInitializer;
    private final CorrelationIDInitializer correlationIdInitializer;



    public BPELTestProcessBuilder() {
        try {
            this.planHandler = new BPELPlanHandler();
            this.serviceInstanceInitializer = new ServiceInstanceInitializer();
            this.instanceInit = new NodeInstanceInitializer(this.planHandler);
        }
        catch (final ParserConfigurationException e) {
            LOGGER.error("Error initializing BuildPlanHandler", e);
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
     * @see org.opentosca.planbuilder.AbstractPlanBuilder#buildPlan(java.lang.String,
     * org.opentosca.planbuilder.model.tosca.AbstractDefinitions, javax.xml.namespace.QName)
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
                BPELBuildProcessBuilder.LOG.warn("Couldn't create BuildPlan for ServiceTemplate {} in Definitions {} of CSAR {}",
                                                 serviceTemplateId.toString(), definitions.getId(), csarName);
                return null;
            }
            final String processName = serviceTemplate.getId() + "_testPlan";
            final String processNamespace = namespace + "_testPlan";

            final AbstractPlan abstractTestPlan =
                generateTestOG(new QName(processNamespace, processName).toString(), definitions, serviceTemplate);

            LOGGER.debug("Generated the following abstract test plan: \n{}", abstractTestPlan.toString());

            final BPELPlan bpelTestPlan =
                this.planHandler.createEmptyBPELPlan(processNamespace, processName, abstractTestPlan,
                                                     TEST_INPUT_OPERATION_NAME);

            bpelTestPlan.setTOSCAInterfaceName(TEST_INTERFACE_NAMESPACE);
            bpelTestPlan.setTOSCAOperationname(TEST_INPUT_OPERATION_NAME);

            this.planHandler.initializeBPELSkeleton(bpelTestPlan, csarName);

            this.serviceInstanceInitializer.initializeInstanceDataFromInput(bpelTestPlan);




            return bpelTestPlan;


        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opentosca.planbuilder.AbstractPlanBuilder#buildPlans(java.lang.String,
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
                BPELBuildProcessBuilder.LOG.debug("ServiceTemplate {} has no TestPlan and {} NodeTemplates with Tests assigned, generating TestPlan",
                                                  serviceTemplateId.toString(), nodeTemplatesWithTests);
                final BPELPlan newTestPlan = buildPlan(csarName, definitions, serviceTemplateId);

                if (newTestPlan != null) {
                    BPELBuildProcessBuilder.LOG.debug("Created TestPlan "
                        + newTestPlan.getBpelProcessElement().getAttribute("name"));
                    testPlanList.add(newTestPlan);
                }
            } else {
                BPELBuildProcessBuilder.LOG.debug("ServiceTemplate {} has TestPlan assigned/ {} defined Tests were found, no generation needed",
                                                  serviceTemplateId.toString(), nodeTemplatesWithTests);
            }
        }
        return testPlanList;
    }


    /**
     * Returns a list of all test operations defined for a note template
     *
     * @param nodeTemplate
     * @return
     */
    private List<String> getTestOperationNames(final AbstractNodeTemplate nodeTemplate) {
        final List<String> testOperationNames = new ArrayList<>();
        final List<AbstractPolicy> policies = nodeTemplate.getPolicies();
        for (final AbstractPolicy policy : policies) {
            testOperationNames.add(policy.getType().getName());
        }
        return testOperationNames;
    }
}
