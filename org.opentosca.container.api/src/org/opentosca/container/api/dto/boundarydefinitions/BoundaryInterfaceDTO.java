package org.opentosca.container.api.dto.boundarydefinitions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentosca.container.api.dto.ResourceSupport;

@XmlRootElement(name = "Interface")
public class BoundaryInterfaceDTO extends ResourceSupport {

	
    private String name;

    private Map<String, BoundaryOperationDTO> operations;


    @XmlAttribute
    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @XmlElement(name = "Operation")
    @XmlElementWrapper(name = "Operations")
    public Map<String, BoundaryOperationDTO> getOperations() {
        return this.operations;
    }

    public void setOperations(final Map<String, BoundaryOperationDTO> operations) {
        this.operations = operations;
    }
}
