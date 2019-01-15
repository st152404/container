package org.opentosca.container.api.dto.situations;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentosca.container.api.dto.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

@XmlRootElement(name = "SituationGuardResources")
public class SituationGuardListDTO extends ResourceSupport {

	 @JsonProperty
	    @XmlElement(name = "SituationGuard")
	    @XmlElementWrapper(name = "SituationGuards")
	    private final List<SituationGuardDTO> situationGuards = Lists.newArrayList();


	    public List<SituationGuardDTO> getSituationTriggers() {
	        return this.situationGuards;
	    }

	    public void add(final SituationGuardDTO... situations) {
	        this.situationGuards.addAll(Arrays.asList(situations));
	    }
}
