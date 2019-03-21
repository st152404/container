package org.opentosca.planbuilder.core.bpel;

import java.util.List;

import org.opentosca.planbuilder.model.tosca.AbstractImplementationArtifact;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractOperation;
import org.opentosca.planbuilder.model.tosca.AbstractParameter;

/**
 * As some IAs may implement a whole interface we mock the matching of these kind of IAs with this
 * dummy class
 *
 * @author Kálmán Képes - kalman.kepes@iaas.uni-stuttgart.de
 *
 */
class InterfaceDummy extends AbstractOperation {

    private final AbstractImplementationArtifact ia;
    private final AbstractNodeTemplate nodeTemplate;

    public InterfaceDummy(final AbstractNodeTemplate nodeTemplate, final AbstractImplementationArtifact ia) {
        this.ia = ia;
        this.nodeTemplate = nodeTemplate;
    }

    /**
     * Return the AbstractOperation for the given name if an operation with that name is available
     * in the interface represented by this InterfaceDummy.
     *
     * @param opName the name of the operation
     * @return an AbstractOperation with the given name, or else null
     */
    public AbstractOperation getOperation(final String opName) {
        return this.nodeTemplate.getType().getInterfaces().stream()
                                .filter(iface -> iface.getName().equals(this.ia.getInterfaceName()))
                                .flatMap(iface -> iface.getOperations().stream())
                                .filter(op -> op.getName().equals(opName)).findFirst().orElse(null);
    }

    public AbstractNodeTemplate getNodeTemplate() {
        return this.nodeTemplate;
    }

    public AbstractImplementationArtifact getIA() {
        return this.ia;
    }

    @Override
    public String getName() {
        return this.ia.getInterfaceName();
    }

    @Override
    public List<AbstractParameter> getInputParameters() {
        return null;
    }

    @Override
    public List<AbstractParameter> getOutputParameters() {
        return null;
    }
}
