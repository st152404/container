package org.opentosca.planbuilder.type.plugin.connectsto.core.handler;

import org.opentosca.planbuilder.plugins.context.PlanContext;

/**
 * Interface to handle Plugins for the IoT-Device Modelling
 *
 * @author Marc Schmid
 *
 * @param <T> the Plancontext
 */
public interface IoTTypePluginHandler<T extends PlanContext> {

    public boolean handle(final T context);

}
