package org.opentosca.planbuilder.core.bpel.situations;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SituationGuard {

	public static final String xpath_query_situationguards = "//*[local-name()='SituationGuard' and namespace-uri()='http://opentosca.org/situations']";

	public static final String xpath_query_situations = "//*[local-name()='Situation' and namespace-uri()='http://opentosca.org/situations']";

	public static final String xpath_query_situations_situationTemplateId = "//*[local-name()='Situation' and namespace-uri()='http://opentosca.org/situations']/*[local-name()='SituationTemplateId']";

	public static final String xpath_query_situations_thingId = "//*[local-name()='Situation' and namespace-uri()='http://opentosca.org/situations']/*[local-name()='ThingId']";

	public static final String xpath_query_situationguard_onActive = "//*[local-name()='onActive' and namespace-uri()='http://opentosca.org/situations']";

	public static final String xpath_query_situationguard_isSingleInstance = "//*[local-name()='isSingleInstance' and namespace-uri()='http://opentosca.org/situations']";

	public static final String xpath_query_situationguard_serviceInstanceId = "//*[local-name()='ServiceInstanceId' and namespace-uri()='http://opentosca.org/situations']";

	public static final String xpath_query_situationguard_nodeInstanceId = "//*[local-name()='NodeInstanceId' and namespace-uri()='http://opentosca.org/situations']";

	public static final String xpath_query_situationguard_interfaceName = "//*[local-name()='InterfaceName' and namespace-uri()='http://opentosca.org/situations']";

	public static final String xpath_query_situationguard_operationName = "//*[local-name()='OperationName' and namespace-uri()='http://opentosca.org/situations']";

	public static final String xpath_query_situationguard_inputParameters = "//*[local-name()='InputParameter' and namespace-uri()='http://opentosca.org/situations']";

	public static final String xpath_query_situationguard_outputParameters = "//*[local-name()='OutputParameter' and namespace-uri()='http://opentosca.org/situations']";

	private List<Situation> situations;
	private boolean onActivation;
	private boolean isSingleInstance;
	private String serviceInstanceId;
	private String nodeInstanceId;
	private String interfaceName;
	private String operationName;

	public List<Situation> getSituations() {
		return situations;
	}

	public void setSituations(List<Situation> situations) {
		this.situations = situations;
	}

	public boolean isOnActivation() {
		return onActivation;
	}

	public void setOnActivation(boolean onActivation) {
		this.onActivation = onActivation;
	}

	public boolean isSingleInstance() {
		return isSingleInstance;
	}

	public void setSingleInstance(boolean isSingleInstance) {
		this.isSingleInstance = isSingleInstance;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	public String getNodeInstanceId() {
		return nodeInstanceId;
	}

	public void setNodeInstanceId(String nodeInstanceId) {
		this.nodeInstanceId = nodeInstanceId;
	}

	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public String getOperationName() {
		return operationName;
	}

	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	public static String getXpathQuerySituations() {
		return xpath_query_situations;
	}

	public static String getXpathQuerySituationsSituationtemplateid() {
		return xpath_query_situations_situationTemplateId;
	}

	public static String getXpathQuerySituationsThingid() {
		return xpath_query_situations_thingId;
	}

	public static String getXpathQuerySituationtriggerOnactivation() {
		return xpath_query_situationguard_onActive;
	}

	public static String getXpathQuerySituationtriggerIssingleinstance() {
		return xpath_query_situationguard_isSingleInstance;
	}

	public static String getXpathQuerySituationtriggerServiceinstanceid() {
		return xpath_query_situationguard_serviceInstanceId;
	}

	public static String getXpathQuerySituationtriggerNodeinstanceid() {
		return xpath_query_situationguard_nodeInstanceId;
	}

	public static String getXpathQuerySituationtriggerInterfacename() {
		return xpath_query_situationguard_interfaceName;
	}

	public static String getXpathQuerySituationtriggerOperationname() {
		return xpath_query_situationguard_operationName;
	}

	public static String getXpathQuerySituationtriggerInputparameters() {
		return xpath_query_situationguard_inputParameters;
	}

	public static String getXpathQuerySituationtriggerOutputparameters() {
		return xpath_query_situationguard_outputParameters;
	}

	public static List<SituationGuard> parseSituationGuards(final AbstractServiceTemplate serviceTemplate)
			throws XPathExpressionException {
		final List<SituationGuard> situationTriggers = new ArrayList<>();
		final Element properties = ModelUtils.getBoundaryPropertiesSafely(serviceTemplate);

		if (properties == null) {
			return situationTriggers;
		}

		final NodeList list = ModelUtils.queryNodeSet(properties, xpath_query_situationguards);

		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				final SituationGuard trigger = parseSituationGuard((Element) list.item(i));
				situationTriggers.add(trigger);
			}
		}

		return situationTriggers;
	}

	public static SituationGuard parseSituationGuard(final Element situationGuardElement)
			throws XPathExpressionException {

		final SituationGuard guard = new SituationGuard();

		final List<Situation> situations = Situation.parseSituations(
				ModelUtils.queryNodeSet(situationGuardElement, SituationGuard.xpath_query_situations));

		final String onActivation = ModelUtils.getNodeContent(ModelUtils
				.queryNodeSet(situationGuardElement, SituationGuard.xpath_query_situationguard_onActive)
				.item(0));
		final String serviceInstanceId = ModelUtils.getNodeContent(ModelUtils
				.queryNodeSet(situationGuardElement, SituationGuard.xpath_query_situationguard_serviceInstanceId)
				.item(0));

		NodeList nodeInstanceIdList = null;
		if ((nodeInstanceIdList = ModelUtils.queryNodeSet(situationGuardElement,
				SituationGuard.xpath_query_situationguard_nodeInstanceId)).getLength() != 0) {
			final String nodeInstanceId = ModelUtils.getNodeContent(nodeInstanceIdList.item(0));
			guard.setNodeInstanceId(nodeInstanceId);
		}

		final String interfaceName = ModelUtils.getNodeContent(ModelUtils
				.queryNodeSet(situationGuardElement, SituationGuard.xpath_query_situationguard_interfaceName)
				.item(0));
		final String operationName = ModelUtils.getNodeContent(ModelUtils
				.queryNodeSet(situationGuardElement, SituationGuard.xpath_query_situationguard_operationName)
				.item(0));

		guard.setSituations(situations);
		guard.setOnActivation(Boolean.valueOf(onActivation));
		guard.setServiceInstanceId(serviceInstanceId);
		guard.setInterfaceName(interfaceName);
		guard.setOperationName(operationName);

		return guard;
	}
	
	public static String createXMLLiteral(final SituationGuard guard) {
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
        strB.append("<SituationGuard>");
        strB.append("<SituationGuard xmlns=\"\"><Situations>");
        for (final Situation situation : guard.situations) {
        	// some situation ids are only known at runtime, e.g., given as input
            strB.append("<SituationId></SituationId>");
        }
        strB.append("</Situations>");
        strB.append("<onActivation>" + guard.onActivation + "</onActivation>");
        strB.append("<ServiceInstanceId></ServiceInstanceId>");
        if (guard.nodeInstanceId != null) {
            strB.append("<NodeInstanceId></NodeInstanceId>");
        }
        strB.append("<InterfaceName>" + guard.interfaceName + "</InterfaceName>");
        strB.append("<OperationName>" + guard.operationName + "</OperationName>");


        strB.append("</SituationGuard>");
        strB.append("</SituationGuard>");

        return strB.toString();
    }
}