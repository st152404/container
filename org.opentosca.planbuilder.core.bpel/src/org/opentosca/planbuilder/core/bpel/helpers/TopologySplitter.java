package org.opentosca.planbuilder.core.bpel.helpers;

import java.util.ArrayList;
import java.util.List;

import org.opentosca.planbuilder.model.plan.TopologyFragment;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;

/**
 * This class can be used to split the topology of a ServiceTemplate into multiple fragments for
 * which independent plans can be created. Therefore, monolythic plans can be replaced by plans with
 * finer granularity.<br>
 * <br>
 *
 * Copyright 2019 IAAS University of Stuttgart
 */
public class TopologySplitter {

    public static List<TopologyFragment> splitTopologyHorizontally(final AbstractDefinitions definitions,
                                                                   final AbstractServiceTemplate serviceTemplate) {
        return new ArrayList<>();
        // TODO
    }
}
