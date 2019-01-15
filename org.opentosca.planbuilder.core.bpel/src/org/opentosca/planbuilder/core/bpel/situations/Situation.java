package org.opentosca.planbuilder.core.bpel.situations;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Situation {
    final String situationTemplateId;
    final String thingId;
    final boolean fromInput;

    public Situation(final String situationTemplateId, final String thingId, final boolean fromInput) {
        this.situationTemplateId = situationTemplateId;
        this.thingId = thingId;
        this.fromInput = fromInput;
    }
    
    public static List<Situation> parseSituations(final NodeList situationElements) throws XPathExpressionException {
        final List<Situation> situations = new ArrayList<>();
        for (int i = 0; i < situationElements.getLength(); i++) {
            if (situationElements.item(i).getNodeType() == Node.ELEMENT_NODE) {
                final Element situationElement = (Element) situationElements.item(i);
                final String situationTemplateId =
                		ModelUtils.getNodeContent(ModelUtils.queryNodeSet(situationElement,
                                                SituationTrigger.xpath_query_situations_situationTemplateId).item(0));
                final String thingId =
                		ModelUtils.getNodeContent(ModelUtils.queryNodeSet(situationElement,
                                                SituationTrigger.xpath_query_situations_thingId).item(0));
                situations.add(new Situation(situationTemplateId, thingId,
                    Boolean.valueOf(situationElement.getAttribute("fromInput"))));
            }
        }

        return situations;
    }
}