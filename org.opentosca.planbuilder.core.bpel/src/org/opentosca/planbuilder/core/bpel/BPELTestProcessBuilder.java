package org.opentosca.planbuilder.core.bpel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.opentosca.planbuilder.AbstractTestPlanBuilder;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;

public class BPELTestProcessBuilder extends AbstractTestPlanBuilder {

    List<AbstractNodeTemplate> nodeTemplatesWithTest = new ArrayList<>();

    public BPELTestProcessBuilder() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public AbstractPlan buildPlan(final String csarName, final AbstractDefinitions definitions, final QName serviceTemplateId) {
        final List<AbstractServiceTemplate> serviceTemplates = definitions.getServiceTemplates();
        for(final AbstractServiceTemplate serviceTemplate : serviceTemplates) {
            String namespace = serviceTemplate.getTargetNamespace();
            if(namespace == null) {
                namespace = definitions.getTargetNamespace();
            }
        }
        return null;
    }

    @Override
    public List<AbstractPlan> buildPlans(final String csarName, final AbstractDefinitions definitions) {
        // TODO Implement
        return null;
    }

}
