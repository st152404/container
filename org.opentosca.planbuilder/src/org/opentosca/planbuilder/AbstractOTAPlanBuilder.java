package org.opentosca.planbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.opentosca.planbuilder.model.plan.ANodeTemplateActivity;
import org.opentosca.planbuilder.model.plan.ARelationshipTemplateActivity;
import org.opentosca.planbuilder.model.plan.AbstractActivity;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.AbstractPlan.Link;
import org.opentosca.planbuilder.model.plan.ActivityType;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractTopologyTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;

public abstract class AbstractOTAPlanBuilder extends AbstractPlanBuilder {



    protected AbstractPlan generateOTA(final String id, final AbstractDefinitions definitions,
                                       final AbstractServiceTemplate serviceTemplate) {

        final Collection<AbstractActivity> activities = new ArrayList<>();
        final Set<Link> links = new HashSet<>();
        final Map<AbstractNodeTemplate, AbstractActivity> nodeMapping = new HashMap<>();
        final Map<AbstractRelationshipTemplate, AbstractActivity> relationMapping = new HashMap<>();
        final AbstractTopologyTemplate topology = serviceTemplate.getTopologyTemplate();

        for (final AbstractNodeTemplate nodeTemplate : topology.getNodeTemplates()) {
            final AbstractActivity activity = new ANodeTemplateActivity(nodeTemplate.getId() + "_iotoperation_activity",
                ActivityType.IOTOPERATION, nodeTemplate);
            activities.add(activity);
            nodeMapping.put(nodeTemplate, activity);
        }

        for (final AbstractRelationshipTemplate relationshipTemplate : topology.getRelationshipTemplates()) {
            final AbstractActivity activity =
                new ARelationshipTemplateActivity(relationshipTemplate.getId() + "__iotoperation_activity",
                    ActivityType.IOTOPERATION, relationshipTemplate);
            activities.add(activity);
            relationMapping.put(relationshipTemplate, activity);
        }

        for (final AbstractRelationshipTemplate relationshipTemplate : topology.getRelationshipTemplates()) {
            final AbstractActivity activity = relationMapping.get(relationshipTemplate);
            final QName baseType = ModelUtils.getRelationshipBaseType(relationshipTemplate);
            if (baseType.equals(ModelUtils.TOSCABASETYPE_CONNECTSTO)) {
                links.add(new Link(nodeMapping.get(relationshipTemplate.getSource()), activity));
                links.add(new Link(nodeMapping.get(relationshipTemplate.getTarget()), activity));
            } else if (baseType.equals(ModelUtils.TOSCABASETYPE_DEPENDSON)
                | baseType.equals(ModelUtils.TOSCABASETYPE_HOSTEDON)
                | baseType.equals(ModelUtils.TOSCABASETYPE_DEPLOYEDON)) {
                links.add(new Link(nodeMapping.get(relationshipTemplate.getTarget()), activity));
                links.add(new Link(activity, nodeMapping.get(relationshipTemplate.getSource())));
            }

        }

        final AbstractPlan plan =
            new AbstractPlan(id, AbstractPlan.PlanType.MANAGE, definitions, serviceTemplate, activities, links) {

            };
        return plan;
    }
}
