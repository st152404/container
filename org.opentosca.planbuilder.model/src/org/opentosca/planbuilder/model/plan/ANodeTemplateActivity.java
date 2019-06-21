package org.opentosca.planbuilder.model.plan;

import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;

/**
 * Copyright 2017 IAAS University of Stuttgart <br>
 * <br>
 *
 * @author Kálmán Képes - kalman.kepes@iaas.uni-stuttgart.de
 *
 */
public class ANodeTemplateActivity extends AbstractActivity {

    private final AbstractNodeTemplate nodeTemplate;


    public ANodeTemplateActivity(final String id, final ActivityType type, final AbstractNodeTemplate nodeTemplate) {
        super(id, type);
        this.nodeTemplate = nodeTemplate;
    }

    public ANodeTemplateActivity(final AbstractNodeTemplate abstractNodeTemplate, final ActivityType activityType) {
        this(ANodeTemplateActivity.generateId(abstractNodeTemplate, activityType), activityType, abstractNodeTemplate);
    }

    private static String generateId(final AbstractNodeTemplate abstractNodeTemplate, final ActivityType activityType) {
        final String result = abstractNodeTemplate.getId();
        return result + "_" + getIdSuffix(activityType);
    }

    public AbstractNodeTemplate getNodeTemplate() {
        return this.nodeTemplate;
    }

}
