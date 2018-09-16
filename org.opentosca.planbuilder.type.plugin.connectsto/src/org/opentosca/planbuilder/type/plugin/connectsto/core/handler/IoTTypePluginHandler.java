package org.opentosca.planbuilder.type.plugin.connectsto.core.handler;

import org.opentosca.planbuilder.plugins.context.PlanContext;

public interface IoTTypePluginHandler<T extends PlanContext> {

    public boolean handle(final T context);

}
