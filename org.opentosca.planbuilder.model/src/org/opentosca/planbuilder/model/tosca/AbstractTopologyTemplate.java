package org.opentosca.planbuilder.model.tosca;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * This class represents a TOSCA TopologyTemplate
 * </p>
 * Copyright 2019 IAAS University of Stuttgart <br>
 * <br>
 *
 * @author Kalman Kepes - kepeskn@studi.informatik.uni-stuttgart.de
 *
 */
public abstract class AbstractTopologyTemplate {

    /**
     * Returns the NodeTemplates of this TopologyTemplate
     *
     * @return a List of AbstractNodeTemplate
     */
    public abstract List<AbstractNodeTemplate> getNodeTemplates();

    /**
     * Returns the RelationshipTemplate of this TopologyTemplate
     *
     * @return a List of AbstractRelationshipTemplate
     */
    public abstract List<AbstractRelationshipTemplate> getRelationshipTemplates();

    /**
     * Returns all NodeTemplates which can be considered as sources
     *
     * @return a List of AbstractNodeTemplates that have no incident RelationshipTemplates
     */
    public List<AbstractNodeTemplate> getSources() {
        return getNodeTemplates().stream().filter(template -> template.getIngoingRelations().isEmpty())
                                 .collect(Collectors.toList());
    }

    /**
     * Returns all NodeTemplates which could be considered as sinks
     *
     * @return a List of AbstractNodeTemplates that have no adjacent RelationshipTemplates
     */
    public List<AbstractNodeTemplate> getSinks() {
        return getNodeTemplates().stream().filter(template -> template.getOutgoingRelations().isEmpty())
                                 .collect(Collectors.toList());
    }
}
