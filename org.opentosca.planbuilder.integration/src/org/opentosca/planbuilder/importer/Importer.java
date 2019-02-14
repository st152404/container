package org.opentosca.planbuilder.importer;

import java.util.ArrayList;
import java.util.List;

import org.opentosca.container.core.common.SystemException;
import org.opentosca.container.core.common.UserException;
import org.opentosca.container.core.model.csar.CSARContent;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.planbuilder.csarhandler.CSARHandler;
import org.opentosca.planbuilder.importer.context.impl.DefinitionsImpl;
import org.opentosca.planbuilder.integration.layer.AbstractImporter;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class is a PlanBuilder Importer for OpenTOSCA. Importing of CSARs is handled by passing a
 * CSARID
 * </p>
 * Copyright 2019 IAAS University of Stuttgart <br>
 * <br>
 *
 * @author Kalman Kepes - kepeskn@studi.informatik.uni-stuttgart.de
 */
public class Importer extends AbstractImporter {

    final private static Logger LOG = LoggerFactory.getLogger(Importer.class);

    private final CSARHandler handler = new CSARHandler();

    /**
     * Generates a List of BuildPlans for the given CSARID. The build plans are generated for the
     * ServiceTemplates inside the Entry-Definitions Document, that haven't got a build plan yet.
     *
     * @param csarId the CSARID for the CSAR the build plans should be generated
     * @return a List of generated build plans
     */
    public List<AbstractPlan> importDefs(final CSARID csarId) {
        try {
            final CSARContent content = this.handler.getCSARContentForID(csarId);
            final AbstractDefinitions defs = createContext(content);
            return buildPlans(defs, csarId.getFileName());
        }
        catch (final UserException e) {
            LOG.error("Some error within input", e);
        }
        catch (final SystemException e) {
            LOG.error("Some internal error", e);
        }
        return new ArrayList<>();
    }

    /**
     * Returns a TOSCA Definitions object which contains the Entry-ServiceTemplate
     *
     * @param csarId an ID of a CSAR
     * @return an AbstractDefinitions object
     */
    public AbstractDefinitions getMainDefinitions(final CSARID csarId) {
        try {
            return createContext(this.handler.getCSARContentForID(csarId));
        }
        catch (final UserException e) {
            LOG.error("Some error within input", e);
        }
        catch (final SystemException e) {
            LOG.error("Some internal error", e);
        }
        return null;
    }

    /**
     * Creates an AbstractDefinitions Object of the given CSARContent
     *
     * @param csarContent the CSARContent to generate an AbstractDefinitions for
     * @return an AbstractDefinitions which is the Entry-Definitions of the given CSAR
     * @throws SystemException is thrown if accessing data inside the OpenTOSCA Core fails
     */
    public AbstractDefinitions createContext(final CSARContent csarContent) throws SystemException {
        return new DefinitionsImpl(csarContent.getRootTOSCA(), csarContent.getFilesRecursively(), true);
    }
}
