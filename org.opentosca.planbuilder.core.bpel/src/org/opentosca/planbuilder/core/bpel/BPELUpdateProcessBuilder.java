/**
 *
 */
package org.opentosca.planbuilder.core.bpel;

import java.util.List;

import javax.xml.namespace.QName;

import org.opentosca.planbuilder.AbstractUpdatePlanBuilder;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;

/**
 * @author Kalman Kepes - kepes@iaas.uni-stuttgart.de
 *
 */
public class BPELUpdateProcessBuilder extends AbstractUpdatePlanBuilder {

    /*
     * (non-Javadoc)
     *
     * @see org.opentosca.planbuilder.AbstractTransformingPlanBuilder#buildPlan(java.lang.String,
     * org.opentosca.planbuilder.model.tosca.AbstractDefinitions, javax.xml.namespace.QName,
     * java.lang.String, org.opentosca.planbuilder.model.tosca.AbstractDefinitions,
     * javax.xml.namespace.QName)
     */
    @Override
    public AbstractPlan buildPlan(final String currentCsarName, final AbstractDefinitions currentDefinitions,
                                  final QName currentServiceTemplate, final String desiredCsarName,
                                  final AbstractDefinitions desiredDefinitions, final QName desiredServiceTemplate) {
        final AbstractPlan plan = generateUOG(currentCsarName, currentDefinitions, currentServiceTemplate,
                                              desiredCsarName, desiredDefinitions, desiredServiceTemplate);
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opentosca.planbuilder.AbstractTransformingPlanBuilder#buildPlans(java.lang.String,
     * org.opentosca.planbuilder.model.tosca.AbstractDefinitions, javax.xml.namespace.QName,
     * java.lang.String, org.opentosca.planbuilder.model.tosca.AbstractDefinitions,
     * javax.xml.namespace.QName)
     */
    @Override
    public List<AbstractPlan> buildPlans(final String currentCsarName, final AbstractDefinitions currentDefinitions,
                                         final QName currentServiceTemplate, final String desiredCsarName,
                                         final AbstractDefinitions desiredDefinitions,
                                         final QName desiredServiceTemplate) {
        // TODO Auto-generated method stub
        return null;
    }

}
