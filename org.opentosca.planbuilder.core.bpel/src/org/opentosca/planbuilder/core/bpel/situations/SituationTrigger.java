package org.opentosca.planbuilder.core.bpel.situations;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SituationTrigger {
	
	public static final String xpath_query_situationtriggers =
	        "//*[local-name()='SituationTrigger' and namespace-uri()='http://opentosca.org/situations']";

    public static final String xpath_query_situations =
        "//*[local-name()='Situation' and namespace-uri()='http://opentosca.org/situations']";

    public static final String xpath_query_situations_situationTemplateId =
        "//*[local-name()='Situation' and namespace-uri()='http://opentosca.org/situations']/*[local-name()='SituationTemplateId']";

    public static final String xpath_query_situations_thingId =
        "//*[local-name()='Situation' and namespace-uri()='http://opentosca.org/situations']/*[local-name()='ThingId']";

    public static final String xpath_query_situationtrigger_onActivation =
        "//*[local-name()='onActivation' and namespace-uri()='http://opentosca.org/situations']";

    public static final String xpath_query_situationtrigger_isSingleInstance =
        "//*[local-name()='isSingleInstance' and namespace-uri()='http://opentosca.org/situations']";

    public static final String xpath_query_situationtrigger_serviceInstanceId =
        "//*[local-name()='ServiceInstanceId' and namespace-uri()='http://opentosca.org/situations']";

    public static final String xpath_query_situationtrigger_nodeInstanceId =
        "//*[local-name()='NodeInstanceId' and namespace-uri()='http://opentosca.org/situations']";

    public static final String xpath_query_situationtrigger_interfaceName =
        "//*[local-name()='InterfaceName' and namespace-uri()='http://opentosca.org/situations']";

    public static final String xpath_query_situationtrigger_operationName =
        "//*[local-name()='OperationName' and namespace-uri()='http://opentosca.org/situations']";

    public static final String xpath_query_situationtrigger_inputParameters =
        "//*[local-name()='InputParameter' and namespace-uri()='http://opentosca.org/situations']";

    public static final String xpath_query_situationtrigger_outputParameters =
        "//*[local-name()='OutputParameter' and namespace-uri()='http://opentosca.org/situations']";


    List<Situation> situations;
    boolean onActivation;
    boolean isSingleInstance;
    String serviceInstanceId;
    String nodeInstanceId;
    String interfaceName;
    String operationName;
    List<Triplet<String, String, String>> inputParameters;

    public List<Situation> getSituations() {
        return this.situations;
    }

    public void setSituations(final List<Situation> situations) {
        this.situations = situations;
    }

    public boolean isOnActivation() {
        return this.onActivation;
    }

    public void setOnActivation(final boolean onActivation) {
        this.onActivation = onActivation;
    }

    public boolean isSingelInstance() {
        return this.isSingleInstance;
    }

    public void setSingelInstance(final boolean isSingelInstance) {
        this.isSingleInstance = isSingelInstance;
    }

    public String getServiceInstanceId() {
        return this.serviceInstanceId;
    }

    public void setServiceInstanceId(final String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public String getNodeInstanceId() {
        return this.nodeInstanceId;
    }

    public void setNodeInstanceId(final String nodeInstanceId) {
        this.nodeInstanceId = nodeInstanceId;
    }

    public String getInterfaceName() {
        return this.interfaceName;
    }

    public void setInterfaceName(final String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getOperationName() {
        return this.operationName;
    }

    public void setOperationName(final String operationName) {
        this.operationName = operationName;
    }

    public List<Triplet<String, String, String>> getInputParameters() {
        return this.inputParameters;
    }

    public void setInputParameters(final List<Triplet<String, String, String>> inputParameters) {
        this.inputParameters = inputParameters;
    }
    
  
    
    public static List<SituationTrigger> parseSituationTriggers(final AbstractServiceTemplate serviceTemplate) throws XPathExpressionException {
        final List<SituationTrigger> situationTriggers = new ArrayList<>();
        final Element properties = ModelUtils.getBoundaryPropertiesSafely(serviceTemplate);

        if (properties == null) {
            return situationTriggers;
        }

        final NodeList list = ModelUtils.queryNodeSet(properties, xpath_query_situationtriggers);

        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
                final SituationTrigger trigger = parseSituationTrigger((Element) list.item(i));
                situationTriggers.add(trigger);
            }
        }

        return situationTriggers;
    }

   

    public static SituationTrigger parseSituationTrigger(final Element situationTriggerElement) throws XPathExpressionException {

        final SituationTrigger trigger = new SituationTrigger();

        final List<Situation> situations =
            Situation.parseSituations(ModelUtils.queryNodeSet(situationTriggerElement, SituationTrigger.xpath_query_situations));

        final String onActivation =
            ModelUtils.getNodeContent(ModelUtils.queryNodeSet(situationTriggerElement,
                                        SituationTrigger.xpath_query_situationtrigger_onActivation).item(0));
        final String isSingleInstance =
        		ModelUtils.getNodeContent(ModelUtils.queryNodeSet(situationTriggerElement,
                                        SituationTrigger.xpath_query_situationtrigger_isSingleInstance).item(0));
        final String serviceInstanceId =
        		ModelUtils.getNodeContent(ModelUtils.queryNodeSet(situationTriggerElement,
                                        SituationTrigger.xpath_query_situationtrigger_serviceInstanceId).item(0));

        NodeList nodeInstanceIdList = null;
        if ((nodeInstanceIdList = ModelUtils.queryNodeSet(situationTriggerElement,
                                               SituationTrigger.xpath_query_situationtrigger_nodeInstanceId)).getLength() != 0) {
            final String nodeInstanceId = ModelUtils.getNodeContent(nodeInstanceIdList.item(0));
            trigger.setNodeInstanceId(nodeInstanceId);
        }

        final String interfaceName =
        		ModelUtils.getNodeContent(ModelUtils.queryNodeSet(situationTriggerElement,
                                        SituationTrigger.xpath_query_situationtrigger_interfaceName).item(0));
        final String operationName =
        		ModelUtils.getNodeContent(ModelUtils.queryNodeSet(situationTriggerElement,
                                        SituationTrigger.xpath_query_situationtrigger_operationName).item(0));
        final List<Triplet<String, String, String>> inputParameters =
            parseParameters(situationTriggerElement, SituationTrigger.xpath_query_situationtrigger_inputParameters);


        trigger.setSituations(situations);
        trigger.setOnActivation(Boolean.valueOf(onActivation));
        trigger.setSingelInstance(Boolean.valueOf(isSingleInstance));
        trigger.setServiceInstanceId(serviceInstanceId);
        trigger.setInterfaceName(interfaceName);
        trigger.setOperationName(operationName);
        trigger.setInputParameters(inputParameters);

        return trigger;
    }
    
    public static String createXMLLiteral(final SituationTrigger trigger) {
        /*
         * <?xml version="1.0" encoding="UTF-8"?> <SituationTrigger> <SituationId>36</SituationId>
         * <onActivation>true</onActivation> <isSingleInstance>true</isSingleInstance>
         * <ServiceInstanceId>19</ServiceInstanceId> <InterfaceName>scaleout_dockercontainer</InterfaceName>
         * <OperationName>scale-out</OperationName> <InputParameters> <InputParameter>
         * <name>ApplicationPort</name> <Value>9991</Value> <Type>String</Type> </InputParameter>
         * <InputParameter> <name>ContainerSSHPort</name> <Value></Value> <Type>String</Type>
         * </InputParameter> </InputParameters> </SituationTrigger>
         */

        final StringBuilder strB = new StringBuilder();
        // don't ask why double, but basically because of ODE engine..
        strB.append("<SituationTrigger>");
        strB.append("<SituationTrigger xmlns=\"\"><Situations>");
        for (final Situation situation : trigger.situations) {
            strB.append("<SituationId></SituationId>");
        }
        strB.append("</Situations>");
        strB.append("<onActivation>" + trigger.onActivation + "</onActivation>");
        strB.append("<isSingleInstance>" + trigger.isSingleInstance + "</isSingleInstance>");
        strB.append("<ServiceInstanceId></ServiceInstanceId>");
        if (trigger.nodeInstanceId != null) {
            strB.append("<NodeInstanceId></NodeInstanceId>");
        }
        strB.append("<InterfaceName>" + trigger.interfaceName + "</InterfaceName>");
        strB.append("<OperationName>" + trigger.operationName + "</OperationName>");

        strB.append("<InputParameters>");
        for (final Triplet<String, String, String> inputParam : trigger.inputParameters) {

            strB.append("<InputParameter>");

            strB.append("<name>" + inputParam.first + "</name>");
            strB.append("<Value>" + inputParam.second + "</Value>");
            strB.append("<Type>" + inputParam.third + "</Type>");

            strB.append("</InputParameter>");
        }
        strB.append("</InputParameters>");
        strB.append("</SituationTrigger>");
        strB.append("</SituationTrigger>");

        return strB.toString();
    }

    private static List<Triplet<String, String, String>> parseParameters(final Element situationTriggerElement,
                                                                  final String xpathQuery) throws XPathExpressionException {
        final List<Triplet<String, String, String>> parameters = new ArrayList<>();

        final NodeList parameterNodes = ModelUtils.queryNodeSet(situationTriggerElement, xpathQuery);

        for (int i = 0; i < parameterNodes.getLength(); i++) {
            final String name =
            		ModelUtils.getNodeContent(ModelUtils.queryNodeSet(parameterNodes.item(i), "//*[local-name()='name']").item(0));
            final String val =
            		ModelUtils.getNodeContent(ModelUtils.queryNodeSet(parameterNodes.item(i), "//*[local-name()='Value']").item(0));
            final String type =
            		ModelUtils.getNodeContent(ModelUtils.queryNodeSet(parameterNodes.item(i), "//*[local-name()='Type']").item(0));
            parameters.add(new Triplet<>(name, val, type));
        }


        return parameters;
    }

    

   
}