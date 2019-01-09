package org.opentosca.planbuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.graph.DirectedMultigraph;
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

/**
 * @author Kalman Kepes - kepes@iaas.uni-stuttgart.de
 *
 */
public abstract class AbstractUpdatePlanBuilder extends AbstractTransformingPlanBuilder {

	private class GraphResult {
		Set<AbstractNodeTemplate> nodes;
		Set<AbstractRelationshipTemplate> relations;

		public GraphResult(final Set<AbstractNodeTemplate> nodes, final Set<AbstractRelationshipTemplate> relations) {
			this.nodes = nodes;
			this.relations = relations;
		}
	}

	public AbstractPlan generateUOG(final String currentCsarName, final AbstractDefinitions currentDefinitions,
			final QName currentServiceTemplate, final String desiredCsarName,
			final AbstractDefinitions desiredDefinitions, final QName desiredServiceTemplate) {
		final String planId = "updatePlan_" + currentCsarName + "_" + currentServiceTemplate + "_" + desiredCsarName
				+ "_" + desiredServiceTemplate;

		// get serviceTemplates
		final Graph<AbstractNodeTemplate, AbstractRelationshipTemplate> currentServTemp = transformToJGraph(
				getServiceTemplate(currentDefinitions, currentServiceTemplate));
		final Graph<AbstractNodeTemplate, AbstractRelationshipTemplate> desiredServTemp = transformToJGraph(
				getServiceTemplate(desiredDefinitions, desiredServiceTemplate));

		// // calc max common subgraph s
		final GraphMapping<AbstractNodeTemplate, AbstractRelationshipTemplate> maxSubgraphMapping = getMaxSubgraph(
				currentServTemp, desiredServTemp);

		Set<AbstractNodeTemplate> subgraphNodeSet = null;
		Set<AbstractRelationshipTemplate> subgraphRelationSet = null;

		if (maxSubgraphMapping != null) {
			// found subgraph
			subgraphNodeSet = transformToNodeSet(currentServTemp.vertexSet(), maxSubgraphMapping, true);
			subgraphRelationSet = transformToRelationSet(currentServTemp.edgeSet(), maxSubgraphMapping, true);

		} else {
			// if there is no common subgraph, this will generate activites that terminates
			// everything from
			// currentServTemp and provision everything from desiredServTemp
			subgraphNodeSet = Collections.emptySet();
			subgraphRelationSet = Collections.emptySet();
		}

		// clean subgraph of nodes and egdes for which infrastructure nodes and edges
		// are missing in the max common subgraph
		final GraphResult subgraphToRemove = getIncompleteInfrastructureNodes(currentServTemp.vertexSet(),
				maxSubgraphMapping, true);

		// calculate new cleaned subgraph
		subgraphNodeSet.removeAll(subgraphToRemove.nodes);
		subgraphRelationSet.removeAll(subgraphToRemove.relations);

		return generateUOGActivitiesAndLinks(planId, currentDefinitions, desiredDefinitions, subgraphNodeSet,
				subgraphRelationSet, getServiceTemplate(currentDefinitions, currentServiceTemplate),
				getServiceTemplate(desiredDefinitions, desiredServiceTemplate));

	}

