package cc.alcina.framework.gwt.client.gwittir.customiser;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * Lets you specify customiser in...for instance...the client package.
 * 
 * @author nick@alcina.cc
 * 
 */
@ClientInstantiable
public class RegistryLookupCustomiser implements Customiser {
	public static final String MARKER_CLASS = "marker-class";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom params) {
		Class markerClass = NamedParameter.Support.classValue(params.parameters(), MARKER_CLASS,
				null);
		return ((Customiser) Registry.get().instantiateSingle(
				RegistryLookupCustomiser.class, markerClass)).getProvider(editable, objectClass, multiple, params);
	}
}
