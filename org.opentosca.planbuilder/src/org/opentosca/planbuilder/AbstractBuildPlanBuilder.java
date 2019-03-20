package org.opentosca.planbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.opentosca.planbuilder.model.plan.ANodeTemplateActivity;
import org.opentosca.planbuilder.model.plan.ARelationshipTemplateActivity;
import org.opentosca.planbuilder.model.plan.AbstractActivity;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.AbstractPlan.Link;
import org.opentosca.planbuilder.model.plan.ActivityType;
import org.opentosca.planbuilder.model.plan.TopologyFragment;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;;

public abstract class AbstractBuildPlanBuilder extends AbstractPlanBuilder {

    /**
     * Generates a provisioning order graph (POG) for the given Service Template which is defined in
     * the given definitions element. A POG defines the order in which the Node and Relationship
     * Templates of the Service Template have to be provisioned.
     *
     * @param id an ID which identifies the generated AbstractPlan
     * @param definitions the definitions element containing the Service Template
     * @param serviceTemplate the Service Template for which the POG is generated
     * @return an AbstractPlan representing the POG for the Service Template
     */
    public static AbstractPlan generatePOG(final String id, final AbstractDefinitions definitions,
                                           final AbstractServiceTemplate serviceTemplate) {
        return generatePOG(id, definitions, serviceTemplate, serviceTemplate.getTopologyTemplate().getNodeTemplates(),
                           serviceTemplate.getTopologyTemplate().getRelationshipTemplates());
    }

    /**
     * Generates a provisioning order graph (POG) for the given Service Template which is defined in
     * the given definitions element. A POG defines the order in which the Node and Relationship
     * Templates of the Service Template have to be provisioned. By providing a subset of Node and
     * Relationship Templates of the Service Template it is possible to generate a POG that only
     * contains activities for this part of the topology.
     *
     * @param id an ID which identifies the generated AbstractPlan
     * @param definitions the definitions element containing the Service Template
     * @param serviceTemplate the Service Template for which the POG is generated
     * @param nodeTemplates the subset of Node Templates to use
     * @param relationshipTemplates the subset of Relationship Templates to use
     * @return an AbstractPlan representing the POG for the Service Template snippet
     */
    public static AbstractPlan generatePOG(final String id, final AbstractDefinitions definitions,
                                           final AbstractServiceTemplate serviceTemplate,
                                           final Collection<AbstractNodeTemplate> nodeTemplates,
                                           final Collection<AbstractRelationshipTemplate> relationshipTemplates) {

        final Collection<AbstractActivity> activities = new ArrayList<>();
        final Set<Link> links = new HashSet<>();

        generatePOGActivitesAndLinks(activities, links, nodeTemplates, relationshipTemplates);

        final AbstractPlan plan =
            new AbstractPlan(id, AbstractPlan.PlanType.BUILD, definitions, serviceTemplate, activities, links) {

            };
        return plan;
    }

    /**
     * Generates a list of provisioning order graphs (POGs) for the given ServiceTemplate which is
     * defined in the given definitions element. For each given TopologyFragment one POG is
     * generated. Each POG contains the provisioning activities for a fragment of the
     * ServiceTemplate and the order in which they need to be executed. Additionally, the POGs
     * contain the order in which they have to be executed by an orchestrator build plan to
     * provision the complete application defined by the ServiceTemplate.
     *
     * @param id the ID of the orchestrator build plan
     * @param definitions the definitions element that contains the ServiceTemplate
     * @param serviceTemplate the ServiceTemplate for which the POGs shall be generated
     * @param fragments a list of TopologyFragment which need a dedicated build plan
     * @return a list of generated plans
     */
    public static List<AbstractPlan> generatePOGs(final String id, final AbstractDefinitions definitions,
                                                  final AbstractServiceTemplate serviceTemplate,
                                                  final List<TopologyFragment> fragments) {

        final List<AbstractPlan> pogs = new ArrayList<>();
        final Map<TopologyFragment, AbstractPlan> fragmentPlanMapping = new HashMap<>();

        // generate one POG per topology fragment
        for (final TopologyFragment fragment : fragments) {

            final String fragmentID = id + "_fragment_" + fragment.getID();

            final Collection<AbstractActivity> activities = new ArrayList<>();
            final Set<Link> links = new HashSet<>();

            // TODO: generate activities and links

            final AbstractPlan plan = new AbstractPlan(fragmentID, AbstractPlan.PlanType.BUILD, definitions,
                serviceTemplate, activities, links) {};

            pogs.add(plan);
            fragmentPlanMapping.put(fragment, plan);
        }

        // create order between plan fragments for the use in an orchestrator plan
        for (final TopologyFragment fragment : fragments) {
            final AbstractPlan plan = fragmentPlanMapping.get(fragment);

            for (final TopologyFragment precedingFragment : fragment.getPrecedingFragments()) {
                final AbstractPlan precedingPlan = fragmentPlanMapping.get(precedingFragment);

                // TODO: add link between different plans
            }
        }

        return pogs;
    }

    /**
     * Add the activities and the links of the POG to the given live lists.
     *
     * @param activities the live list of activities
     * @param links the live list of links
     * @param nodeTemplates the Node Templates for which provisioning shall be performed
     * @param relationshipTemplates the Relationship Templates for which provisioning shall be
     *        performed
     */
    private static void generatePOGActivitesAndLinks(final Collection<AbstractActivity> activities,
                                                     final Set<Link> links,
                                                     final Collection<AbstractNodeTemplate> nodeTemplates,
                                                     final Collection<AbstractRelationshipTemplate> relationshipTemplates) {
        // generate provisioning activities for all Node Templates
        final Map<AbstractNodeTemplate, AbstractActivity> nodeActivityMapping = new HashMap<>();
        for (final AbstractNodeTemplate nodeTemplate : nodeTemplates) {
            final AbstractActivity activity = new ANodeTemplateActivity(nodeTemplate.getId() + "_provisioning_activity",
                ActivityType.PROVISIONING, nodeTemplate);
            activities.add(activity);
            nodeActivityMapping.put(nodeTemplate, activity);
        }

        for (final AbstractRelationshipTemplate relationshipTemplate : relationshipTemplates) {
            // generate provisioning activities for all Relationship Templates
            final AbstractActivity activity =
                new ARelationshipTemplateActivity(relationshipTemplate.getId() + "_provisioning_activity",
                    ActivityType.PROVISIONING, relationshipTemplate);
            activities.add(activity);

            // add order for the provisioning activities
            final QName baseType = ModelUtils.getRelationshipBaseType(relationshipTemplate);

            // Relationship provisioning must always be after provisioning of Node below
            links.add(new Link(nodeActivityMapping.get(relationshipTemplate.getTarget()), activity));

            if (baseType.equals(ModelUtils.TOSCABASETYPE_CONNECTSTO)) {
                // Connects_To is provisioned after both connected Node Templates
                links.add(new Link(nodeActivityMapping.get(relationshipTemplate.getSource()), activity));
            } else if (baseType.equals(ModelUtils.TOSCABASETYPE_DEPENDSON)
                | baseType.equals(ModelUtils.TOSCABASETYPE_HOSTEDON)
                | baseType.equals(ModelUtils.TOSCABASETYPE_DEPLOYEDON)) {
                // Infrastructure Relationship Templates are provisioned before the Nodes above
                links.add(new Link(activity, nodeActivityMapping.get(relationshipTemplate.getSource())));
            }
        }
    }
}