	private AbstractPlan generateUOGActivitiesAndLinks(final String id, final AbstractDefinitions definitions1,
			final AbstractDefinitions definitions2, final Set<AbstractNodeTemplate> commonSubgraphNodeSet,
			final Set<AbstractRelationshipTemplate> commonSubgraphEdgeSet,
			final AbstractServiceTemplate currentServiceTemplate,
			final AbstractServiceTemplate desiredServiceTemplate) {

		// TODO refactor code to use termination and provisioning builder code

		final Map<AbstractNodeTemplate, AbstractActivity> nodeActivityMapping = new HashMap<>();
		final Map<AbstractRelationshipTemplate, AbstractActivity> relationActivityMapping = new HashMap<>();
		final Collection<AbstractActivity> activities = Collections.emptySet();
		final Set<Link> links = Collections.emptySet();

		// create termination activities for all nodes and edges of the current topology
		// that are not part
		// of the given common subgraph
		for (final AbstractNodeTemplate nodeTemplate : currentServiceTemplate.getTopologyTemplate()
				.getNodeTemplates()) {
			if (!commonSubgraphNodeSet.contains(nodeTemplate)) {

				final ANodeTemplateActivity activity = new ANodeTemplateActivity(
						nodeTemplate.getId() + "_termination_activity", ActivityType.TERMINATION, nodeTemplate);
				activities.add(activity);
				nodeActivityMapping.put(nodeTemplate, activity);
			}
		}

		for (final AbstractRelationshipTemplate relationshipTemplate : currentServiceTemplate.getTopologyTemplate()
				.getRelationshipTemplates()) {
			if (!commonSubgraphEdgeSet.contains(relationshipTemplate)) {
				final ARelationshipTemplateActivity activity = new ARelationshipTemplateActivity(
						relationshipTemplate.getId() + "_termination_activity", ActivityType.TERMINATION,
						relationshipTemplate);
				activities.add(activity);

				final QName baseType = ModelUtils.getRelationshipBaseType(relationshipTemplate);

				if (baseType.equals(ModelUtils.TOSCABASETYPE_CONNECTSTO)) {
					links.add(new Link(activity, nodeActivityMapping.get(relationshipTemplate.getSource())));
					links.add(new Link(activity, nodeActivityMapping.get(relationshipTemplate.getTarget())));
				} else if (baseType.equals(ModelUtils.TOSCABASETYPE_DEPENDSON)
						| baseType.equals(ModelUtils.TOSCABASETYPE_HOSTEDON)
						| baseType.equals(ModelUtils.TOSCABASETYPE_DEPLOYEDON)) {
					links.add(new Link(nodeActivityMapping.get(relationshipTemplate.getSource()), activity));
					links.add(new Link(activity, nodeActivityMapping.get(relationshipTemplate.getTarget())));
				}
			}

		}

		// create provisioning activities for all nodes and edges of the desired
		// topology that are not part
		// of the given common subgraph

		for (final AbstractNodeTemplate nodeTemplate : desiredServiceTemplate.getTopologyTemplate()
				.getNodeTemplates()) {
			if (!commonSubgraphNodeSet.contains(nodeTemplate)) {
				final AbstractActivity activity = new ANodeTemplateActivity(
						nodeTemplate.getId() + "_provisioning_activity", ActivityType.PROVISIONING, nodeTemplate);
				activities.add(activity);
				nodeActivityMapping.put(nodeTemplate, activity);
			}
		}

		for (final AbstractRelationshipTemplate relationshipTemplate : desiredServiceTemplate.getTopologyTemplate()
				.getRelationshipTemplates()) {
			if (!commonSubgraphEdgeSet.contains(relationshipTemplate)) {
				final AbstractActivity activity = new ARelationshipTemplateActivity(
						relationshipTemplate.getId() + "_provisioning_activity", ActivityType.PROVISIONING,
						relationshipTemplate);
				activities.add(activity);
				relationActivityMapping.put(relationshipTemplate, activity);
			}
		}

		for (final AbstractRelationshipTemplate relationshipTemplate : desiredServiceTemplate.getTopologyTemplate()
				.getRelationshipTemplates()) {
			if (!commonSubgraphEdgeSet.contains(relationshipTemplate)) {
				final AbstractActivity activity = relationActivityMapping.get(relationshipTemplate);
				final QName baseType = ModelUtils.getRelationshipBaseType(relationshipTemplate);
				if (baseType.equals(ModelUtils.TOSCABASETYPE_CONNECTSTO)) {
					links.add(new Link(nodeActivityMapping.get(relationshipTemplate.getSource()), activity));
					links.add(new Link(nodeActivityMapping.get(relationshipTemplate.getTarget()), activity));
				} else if (baseType.equals(ModelUtils.TOSCABASETYPE_DEPENDSON)
						| baseType.equals(ModelUtils.TOSCABASETYPE_HOSTEDON)
						| baseType.equals(ModelUtils.TOSCABASETYPE_DEPLOYEDON)) {
					links.add(new Link(nodeActivityMapping.get(relationshipTemplate.getTarget()), activity));
					links.add(new Link(activity, nodeActivityMapping.get(relationshipTemplate.getSource())));
				}
			}
		}

		final AbstractPlan plan = new AbstractPlan(id, AbstractPlan.PlanType.MANAGE, definitions1, definitions2,
				currentServiceTemplate, desiredServiceTemplate, activities, links) {
		};

		return plan;

	}

	private Set<AbstractNodeTemplate> transformToNodeSet(final Set<AbstractNodeTemplate> completeNodeSet,
			final GraphMapping<AbstractNodeTemplate, AbstractRelationshipTemplate> subgraph,
			final boolean g1Tog2Mapping) {
		final Set<AbstractNodeTemplate> nodeSet = new HashSet<>();
		for (final AbstractNodeTemplate node : completeNodeSet) {
			if (subgraph.getVertexCorrespondence(node, g1Tog2Mapping) != null) {
				nodeSet.add(node);
			}
		}
		return nodeSet;
	}

