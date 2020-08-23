/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.provider;

import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

/**
 * Support for localisations. Defaults to a simple provider
 * 
 * @author nick@alcina.cc
 * 
 */
public class TextProvider {
	public static final String DISPLAY_NAME = "displayName";

	protected static final int TRIMMED_LENGTH = 60;

	private static TextProvider instance;

	public static TextProvider get() {
		if (instance == null) {
			instance = new TextProvider();
		}
		TextProvider tp = instance.getT();
		if (tp != null) {
			return tp;
		}
		return instance;
	}

	public static void registerTextProvider(TextProvider theInstance) {
		TextProvider.instance = theInstance;
	}

	private boolean decorated = false;

	private boolean trimmed = false;

	protected TextProvider() {
		super();
	}

	public void appShutdown() {
		instance = null;
	}

	// For app-level subclassing, if these should be HTML
	public Label getInlineLabel(String text) {
		return new InlineLabel(text);
	}

	public String getLabelText(Class c, ClientPropertyReflector pr) {
		return pr.getDisplayName();
	}

	public Object getLabelText(Class c, String propertyName) {
		ClientBeanReflector beanReflector = ClientReflector.get()
				.beanInfoForClass(c);
		ClientPropertyReflector propertyReflector = beanReflector
				.getPropertyReflectors().get(propertyName);
		return propertyReflector == null ? propertyName
				: getLabelText(c, propertyReflector);
	}

	public String getObjectName(Object o) {
		if (o == null) {
			return "null";
		}
		ClientBeanReflector beanReflector = ClientReflector.get()
				.beanInfoForClass(o.getClass());
		return getObjectName(o, beanReflector);
	}

	public String getObjectName(Object o, ClientBeanReflector beanReflector) {
		if (o instanceof HasDisplayName) {
			return ((HasDisplayName) o).displayName();
		}
		String dnpn = beanReflector.getDisplayNamePropertyName();
		Object pv = GwittirBridge.get().getPropertyValue(o, dnpn);
		return (pv == null) ? "---"
				: CommonUtils.trimToWsChars(pv.toString(),
						trimmed ? TRIMMED_LENGTH : 999, true);
	}

	public String getUiObjectText(Class clazz, String key,
			String defaultValue) {
		return defaultValue;
	}

	public boolean isDecorated() {
		return decorated;
	}

	public boolean isTrimmed() {
		return this.trimmed;
	}

	public void setDecorated(boolean decorated) {
		this.decorated = decorated;
	}

	public void setObjectName(Entity newObj, String name) {
		if (newObj instanceof HasDisplayName) {
			return;
		}
		String dnpn = ClientReflector.get().beanInfoForClass(newObj.getClass())
				.getDisplayNamePropertyName();
		if (dnpn.equals("id")) {
			return;
		}
		Reflections.propertyAccessor().setPropertyValue(newObj, dnpn, name);
	}

	public void setTrimmed(boolean trimmed) {
		this.trimmed = trimmed;
	}

	protected TextProvider getT() {
		return null;
	}
}
