package org.opentosca.planbuilder.core.bpel.situations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.opentosca.planbuilder.core.bpel.helpers.ServiceInstanceVariablesHandler;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;

import org.xml.sax.SAXException;

/**
 * @author Kalman Kepes - kepes@iaas.uni-stuttgart.de
 *
 */
public class SituationGuardRegistration extends SituationRegistration {

	public SituationGuardRegistration() throws ParserConfigurationException {
		super();
	}

	public boolean handle(AbstractServiceTemplate serviceTemplate, BPELPlan plan) {

		// parse guards

		try {
			final List<SituationGuard> guards = SituationGuard.parseSituationGuards(serviceTemplate);

			for (SituationGuard guard : guards) {

				// check if guard corresponds to the generated plan
				if (!guard.getInterfaceName().equals(plan.getTOSCAInterfaceName())
						&& !guard.getOperationName().equals(plan.getTOSCAOperationName())) {
					continue;
				}

				String xmlLiteral = SituationGuard.createXMLLiteral(guard);

				String varName = "SituationGuardRegistration_" + System.currentTimeMillis();

				appendAssignLiteralVariable(plan, xmlLiteral, varName, plan.getBpelMainFlowElement());

				final List<String> situationIdVarNames = new ArrayList<>();

				for (int i = 0; i < guard.getSituations().size(); i++) {
					// TODO set situationId
					final Situation situation = guard.getSituations().get(i);
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

				if (guard.getServiceInstanceId().equals("Build")) {
					// fetch serviceInstance from buildPlan
					final String serviceInstanceIdVar = ServiceInstanceVariablesHandler
							.findServiceInstanceIdVarName(this.handler, plan);
					appendAssignServiceInstanceIdFromServiceInstanceIdVar(plan, serviceInstanceIdVar, varName,
							plan.getBpelMainFlowElement());
				}

				// optional TODO set nodeInstance selection

				final String situationsAPI = "SituationsAPI_Guards_URL";
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

		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		return true;
	}

}