	private Set<AbstractRelationshipTemplate> transformToRelationSet(
			final Set<AbstractRelationshipTemplate> completeRelationSet,
			final GraphMapping<AbstractNodeTemplate, AbstractRelationshipTemplate> subgraph,
			final boolean g1Tog2Mapping) {
		final Set<AbstractRelationshipTemplate> relationSet = new HashSet<>();
		for (final AbstractRelationshipTemplate relation : completeRelationSet) {
			if (subgraph.getEdgeCorrespondence(relation, g1Tog2Mapping) != null) {
				relationSet.add(relation);
			}
		}
		return relationSet;
	}

	private GraphResult getIncompleteInfrastructureNodes(final Set<AbstractNodeTemplate> nodeTemplates,
			final GraphMapping<AbstractNodeTemplate, AbstractRelationshipTemplate> subgraph,
			final boolean g1Tog2Mapping) {
		final Set<AbstractNodeTemplate> nodesToRemove = new HashSet<>();
		final Set<AbstractRelationshipTemplate> relationsToRemove = new HashSet<>();
		// We check all nodes of the topology with the subgraph, because jgrapht is a
		// bit weird when working
		// the mapping
		for (final AbstractNodeTemplate nodeOfGraph : nodeTemplates) {
			boolean mustRemove = false;
			final AbstractNodeTemplate nodeOfMapping = subgraph.getVertexCorrespondence(nodeOfGraph, g1Tog2Mapping);
			if (nodeOfMapping != null) {
				final List<AbstractRelationshipTemplate> infraEdges = ModelUtils
						.getOutgoingInfrastructureEdges(nodeOfMapping);
				for (final AbstractRelationshipTemplate infraEdge : infraEdges) {
					if (subgraph.getVertexCorrespondence(infraEdge.getTarget(), g1Tog2Mapping) == null) {
						// if there is a single node, which has an outgoing infrastructure edge and the
						// target of the
						// edge is NOT in the subgraph, remove the node as its infrastructure is not in
						// the mapping
						mustRemove = true;
						relationsToRemove.add(infraEdge);
						break;
					}
				}
			}
			if (mustRemove) {
				nodesToRemove.add(nodeOfGraph);
			}
		}
		return new GraphResult(nodesToRemove, relationsToRemove);
	}

	private GraphMapping<AbstractNodeTemplate, AbstractRelationshipTemplate> getMaxSubgraph(
			final Graph<AbstractNodeTemplate, AbstractRelationshipTemplate> g1,
			final Graph<AbstractNodeTemplate, AbstractRelationshipTemplate> g2) {
		final VF2SubgraphIsomorphismInspector<AbstractNodeTemplate, AbstractRelationshipTemplate> iso = new VF2SubgraphIsomorphismInspector<>(
				g1, g2);

		if (iso.isomorphismExists()) {
			final Iterator<GraphMapping<AbstractNodeTemplate, AbstractRelationshipTemplate>> iter = iso.getMappings();
			final int maxSubgraphSize = 0;
			GraphMapping<AbstractNodeTemplate, AbstractRelationshipTemplate> maxSubgraph = null;

			while (iter.hasNext()) {
				final GraphMapping<AbstractNodeTemplate, AbstractRelationshipTemplate> mapping = iter.next();
				int subgraphSize = 0;
				for (final AbstractNodeTemplate node : g1.vertexSet()) {
					if (mapping.getVertexCorrespondence(node, true) != null) {
						subgraphSize++;
					}
				}

				if (subgraphSize > maxSubgraphSize) {
					maxSubgraph = mapping;
				}
			}
			return maxSubgraph;
		} else {
			return null;
		}

	}

	private Graph<AbstractNodeTemplate, AbstractRelationshipTemplate> transformToJGraph(
			final AbstractServiceTemplate serviceTemplate) {
		final Graph<AbstractNodeTemplate, AbstractRelationshipTemplate> g = new DirectedMultigraph<>(
				AbstractRelationshipTemplate.class);

		final AbstractTopologyTemplate topology = serviceTemplate.getTopologyTemplate();

		for (final AbstractRelationshipTemplate relationship : topology.getRelationshipTemplates()) {
			if (!g.containsVertex(relationship.getSource())) {
				g.addVertex(relationship.getSource());
			}
			if (!g.containsVertex(relationship.getTarget())) {
				g.addVertex(relationship.getTarget());
			}

			g.addEdge(relationship.getSource(), relationship.getTarget(), relationship);
		}
		return g;
	}

}
