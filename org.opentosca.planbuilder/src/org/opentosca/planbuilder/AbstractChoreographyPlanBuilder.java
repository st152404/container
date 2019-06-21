/**
 * Copyright 2019 IAAS University of Stuttgart <br>
 */
package org.opentosca.planbuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.opentosca.container.core.tosca.convention.Types;
import org.opentosca.planbuilder.model.plan.AbstractPlan.PlanType;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractTopologyTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Frank - st152404@stud.uni-stuttgart.de <br>
 *         <b>Description:</b> This class splits a topology according to the provided splits and
 *         generates a plan for every partner.
 *
 */
public abstract class AbstractChoreographyPlanBuilder extends AbstractSimplePlanBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractChoreographyPlanBuilder.class);

    @Override
    public PlanType createdPlanType() {
        return PlanType.MANAGE;
    }

    /**
     * Returns all existing partner definitions
     *
     * @param defs The {@link AbstractDefinitions}
     * @return A lsit of {@link PartnerDefinition}
     */
    public static List<PartnerDefinition> getPartnerDefinitions(final AbstractDefinitions defs) {
        final List<PartnerDefinition> partnerDefinitions = new ArrayList<>();
        final List<AbstractServiceTemplate> serviceTemplates = defs.getServiceTemplates();
        for (final AbstractServiceTemplate abstractServiceTemplate : serviceTemplates) {
            final AbstractTopologyTemplate topologyTemplate = abstractServiceTemplate.getTopologyTemplate();
            if (AbstractChoreographyPlanBuilder.isValidTopology(topologyTemplate)) {
                final Set<String> splitLabels = getSplitLabels(topologyTemplate);
                for (final String label : splitLabels) {
                    final PartnerDefinition partnerDefinition = new PartnerDefinition(label, topologyTemplate);
                    partnerDefinitions.add(partnerDefinition);
                }
            }
        }

        return partnerDefinitions;
    }

    /**
     * Returns the Split Labels of a given Topology
     *
     * @param topologyTemplate The {@link AbstractTopologyTemplate}
     * @return Set of Labels or an empty Set
     */
    protected static Set<String> getSplitLabels(final AbstractTopologyTemplate topologyTemplate) {
        return topologyTemplate.getNodeTemplates().stream().map(nodeTemplate -> {
            final Optional<String> splitLabel = nodeTemplate.getSplitLabel();
            if (!splitLabel.isPresent()) {
                throw new IllegalArgumentException(nodeTemplate.getId() + " has no label");
            }

            final String label = splitLabel.get();
            return label;
        }).collect(Collectors.toSet());
    }

    /**
     * Implementation of the Validation Check Algorithm by Saatkamp et. al (Topology Splitting and
     * Matching for Multi-Cloud Deployments)
     *
     * @param topology A Topology
     * @return True if the topology is valid, false otherwise
     */
    protected static boolean isValidTopology(final AbstractTopologyTemplate topology) {
        final Set<AbstractNodeTemplate> rootNodes = new HashSet<>();
        final List<AbstractNodeTemplate> nodeTemplates = topology.getNodeTemplates();
        final Set<String> splitLabels = getSplitLabels(topology);
        // Topology should contain at least 2 partners/labels
        if (splitLabels.size() < 2) {
            return false;
        }

        // Find predecessor withouth hostedOn ingoing relations
        for (final AbstractNodeTemplate abstractNodeTemplate : nodeTemplates) {
            final List<AbstractRelationshipTemplate> ingoingRelations = abstractNodeTemplate.getIngoingRelations();
            final boolean emptyRelations = ingoingRelations.isEmpty();
            final boolean hasLabel = abstractNodeTemplate.getSplitLabel().isPresent();
            final boolean hasNoHostedOnRelations =
                ingoingRelations.stream().noneMatch(rs -> rs.getType().equals(Types.hostedOnRelationType));

            // Root nodes always must have a label
            if (emptyRelations || hasNoHostedOnRelations) {
                if (hasLabel) {
                    rootNodes.add(abstractNodeTemplate);
                } else {
                    return false;
                }
            }
        }

        // Check child nodes of root nodes
        for (final AbstractNodeTemplate rootNode : rootNodes) {
            final String label = rootNode.getSplitLabel().get();
            final boolean hasChildNodeLabels = hasChildNodeLabels(rootNode, label);
            if (!hasChildNodeLabels) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the Node and its child node have labels. Successors of a node must all have the same
     * label as the root node. Only Relationship of Type hostedOn are checked. This method is called
     * recursively.
     *
     * @param node A {@link AbstractNodeTemplate}
     * @param label The Label of the Partner
     * @return true if the node and its children have the same label
     */
    protected static boolean hasChildNodeLabels(final AbstractNodeTemplate node, final String label) {
        final Optional<String> splitLabel = node.getSplitLabel();
        final boolean result = true;
        if (!splitLabel.isPresent()) {
            return false;
        }

        final boolean sameLabels = splitLabel.get().equals(label);

        if (!sameLabels) {
            return false;
        }

        final List<AbstractRelationshipTemplate> outgoingRelations = node.getOutgoingRelations();
        for (final AbstractRelationshipTemplate abstractRelationshipTemplate : outgoingRelations) {
            final QName type = abstractRelationshipTemplate.getType();
            if (type.equals(Types.hostedOnRelationType)) {
                final AbstractNodeTemplate target = abstractRelationshipTemplate.getTarget();
                final boolean childNodesAreLabeled = hasChildNodeLabels(target, label);
                if (!childNodesAreLabeled) {
                    return false;
                }
            }
        }

        return true;
    }
}
