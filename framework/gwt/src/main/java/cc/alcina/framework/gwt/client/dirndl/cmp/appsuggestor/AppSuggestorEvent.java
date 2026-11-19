package cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor;

import cc.alcina.framework.common.client.logic.reflection.Registration;

/**
 * Marker interface applied to ModelEvent subclasses for events triggerable from
 * the app suggestor
 */
@Registration(AppSuggestorEvent.class)
public interface AppSuggestorEvent extends Registration.AllSubtypesClient {
}
