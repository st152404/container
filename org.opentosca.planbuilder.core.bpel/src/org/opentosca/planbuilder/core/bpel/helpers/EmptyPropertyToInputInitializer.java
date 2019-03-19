package org.opentosca.planbuilder.core.bpel.helpers;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.opentosca.container.core.next.model.PlanLanguage;
import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer.PropertyMap;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELScopeActivity;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.opentosca.planbuilder.plugins.context.Variable;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Copyright 2016 IAAS University of Stuttgart <br>
 * <br>
 *
 * @author Kálmán Képes - kalman.kepes@iaas.uni-stuttgart.de
 *
 */
public class EmptyPropertyToInputInitializer {

    /**
     * Adds an element to the plan input with the given name and assign at runtime the value to the
     * given variable
     *
     * @param buildPlan the plan to add the logic to
     * @param propLocalName the name of the element added to the input
     * @param var the variable to assign the value to
     * @param context a context for the manipulation
     */
    private void addToPlanInput(final BPELPlan buildPlan, final String propLocalName, final Variable var,
                                final BPELPlanContext context) {
        // add to input
        context.addStringValueToPlanRequest(propLocalName);

        // add copy from input local element to property variable
        final String bpelCopy = generateCopyFromInputToVariableAsString(createLocalNameXpathQuery(propLocalName),
                                                                        createBPELVariableXpathQuery(var.getName()));
        try {
            final Node bpelCopyNode = ModelUtils.string2dom(bpelCopy);
            appendToInitSequence(bpelCopyNode, buildPlan);
        }
        catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Appends the given node to the main sequence of the buildPlan this context belongs to
     *
     * @param node the XML DOM Node to append
     */
    private void appendToInitSequence(final Node node, final BPELPlan buildPlan) {

        final Node mainSequenceNode = buildPlan.getBpelMainFlowElement().getParentNode();

        final Node importedNode = mainSequenceNode.getOwnerDocument().importNode(node, true);
        mainSequenceNode.insertBefore(importedNode, buildPlan.getBpelMainFlowElement());
    }

    private String createBPELVariableXpathQuery(final String variableName) {
        return "$" + variableName;
    }

    private String createLocalNameXpathQuery(final String localName) {
        return "//*[local-name()='" + localName + "']";
    }

    /**
     * Generates a bpel copy element that queries from the plan input message to some xpath query
     *
     * @param inputQuery the query to a local element inside the input message
     * @param variableQuery the query to set the value for
     * @return a String containing a bpel copy
     */
    private String generateCopyFromInputToVariableAsString(final String inputQuery, final String variableQuery) {
        String copyString = "<bpel:assign xmlns:bpel=\"" + PlanLanguage.BPEL.toString() + "\"><bpel:copy>";

        copyString +=
            "<bpel:from variable=\"input\" part=\"payload\"><bpel:query queryLanguage=\"urn:oasis:names:tc:wsbpel:2.0:sublang:xpath1.0\"><![CDATA["
                + inputQuery + "]]></bpel:query></bpel:from>";

        copyString += "<bpel:to expressionLanguage=\"urn:oasis:names:tc:wsbpel:2.0:sublang:xpath1.0\"><![CDATA[";
        copyString += variableQuery + "]]></bpel:to>";

        copyString += "</bpel:copy></bpel:assign>";

        return copyString;
    }

    /**
     * Initializes all properties of NodeTemplates which contain 'get_input:' as plan input
     * parameters.
     *
     * @param buildPlan the buildPlan for which the properties have to be initialized
     * @param propMap a Map containing all mappings from NodeTemplate properties to the representing
     *        variable
     */
    public void initializeEmptyPropertiesAsInputParam(final BPELPlan buildPlan, final PropertyMap propMap) {
        this.initializeEmptyPropertiesAsInputParam(buildPlan.getTemplateBuildPlans(), buildPlan, propMap);
    }

    public void initializeEmptyPropertiesAsInputParam(final List<BPELScopeActivity> bpelActivities, final BPELPlan plan,
                                                      final PropertyMap propMap) {
        for (final BPELScopeActivity templatePlan : bpelActivities) {
            if (Objects.nonNull(templatePlan.getNodeTemplate())) {
                final AbstractNodeTemplate nodeTemplate = templatePlan.getNodeTemplate();

                if (Objects.isNull(propMap.getPropertyMappingMap(nodeTemplate.getId()))) {
                    // nodeTemplate doesn't have props defined
                    continue;
                }

                final BPELPlanContext context = new BPELPlanContext(templatePlan, propMap, plan.getServiceTemplate());

                for (final String propLocalName : propMap.getPropertyMappingMap(nodeTemplate.getId()).keySet()) {
                    final Variable var = context.getPropertyVariable(nodeTemplate, propLocalName);

                    if (!BPELPlanContext.isVariableValueEmpty(var, context)) {
                        String content = BPELPlanContext.getVariableContent(var, context);
                        if (content.startsWith("get_input") && content.contains("get_input:")) {
                            content = content.replace("get_input:", "").trim();
                            addToPlanInput(plan, content, var, context);
                        }
                    }
                }
            }
        }
    }
}
