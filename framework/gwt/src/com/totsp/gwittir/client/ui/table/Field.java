/*
 * Column.java
 *
 * Created on July 24, 2007, 5:32 PM
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
package com.totsp.gwittir.client.ui.table;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.List;

import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.validator.ValidationFeedback;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public class Field {
	private BoundWidgetProvider cellProvider;

	private Comparator comparator;

	private Converter converter;

	private String helpText;

	private String label;

	private String styleName;

	private String widgetStyleName;

	private ValidationFeedback feedback;

	private Validator validator;

	private String autocompleteName;

	Class<?> declaringType;

	private Property property;

	private AnnotationLocation.Resolver resolver;

	private boolean editable;

	/*
	 * A key optimisation - share column annotation resolutions for tables
	 */
	public class SharedCacheResolver extends ContextResolver {
		@Override
		public MultikeyMap<List<? extends Annotation>> resolvedCache() {
			return super.resolvedCache();
		}

		public BindingsCache bindingsCache() {
			return bindingsCache;
		}
	}

	private SharedCacheResolver sharedCacheResolver = new SharedCacheResolver();

	public SharedCacheResolver getSharedAnnotationResolver() {
		return sharedCacheResolver;
	}

	public Field(Property property, String label,
			BoundWidgetProvider cellProvider, Validator validator,
			ValidationFeedback feedback, Converter converter,
			Class<?> declaringType, AnnotationLocation.Resolver resolver,
			boolean editable) {
		this.property = property;
		if (property.getOwningType().getName().endsWith("_")) {
			int debug = 3;
		}
		this.label = label;
		this.cellProvider = cellProvider;
		this.validator = validator;
		this.feedback = feedback;
		this.converter = converter;
		this.declaringType = declaringType;
		this.resolver = resolver;
		this.editable = editable;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public String getAutocompleteName() {
		return this.autocompleteName;
	}

	public BoundWidgetProvider getCellProvider() {
		return cellProvider;
	}

	public Comparator getComparator() {
		return comparator;
	}

	public Converter getConverter() {
		return converter;
	}

	public ValidationFeedback getFeedback() {
		return feedback;
	}

	public String getHelpText() {
		return helpText;
	}

	public String getLabel() {
		return label;
	}

	public Property getProperty() {
		return property;
	}

	public String getPropertyName() {
		return property.getName();
	}

	public AnnotationLocation.Resolver getResolver() {
		return this.resolver;
	}

	public String getStyleName() {
		return styleName;
	}

	public Validator getValidator() {
		return validator;
	}

	public String getWidgetStyleName() {
		return this.widgetStyleName;
	}

	public void setAutocompleteName(String autocompleteName) {
		this.autocompleteName = autocompleteName;
	}

	public void setCellProvider(BoundWidgetProvider cellProvider) {
		this.cellProvider = cellProvider;
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	public void setConverter(Converter converter) {
		this.converter = converter;
	}

	public void setFeedback(ValidationFeedback feedback) {
		this.feedback = feedback;
	}

	public void setHelpText(String helpText) {
		this.helpText = helpText;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setStyleName(String styleName) {
		this.styleName = styleName;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	public void setWidgetStyleName(String widgetStyleName) {
		this.widgetStyleName = widgetStyleName;
	}

	@Override
	public String toString() {
		return Ax.format("Field: %s", getPropertyName());
	}

	public Validator provideReverseValidator() {
		if (validator instanceof Validator.Bidi) {
			return ((Validator.Bidi) validator).inverseValidator();
		} else {
			return null;
		}
	}
}
