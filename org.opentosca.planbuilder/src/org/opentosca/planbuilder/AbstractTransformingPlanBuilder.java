package org.opentosca.planbuilder;

import java.util.List;

import javax.xml.namespace.QName;

import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;

/**
 * @author Kalman Kepes - kepes@iaas.uni-stuttgart.de
 *
 */
public abstract class AbstractTransformingPlanBuilder extends AbstractPlanBuilder {


    /**
     * <p>
     * Create a single transforming plan for the given Service Templates. A transforming plan takes as
     * input an service instance of the referenced service templates and makes "any kind" of model and
     * instance transformation from the current service template instance to a desired service template
     * instance.
     * </p>
     *
     * @param currentCsarName the name of the csar the tranformation should be made from
     * @param currentDefinitions the definitions the tranformation should be made from
     * @param currentServiceTemplate the service template the tranformation should be made from
     * @param desiredCsarName the name of the csar the tranformation has as goal
     * @param desiredDefinitions the definitions containing the desirec service template model
     * @param desiredServiceTemplate the service template to be used as the goal for the transformation
     * @return a Transformation Plan that is able to to transform (any kind of transformation) a given
     *         service template instance into another
     */
    public abstract AbstractPlan buildPlan(String currentCsarName, AbstractDefinitions currentDefinitions,
                                           QName currentServiceTemplate, String desiredCsarName,
                                           AbstractDefinitions desiredDefinitions, QName desiredServiceTemplate);

    /**
     * <p>
     * Creates transforming plans for the given Service Templates. A transforming plan takes as input an
     * service instance of the referenced service templates and makes "any kind" of model and instance
     * transformation from the current service template instance to a desired service template instance.
     * </p>
     *
     * @param currentCsarName the name of the csar the tranformation should be made from
     * @param currentDefinitions the definitions the tranformation should be made from
     * @param currentServiceTemplate the service template the tranformation should be made from
     * @param desiredCsarName the name of the csar the tranformation has as goal
     * @param desiredDefinitions the definitions containing the desirec service template model
     * @param desiredServiceTemplate the service template to be used as the goal for the transformation
     * @return a List of Transformation Plans that are able to to transform (any kind of transformation)
     *         a given service template instance into another
     */
    public abstract List<AbstractPlan> buildPlans(String currentCsarName, AbstractDefinitions currentDefinitions,
                                                  QName currentServiceTemplate, String desiredCsarName,
                                                  AbstractDefinitions desiredDefinitions, QName desiredServiceTemplate);

}
