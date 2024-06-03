/*
 * BoundWidgetTypeFactory.java
 *
 * Created on July 27, 2007, 8:11 PM
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.totsp.gwittir.client.ui.util;

import java.util.Date;
import java.util.HashMap;

import com.totsp.gwittir.client.ui.Checkbox;
import com.totsp.gwittir.client.ui.Label;

import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxEnumProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.DateBox.DateBoxProvider;

/**
 * DOCUMENT ME!
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public class BoundWidgetTypeFactory {
	public static final transient String CONTEXT_WITH_ENUM_NULL = BoundWidgetTypeFactory.class
			.getName() + ".CONTEXT_WITH_ENUM_NULL";

	public static final BoundWidgetProvider CHECKBOX_PROVIDER = new BoundWidgetProvider() {
		@Override
		public Checkbox get() {
			return new Checkbox();
		}
	};

	public static final BoundWidgetProvider TEXTBOX_PROVIDER = new BoundWidgetProvider() {
		@Override
		public cc.alcina.framework.gwt.client.gwittir.widget.TextBox get() {
			return new cc.alcina.framework.gwt.client.gwittir.widget.TextBox();
		}
	};

	public static final BoundWidgetProvider LABEL_PROVIDER = new BoundWidgetProvider() {
		@Override
		public Label get() {
			return new Label();
		}
	};

	HashMap<Object, BoundWidgetProvider> registry = new HashMap<Object, BoundWidgetProvider>();

	/** Creates a new instance of BoundWidgetTypeFactory */
	public BoundWidgetTypeFactory() {
		registry.put(Boolean.class, BoundWidgetTypeFactory.CHECKBOX_PROVIDER);
		registry.put(boolean.class, BoundWidgetTypeFactory.CHECKBOX_PROVIDER);
		registry.put(String.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
		registry.put(Integer.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
		registry.put(int.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
		registry.put(Long.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
		registry.put(long.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
		registry.put(Float.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
		registry.put(float.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
		registry.put(Double.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
		registry.put(double.class, BoundWidgetTypeFactory.TEXTBOX_PROVIDER);
		registry.put(Date.class, new DateBoxProvider());
	}

	public void add(Class<?> type, BoundWidgetProvider provider) {
		registry.put(type, provider);
	}

	public BoundWidgetProvider getWidgetProvider(Class<?> type) {
		if (type.isEnum()) {
			return new ListBoxEnumProvider((Class<? extends Enum>) type,
					DEFAULT_ENUM_SELECTOR_WITH_NULL);
		}
		return registry.get(type);
	}

	public static boolean DEFAULT_ENUM_SELECTOR_WITH_NULL = true;
}
