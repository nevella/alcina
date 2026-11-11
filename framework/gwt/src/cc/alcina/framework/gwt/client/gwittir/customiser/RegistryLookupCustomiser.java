package cc.alcina.framework.gwt.client.gwittir.customiser;

import cc.alcina.framework.gwt.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * Lets you specify customiser in...for instance...the client package.
 *
 * 
 *
 */
@Reflected
public class RegistryLookupCustomiser implements Customiser {
	public static final String MARKER_CLASS = "marker-class";

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom params) {
		Class markerClass = NamedParameter.Support
				.classValue(params.parameters(), MARKER_CLASS, null);
		return Registry.query(Customiser.class)
				.setKeys(RegistryLookupCustomiser.class, markerClass).impl()
				.getProvider(editable, objectClass, multiple, params);
	}
}
