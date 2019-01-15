package org.opentosca.container.api.dto.boundarydefinitions;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentosca.container.api.dto.ResourceSupport;
import org.opentosca.container.core.tosca.extension.TParameter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Kalman Kepes - kepeskn@iaas.uni-stuttgart.de
 *
 */
@XmlRootElement(name = "Invoke")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoundaryOperationDTO extends ResourceSupport {
	
    private String id;
	
	private String type;
	
    public String getId() {
        return this.id;
    }

    public void setId(final String id) {
        this.id = id;
    }
    
    public String getType() {
    	return this.type;
    }
    
    public void setType(String type) {
    	this.type = type;
    }

	@XmlElement(name = "InputParameter")
	@XmlElementWrapper(name = "InputParameters")
	private List<TParameter> inputParameters = Lists.newArrayList();

	@ApiModelProperty(name = "input_parameters")
	public List<TParameter> getInputParameters() {
		return this.inputParameters;
	}

	public void setInputParameters(final List<TParameter> inputParameters) {
		this.inputParameters = inputParameters;
	}

}
