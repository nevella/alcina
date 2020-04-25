package cc.alcina.framework.gwt.client.logic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.entity.GeneralProperties;
import cc.alcina.framework.gwt.client.util.ClientUtils;

public class DevCSSHelper {
	public static DevCSSHelper get() {
		DevCSSHelper singleton = Registry.checkSingleton(DevCSSHelper.class);
		if (singleton == null) {
			singleton = new DevCSSHelper();
			Registry.registerSingleton(DevCSSHelper.class, singleton);
		}
		return singleton;
	}

	private PropertyChangeListener cssPropertyListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			updateDeveloperCss();
		}
	};

	private Element styleElement;

	private DevCSSHelper() {
		super();
	}

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

	public void updateDeveloperCss() {
		String css = ClientBase.getGeneralProperties().getPersistentCss()
				+ ClientBase.getGeneralProperties().getTransientCss();
		this.styleElement = ClientUtils.updateCss(styleElement, css);
	}
}
