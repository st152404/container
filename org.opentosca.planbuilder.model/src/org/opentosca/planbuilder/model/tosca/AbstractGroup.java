package org.opentosca.planbuilder.model.tosca;

import java.util.List;

import javax.xml.namespace.QName;

public abstract class AbstractGroup {


    public abstract AbstractProperties getProperties();

    public abstract String getId();

    public abstract String getName();

    public abstract QName getType();

    public abstract List<AbstractNodeTemplate> getNodeTemplates();


}
