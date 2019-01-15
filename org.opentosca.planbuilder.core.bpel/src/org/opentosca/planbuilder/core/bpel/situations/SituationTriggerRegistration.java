package org.opentosca.planbuilder.core.bpel.situations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.opentosca.planbuilder.core.bpel.fragments.BPELProcessFragments;
import org.opentosca.planbuilder.core.bpel.handlers.BPELPlanHandler;
import org.opentosca.planbuilder.core.bpel.helpers.ServiceInstanceVariablesHandler;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan.VariableType;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class SituationTriggerRegistration extends SituationRegistration {

	public SituationTriggerRegistration() throws ParserConfigurationException {
		super();
	}

	public boolean handle(final AbstractServiceTemplate serviceTemplate, final BPELPlan plan) {

		try {
			// parse triggers
			final List<SituationTrigger> triggers = SituationTrigger.parseSituationTriggers(serviceTemplate);

			for (final SituationTrigger trigger : triggers) {
				// create xml literal, add variable, assign variable
				final String xmlLiteral = SituationTrigger.createXMLLiteral(trigger);

				final String varName = "SituationTriggerRegistration_" + System.currentTimeMillis();

				appendAssignLiteralVariable(plan, xmlLiteral, varName, plan.getBpelMainFlowElement());

				final List<String> situationIdVarNames = new ArrayList<>();

				for (int i = 0; i < trigger.situations.size(); i++) {
					// TODO set situationId
					final Situation situation = trigger.situations.get(i);
					if (situation.fromInput) {
						final String inputName = "SituationId_" + i + "_" + situation.situationTemplateId;
						this.handler.addStringElementToPlanRequest(inputName, plan);
						appendAssignSituationidFromInput(plan, inputName, varName, plan.getBpelMainFlowElement(), i);
						final String situationIdVarName = this.handler
								.addGlobalStringVariable("SituationId_" + i + "_var", plan);
						appendAssignSituationIdFromInputToSituationIdVar(plan, inputName, i, situationIdVarName,
								plan.getBpelMainFlowElement());
						situationIdVarNames.add(situationIdVarName);
					} else {
						// TODO Add automated (dynamic?) selection of SituationId
					}

				}

				if (trigger.serviceInstanceId.equals("Build")) {
					// fetch serviceInstance from buildPlan
					final String serviceInstanceIdVar = ServiceInstanceVariablesHandler
							.findServiceInstanceIdVarName(this.handler, plan);
					appendAssignServiceInstanceIdFromServiceInstanceIdVar(plan, serviceInstanceIdVar, varName,
							plan.getBpelMainFlowElement());
				}

				// optional TODO set nodeInstance selection

				// create REST call

				final String situationsAPI = "SituationsAPI_Triggers_URL";
				this.handler.addStringElementToPlanRequest(situationsAPI, plan);
				final String situationsAPIVarName = "SituationsAPI_Triggers_URL" + System.currentTimeMillis();

				final String situationsAPIVar = this.handler.addGlobalStringVariable(situationsAPIVarName, plan);

				appendAssignSituationsAPIURLVar(plan, "input", "payload", situationsAPI, situationsAPIVar,
						plan.getBpelMainFlowElement());

				final String stringReqVar = this.handler
						.addGlobalStringVariable("situationRegistrationStringVar" + System.currentTimeMillis(), plan);

				appendAssignTransformXmltoString(plan, varName, stringReqVar, plan.getBpelMainFlowElement());

				appendAssignRESTPOST(plan, situationsAPIVar, stringReqVar,
						this.handler.addGlobalStringVariable("SituationRegistrationResponse", plan),
						plan.getBpelMainFlowElement());
			}

		} catch (final XPathExpressionException e) {
			e.printStackTrace();
			return false;
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		} catch (final SAXException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean canHandle(final AbstractServiceTemplate serviceTemplate, final BPELPlan plan) {

		List<SituationTrigger> triggers = new ArrayList<>();
		try {
			triggers = SituationTrigger.parseSituationTriggers(serviceTemplate);
		} catch (final XPathExpressionException e) {
			e.printStackTrace();
		}

		if (triggers.size() == 0) {
			return false;
		}

		return true;
	}

}
