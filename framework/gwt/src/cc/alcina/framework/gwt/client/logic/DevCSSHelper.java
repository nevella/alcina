package cc.alcina.framework.gwt.client.logic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.framework.gwt.client.util.ClientUtils;

import com.google.gwt.dom.client.Element;

public class DevCSSHelper {
	private PropertyChangeListener cssPropertyListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			updateDeveloperCss();
		}
	};
	public void updateDeveloperCss() {
		String css = ClientLayerLocator.get().getGeneralProperties()
				.getPersistentCss()
				+ ClientLayerLocator.get().getGeneralProperties().getTransientCss();
		this.styleElement = ClientUtils.updateCss(styleElement, css);
	}
	private Element styleElement;

	public void addCssListeners(GeneralProperties props) {
		props.addPropertyChangeListener(
				GeneralProperties.PROPERTY_PERSISTENT_CSS, cssPropertyListener);
		props.addPropertyChangeListener(
				GeneralProperties.PROPERTY_TRANSIENT_CSS, cssPropertyListener);
	}
	public void removeCssListeners(GeneralProperties props) {
		props.removePropertyChangeListener(
				GeneralProperties.PROPERTY_PERSISTENT_CSS, cssPropertyListener);
		props.removePropertyChangeListener(
				GeneralProperties.PROPERTY_TRANSIENT_CSS, cssPropertyListener);
	}
	private DevCSSHelper() {
		super();
	}

	private static DevCSSHelper theInstance;

	public static DevCSSHelper get() {
		if (theInstance == null) {
			theInstance = new DevCSSHelper();
		}
		return theInstance;
	}

	
}
