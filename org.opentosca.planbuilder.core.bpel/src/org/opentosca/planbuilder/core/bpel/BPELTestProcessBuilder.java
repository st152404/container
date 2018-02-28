package org.opentosca.planbuilder.core.bpel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.opentosca.planbuilder.AbstractTestPlanBuilder;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;

public class BPELTestProcessBuilder extends AbstractTestPlanBuilder {

    List<AbstractNodeTemplate> nodeTemplatesWithTest = new ArrayList<>();

    public BPELTestProcessBuilder() {
        // TODO Auto-generated constructor stub
    }


    /*
     * (non-Javadoc)
     * @see org.opentosca.planbuilder.AbstractPlanBuilder#buildPlan(java.lang.String, org.opentosca.planbuilder.model.tosca.AbstractDefinitions, javax.xml.namespace.QName)
     */
    @Override
    public BPELPlan buildPlan(final String csarName, final AbstractDefinitions definitions, final QName serviceTemplateId) {
        final List<AbstractServiceTemplate> serviceTemplates = definitions.getServiceTemplates();
        for(final AbstractServiceTemplate serviceTemplate : serviceTemplates) {
            String namespace = serviceTemplate.getTargetNamespace();
            if(namespace == null) {
                namespace = definitions.getTargetNamespace();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.opentosca.planbuilder.AbstractPlanBuilder#buildPlans(java.lang.String, org.opentosca.planbuilder.model.tosca.AbstractDefinitions)
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

            if (!serviceTemplate.hasBuildPlan()) {
                BPELBuildProcessBuilder.LOG.debug("ServiceTemplate {} has no TestPlan, generating TestPlan",
                                                  serviceTemplateId.toString());
                final BPELPlan newTestPlan = buildPlan(csarName, definitions, serviceTemplateId);

                if (newTestPlan != null) {
                    BPELBuildProcessBuilder.LOG.debug("Created TestPlan "
                        + newTestPlan.getBpelProcessElement().getAttribute("name"));
                    testPlanList.add(newTestPlan);
                }
            } else {
                BPELBuildProcessBuilder.LOG.debug("ServiceTemplate {} has TestPlan, no generation needed",
                                                  serviceTemplateId.toString());
            }
        }
        return testPlanList;
    }

}
