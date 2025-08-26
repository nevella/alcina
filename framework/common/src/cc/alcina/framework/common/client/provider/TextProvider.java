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

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;

/**
 * Support for localisations. Defaults to a simple provider
 *
 * 
 *
 */
public class TextProvider {
	public static final String DISPLAY_NAME = TextProvider.class.getName()
			+ ".DISPLAY_NAME";

	protected static final int TRIMMED_LENGTH = 60;

	private static volatile TextProvider instance;

	public static TextProvider get() {
		if (instance == null) {
			synchronized (instance) {
				if (instance == null) {
					instance = new TextProvider();
				}
			}
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

	public String getLabelText(AnnotationLocation location) {
		Display display = location.getAnnotation(Display.class);
		String rawName = display == null
				? CommonUtils.deInfix(location.property.getName())
				: Display.Support.name(location.property, display);
		return rawName;
	}

	public String getLabelText(Class clazz, Property property) {
		return getLabelText(new AnnotationLocation(clazz, property));
	}

	public Object getLabelText(Class clazz, String propertyName) {
		Property property = Reflections.at(clazz).property(propertyName);
		return property == null ? propertyName : getLabelText(clazz, property);
	}

	public String getObjectName(Object o) {
		if (o == null) {
			return "(null)";
		}
		if (o instanceof HasDisplayName) {
			return ((HasDisplayName) o).displayName();
		}
		if (o instanceof Entity) {
			return ((Entity) o).toStringEntity();
		}
		return o.toString();
	}

	protected TextProvider getT() {
		return null;
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

	public void putDisplayName(Entity entity, String name) {
		((HasDisplayName.Settable) entity).putDisplayName(name);
	}

	public void setDecorated(boolean decorated) {
		this.decorated = decorated;
	}

	public void setTrimmed(boolean trimmed) {
		this.trimmed = trimmed;
	}
}
