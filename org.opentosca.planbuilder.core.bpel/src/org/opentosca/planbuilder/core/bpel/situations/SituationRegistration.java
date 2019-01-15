package org.opentosca.planbuilder.core.bpel.situations;

import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.opentosca.planbuilder.core.bpel.fragments.BPELProcessFragments;
import org.opentosca.planbuilder.core.bpel.handlers.BPELPlanHandler;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan.VariableType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public abstract class SituationRegistration {

	final BPELProcessFragments fragments;
	final BPELPlanHandler handler;

	public SituationRegistration() throws ParserConfigurationException {
		this.fragments = new BPELProcessFragments();
		this.handler = new BPELPlanHandler();
	}

	void appendAssignSituationIdFromInputToSituationIdVar(final BPELPlan plan, final String inputFieldName,
			final int situationIndex, final String varName, final Element elementToAppendBefore)
			throws IOException, SAXException {
		final String xpathQuery1 = "//*[local-name()='" + inputFieldName + "']/text()";
		final String xpathQuery2 = "text()";

		Node assignNode = this.fragments.createAssignVarToVarWithXpathQueriesAsNode("AssignSituationIdFromInputToVar",
				"input", "payload", varName, null, xpathQuery1, xpathQuery2,
				"Assigning the SituationId of a SituationTrigger based on the input variable to situationId Variable",
				null);

		assignNode = this.handler.importNode(plan, assignNode);

		elementToAppendBefore.getParentNode().insertBefore(assignNode, elementToAppendBefore);

	}

	void appendAssignSituationidFromInput(final BPELPlan plan, final String inputFieldName,
			final String situationTriggerRequestVar, final Element elementToAppendBefore, final int situationIndex)
			throws IOException, SAXException {

		final String xpathQuery1 = "//*[local-name()='" + inputFieldName + "']/text()";
		final String xpathQuery2 = "//*[local-name()='SituationId'][" + (situationIndex + 1) + "]";

		Node assignNode = this.fragments.createAssignVarToVarWithXpathQueriesAsNode("AssignSituationIdFromInput",
				"input", "payload", situationTriggerRequestVar, null, xpathQuery1, xpathQuery2,
				"Assigning the SituationId of a SituationTrigger based on the input variable", null);

		assignNode = this.handler.importNode(plan, assignNode);

		elementToAppendBefore.getParentNode().insertBefore(assignNode, elementToAppendBefore);

	}

	void appendAssignTransformXmltoString(final BPELPlan plan, final String xmlVar, final String stringVar,
			final Element elementToAppendBefore) throws IOException, SAXException {
		final String xpathQuery1 = "ode:dom-to-string(\\$" + xmlVar + "/*[local-name()='SituationTrigger'])";
		final String xpathQuery2 = "\\$" + stringVar;

		Node assign = this.fragments.createAssignVarToVarWithXpathQueriesAsNode("transformXMLtoStringVar", xmlVar, null,
				stringVar, null, xpathQuery1, xpathQuery2,
				"Transforms one xml var to a string var as ODE sets a an xml element as wrapper around complex type when using the rest extension.",
				new QName("http://www.apache.org/ode/type/extension", "ode", "ode"));
		assign = this.handler.importNode(plan, assign);
		elementToAppendBefore.getParentNode().insertBefore(assign, elementToAppendBefore);
	}

	void appendAssignRESTPOST(final BPELPlan plan, final String urlVar, final String requestVar,
			final String responseVar, final Element elementToAppendBefore) throws IOException, SAXException {
		Node restCall = this.fragments.generateBPEL4RESTLightServiceInstancePOSTAsNode(urlVar, requestVar, responseVar);
		restCall = this.handler.importNode(plan, restCall);
		elementToAppendBefore.getParentNode().insertBefore(restCall, elementToAppendBefore);
	}

	void appendAssignSituationsAPIURLVar(final BPELPlan plan, final String varName, final String partName,
			final String situationsAPIINputFieldName, final String situationsAPIVarName,
			final Element elementToAppendBefore) throws IOException, SAXException {

		final String xpathQuery1 = "//*[local-name()='" + situationsAPIINputFieldName + "']/text()";
		final String xpathQuery2 = "\\$" + situationsAPIVarName;
		Node assign = this.fragments.createAssignVarToVarWithXpathQueriesAsNode("AssignSituationsAPIUrl", varName,
				partName, situationsAPIVarName, null, xpathQuery1, xpathQuery2,
				"Assigns the SituationsAPIURL from Input to the designated Variable", null);
		assign = this.handler.importNode(plan, assign);
		elementToAppendBefore.getParentNode().insertBefore(assign, elementToAppendBefore);

	}

	void appendAssignServiceInstanceIdFromServiceInstanceIdVar(final BPELPlan plan,
			final String serviceInstanceIdVarName, final String situationTriggerReqVarName,
			final Element elementToAppendBefore) throws IOException, SAXException {
		final String xpathQuery1 = "text()";
		final String xpathQuery2 = "//*[local-name()='ServiceInstanceId']";
		Node assign = this.fragments.createAssignVarToVarWithXpathQueriesAsNode(
				"assignSituationTriggerReqWithServiceInstanceID", serviceInstanceIdVarName, null,
				situationTriggerReqVarName, null, xpathQuery1, xpathQuery2,
				"Assign the ServiceInstanceID of SituationTrigger Request from ServiceInstanceID inside this BuildPlan",
				null);

		assign = this.handler.importNode(plan, assign);
		elementToAppendBefore.getParentNode().insertBefore(assign, elementToAppendBefore);
	}

	void appendAssignLiteralVariable(final BPELPlan plan, final String xmlLiteral, final String varName,
			final Element elementToAppendBefore) throws IOException, SAXException {

		final QName anyDecl = new QName("http://www.w3.org/2001/XMLSchema", "anyType", "xsd");
		this.handler.importNamespace(anyDecl, plan);

		this.handler.addVariable(varName, VariableType.TYPE, anyDecl, plan);

		Node node = this.fragments.createAssignVarWithLiteralAsNode(xmlLiteral, varName,
				"Appending the initial xml body of a situationtrigger which will be used for registering such a trigger");

		node = this.handler.importNode(plan, node);
		elementToAppendBefore.getParentNode().insertBefore(node, elementToAppendBefore);
	}

}
