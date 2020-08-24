/*
 * BoundWidgetTypeFactory.java
 *
 * Created on July 27, 2007, 8:11 PM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.totsp.gwittir.client.ui.util;

import java.util.HashMap;

import com.totsp.gwittir.client.ui.Checkbox;
import com.totsp.gwittir.client.ui.Label;
import com.totsp.gwittir.client.ui.TextBox;

import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;

/**
 * DOCUMENT ME!
 * 
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public class BoundWidgetTypeFactory {
	public static final BoundWidgetProvider<Checkbox> CHECKBOX_PROVIDER = new BoundWidgetProvider<Checkbox>() {
		public Checkbox get() {
			return new Checkbox();
		}
	};

	public static final BoundWidgetProvider<TextBox> TEXTBOX_PROVIDER = new BoundWidgetProvider<TextBox>() {
		public TextBox get() {
			return new TextBox();
		}
	};

	public static final BoundWidgetProvider<Label> LABEL_PROVIDER = new BoundWidgetProvider<Label>() {
		public Label get() {
			return new Label();
		}
	};

	HashMap<Object, BoundWidgetProvider<?>> registry = new HashMap<Object, BoundWidgetProvider<?>>();

	/** Creates a new instance of BoundWidgetTypeFactory */
	public BoundWidgetTypeFactory() {
		this(true);
	}

	public BoundWidgetTypeFactory(boolean defaults) {
		super();
		if (defaults) {
			registry.put(Boolean.class,
					BoundWidgetTypeFactory.CHECKBOX_PROVIDER);
			registry.put(boolean.class,
					BoundWidgetTypeFactory.CHECKBOX_PROVIDER);
			registry.put(String.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
			registry.put(Integer.class,
					BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
			registry.put(int.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
			registry.put(Long.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
			registry.put(long.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
			registry.put(Float.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
			registry.put(float.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
			registry.put(Double.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
			registry.put(double.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
		}
	}

	public void add(Class<?> type, BoundWidgetProvider<?> provider) {
		registry.put(type, provider);
	}

	public void add(String propertyName, BoundWidgetProvider<?> provider) {
		registry.put(propertyName, provider);
	}

	public BoundWidgetProvider<?> getWidgetProvider(Class<?> type) {
		return registry.get(type);
	}

	public BoundWidgetProvider<?> getWidgetProvider(String propertyName,
			Class<?> type) {
		BoundWidgetProvider<?> val = registry.containsKey(propertyName)
				? registry.get(propertyName)
				: registry.get(type);
		return val;
	}

	public BoundWidgetProvider getWidgetProvider(Class propertyType, Class modelType, Bean beanInfo,
			ClientPropertyReflector propertyReflector) {
		return getWidgetProvider(propertyType);
	}
}
