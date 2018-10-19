package org.opentosca.planbuilder.type.plugin.connectsto.bpel;

import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.type.plugin.connectsto.bpel.handler.BPELOTATypePluginHandler;
import org.opentosca.planbuilder.type.plugin.connectsto.core.OTATypePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class with the handle function to check if a Topology has to be handled by the IoT-OTA-Plugin for
 * IoT-Devices and if yes, then call the correct handle plugin
 *
 * @author Marc Schmid
 *
 */
public class BPELOTAPlugin extends OTATypePlugin<BPELPlanContext> {

    private static final Logger LOG = LoggerFactory.getLogger(BPELOTAPlugin.class);
    private final BPELOTATypePluginHandler handler = new BPELOTATypePluginHandler();

    /**
     * Check if the Service Template should be handled by the IoT-OTA-Plugin if yes, then call the
     * Plugin
     */
    @Override
    public boolean handle(final BPELPlanContext templateContext) {
        if (templateContext.getNodeTemplate() == null) {
            // error
            return false;
        } else {
            if (this.canHandle(templateContext.getNodeTemplate())) {
                return handler.handle(templateContext);
            }
        }
        return false;
    }
}
