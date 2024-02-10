/*
 * Binding.java
 *
 * Created on July 16, 2007, 12:49 PM
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
package com.totsp.gwittir.client.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.ValidationFeedback;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.util.GwtDomUtils;

/**
 * This class represents a DataBinding between two objects. It also supports
 * Child bindings. For more information, see
 * <a href="http://code.google.com/p/gwittir/wiki/Binding">Binding</a> in the
 * Wiki.
 *
 * @see com.totsp.gwittir.client.beans.BindingBuilder
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public class Binding {
	// when this converter is used as the transform in O1->[C]->O2, no change in
	// [O1] will change [O2]
	public static final Converter IGNORE_CHANGE = o -> o;

	static Logger logger = LoggerFactory.getLogger(Binding.class);

	BindingInstance left;

	BindingInstance right;

	/**
	 * TRUE = left; FALSE = right;
	 */
	private Boolean lastSet = null;

	private List<Binding> children;

	private boolean bound = false;

	/**
	 * Creates an empty Binding object. This is mostly useful for top-of-tree
	 * parent Bindings.
	 */
	public Binding() {
		super();
	}

	/**
	 * Creates a Binding with two populated binding instances.
	 *
	 * @param left
	 *            The left binding instance.
	 * @param right
	 *            The right binding instance
	 */
	public Binding(BindingInstance left, BindingInstance right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Creates a new binding. This method is a shorthand for working with
	 * BoundWidgets. The bound widget provided will become the left-hand
	 * binding, and the "value" property of the bound widget will be bound to
	 * the property specified by modelProperty of the object on the
	 * BoundWidget's "model" property.
	 *
	 * @param widget
	 *            BoundWidget containing the model.
	 * @param validator
	 *            A validator for the BouldWidget's value property.
	 * @param feedback
	 *            A feedback implementation for validation errors.
	 * @param modelProperty
	 *            The property on the Widgets model object to bind to.
	 */
	public <T> Binding(BoundWidget<T> widget, Validator validator,
			ValidationFeedback feedback, String modelProperty) {
		this(widget, "value", validator, feedback,
				(SourcesPropertyChangeEvents) widget.getModel(),
				"modelProperty", null, null);
	}

	/**
	 *
	 * @param left
	 * @param leftProperty
	 * @param leftConverter
	 * @param right
	 * @param rightProperty
	 * @param rightConverter
	 */
	public Binding(SourcesPropertyChangeEvents left, String leftProperty,
			Converter leftConverter, SourcesPropertyChangeEvents right,
			String rightProperty, Converter rightConverter) {
		this.left = this.createBindingInstance(left, leftProperty);
		this.left.converter = leftConverter;
		this.right = this.createBindingInstance(right, rightProperty);
		this.right.converter = rightConverter;
		this.left.listener = new DefaultPropertyChangeListener(this.left,
				this.right);
		this.right.listener = new DefaultPropertyChangeListener(this.right,
				this.left);
	}

	/**
	 * Creates a new instance of Binding
	 *
	 * @param left
	 *            The left hand object.
	 * @param leftProperty
	 *            Property on the left object.
	 * @param right
	 *            The right hand object
	 * @param rightProperty
	 *            Property on the right object.
	 */
	public Binding(SourcesPropertyChangeEvents left, String leftProperty,
			SourcesPropertyChangeEvents right, String rightProperty) {
		this.left = this.createBindingInstance(left, leftProperty);
		this.right = this.createBindingInstance(right, rightProperty);
		this.left.listener = new DefaultPropertyChangeListener(this.left,
				this.right);
		this.right.listener = new DefaultPropertyChangeListener(this.right,
				this.left);
	}

	public Binding(SourcesPropertyChangeEvents left, String leftProperty,
			Validator leftValidator, ValidationFeedback leftFeedback,
			SourcesPropertyChangeEvents right, String rightProperty) {
		this(left, leftProperty, leftValidator, leftFeedback, right,
				rightProperty, null, null);
	}

	/**
	 * Creates a new Binding instance.
	 *
	 * @param left
	 *            The left hand object.
	 * @param leftProperty
	 *            The property of the left hand object.
	 * @param leftValidator
	 *            A validator for the left hand property.
	 * @param leftFeedback
	 *            Feedback for the left hand validator.
	 * @param right
	 *            The right hand object.
	 * @param rightProperty
	 *            The property on the right hand object
	 * @param rightValidator
	 *            Validator for the right hand property.
	 * @param rightFeedback
	 *            Feedback for the right hand validator.
	 */
	public Binding(SourcesPropertyChangeEvents left, String leftProperty,
			Validator leftValidator, ValidationFeedback leftFeedback,
			SourcesPropertyChangeEvents right, String rightProperty,
			Validator rightValidator, ValidationFeedback rightFeedback) {
		this.left = this.createBindingInstance(left, leftProperty);
		this.left.validator = leftValidator;
		this.left.feedback = leftFeedback;
		this.right = this.createBindingInstance(right, rightProperty);
		this.right.validator = rightValidator;
		this.right.feedback = rightFeedback;
		this.left.listener = new DefaultPropertyChangeListener(this.left,
				this.right);
		this.right.listener = new DefaultPropertyChangeListener(this.right,
				this.left);
	}

	/**
	 * Establishes a two-way binding between the objects.
	 */
	public void bind() {
		if (this.bound) {
			return;
		}
		if ((left != null) && (right != null)) {
			left.object.addPropertyChangeListener(left.property.getName(),
					left.listener);
			if (left.nestedListener != null) {
				left.nestedListener.setup();
			}
			right.object.addPropertyChangeListener(right.property.getName(),
					right.listener);
			if (right.nestedListener != null) {
				right.nestedListener.setup();
			}
		}
		for (int i = 0; (children != null) && (i < children.size()); i++) {
			Binding child = children.get(i);
			child.bind();
		}
		this.bound = true;
	}

	BindingInstance createBindingInstance(SourcesPropertyChangeEvents object,
			String propertyName) {
		int dotIndex = propertyName.indexOf(".");
		BindingInstance instance = new BindingInstance();
		NestedPropertyChangeListener rtpcl = (dotIndex == -1) ? null
				: new NestedPropertyChangeListener(instance, object,
						propertyName);
		ArrayList parents = new ArrayList();
		ArrayList propertyNames = new ArrayList();
		while (dotIndex != -1) {
			String pname = propertyName.substring(0, dotIndex);
			propertyName = propertyName.substring(dotIndex + 1);
			parents.add(object);
			try {
				propertyNames.add(pname);
				object = (SourcesPropertyChangeEvents) Reflections.at(object)
						.property(pname).get(object);
			} catch (ClassCastException cce) {
				throw new RuntimeException(
						"Nonbindable sub property: " + object + " . " + pname,
						cce);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			dotIndex = propertyName.indexOf(".");
		}
		if (rtpcl != null) {
			rtpcl.parents = new SourcesPropertyChangeEvents[parents.size()];
			parents.toArray(rtpcl.parents);
			rtpcl.propertyNames = new String[propertyNames.size()];
			propertyNames.toArray(rtpcl.propertyNames);
		}
		instance.object = object;
		try {
			instance.property = Reflections.at(object).property(propertyName);
			if (instance.property == null) {
				throw new NullPointerException("Property Not Found.");
			}
		} catch (NullPointerException e) {
			throw new RuntimeException(
					"Exception getting property " + propertyName, e);
		}
		instance.nestedListener = rtpcl;
		return instance;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != this.getClass()) {
			return false;
		}
		final Binding other = (Binding) obj;
		if ((this.left != other.left)
				&& ((this.left == null) || !this.left.equals(other.left))) {
			return false;
		}
		if ((this.right != other.right)
				&& ((this.right == null) || !this.right.equals(other.right))) {
			return false;
		}
		return true;
	}

	/**
	 * Returns a list of child Bindings.
	 *
	 * @return List of child bindings.
	 */
	public List<Binding> getChildren() {
		return children = (children == null) ? new ArrayList<Binding>()
				: children;
	}

	/**
	 * Returns the left hand BindingInstance.
	 *
	 * @return Returns the left hand BindingInstance.
	 */
	public BindingInstance getLeft() {
		return this.left;
	}

	/**
	 * Returns the right hand BindingInstance.
	 *
	 * @return Returns the left hand BindingInstance.
	 */
	public BindingInstance getRight() {
		return this.right;
	}

	/**
	 *
	 * @return int based on hash of the two objects being bound.
	 */
	@Override
	public int hashCode() {
		if (this.right == null || this.left == null) {
			return System.identityHashCode(this);
		}
		return this.right.object.hashCode() ^ this.left.object.hashCode();
	}

	public boolean isBound() {
		return bound;
	}

	/**
	 * Performs a quick validation on the Binding to determine if it is valid.
	 *
	 * @return boolean indicating all values are valid.
	 */
	public boolean isValid() {
		if (!bound) {
			return true;
		}
		try {
			if ((left != null) && (right != null)) {
				if (leftObjectIsHiddenWidget()) {
					return true;
				}
				if (left.validator != null) {
					left.validator.validate(left.property.get(left.object));
				}
				if (right.validator != null) {
					right.validator.validate(right.property.get(right.object));
				}
			}
			boolean valid = true;
			for (int i = 0; (children != null) && (i < children.size()); i++) {
				Binding child = children.get(i);
				valid = valid & child.isValid();
			}
			return valid;
		} catch (ValidationException ve) {
			return false;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected boolean leftObjectIsHiddenWidget() {
		if (left.object instanceof Widget) {
			if (!GwtDomUtils.isVisibleAncestorChain(
					((Widget) left.object).getElement())) {
				return true;
			}
		}
		return false;
	}

	public List<Binding> provideAllBindings(List<Binding> list) {
		list = list == null ? new ArrayList<Binding>() : list;
		list.add(this);
		if (children != null) {
			for (Binding b : children) {
				b.provideAllBindings(list);
			}
		}
		return list;
	}

	public Binding provideBindingByLeftObject(Object left) {
		List<Binding> bindings = provideAllBindings(null);
		for (Binding binding : bindings) {
			if (binding.getLeft() != null && binding.getLeft().object == left) {
				return binding;
			}
		}
		return null;
	}

	public void resolveAllFeedbacks() {
		List<Binding> bindings = provideAllBindings(null);
		for (Binding binding : bindings) {
			if (binding.getLeft() != null
					&& binding.getLeft().feedback != null) {
				binding.getLeft().feedback.resolve(binding.getLeft().object);
			}
			if (binding.getRight() != null
					&& binding.getRight().feedback != null) {
				binding.getRight().feedback.resolve(binding.getRight().object);
			}
		}
	}

	/**
	 * Sets the left hand property to the current value of the right.
	 */
	public void setLeft() {
		if ((left != null) && (right != null)) {
			try {
				right.listener.propertyChange(new PropertyChangeEvent(
						right.object, right.property.getName(), null,
						right.property.get(right.object)));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		for (int i = 0; (children != null) && (i < children.size()); i++) {
			Binding child = children.get(i);
			child.setLeft();
		}
		this.lastSet = Boolean.TRUE;
	}

	/**
	 * Sets the right objects property to the current value of the left.
	 */
	public void setRight() {
		if ((left != null) && (right != null)) {
			try {
				left.listener.propertyChange(new PropertyChangeEvent(
						left.object, left.property.getName(), null,
						left.property.get(left.object)));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		for (int i = 0; (children != null) && (i < children.size()); i++) {
			Binding child = children.get(i);
			child.setRight();
		}
		this.lastSet = Boolean.FALSE;
	}

	@Override
	public String toString() {
		FormatBuilder fb = new FormatBuilder().separator("\n");
		if (left != null) {
			fb.append("[Binding property ").append(left.property).append(" on ")
					.append(left.object).append(" to ").append(right.property)
					.append(" on ").append(right.object)
					.append(" ] with children :");
		}
		for (Binding b : this.getChildren()) {
			fb.append(b.toString());
		}
		return fb.toString();
	}

	/**
	 * Breaks the two way binding and removes all listeners.
	 */
	public void unbind() {
		if (!this.bound) {
			return;
		}
		if ((left != null) && (right != null)) {
			left.object.removePropertyChangeListener(left.property.getName(),
					left.listener);
			if (left.feedback != null) {
				try {
					left.feedback.resolve(left.object);
				} catch (Exception e) {
					logger.warn("Exception cleaning up feedback ", e);
				}
			}
			if (left.nestedListener != null) {
				left.nestedListener.cleanup();
			}
			right.object.removePropertyChangeListener(right.property.getName(),
					right.listener);
			if (right.feedback != null) {
				try {
					right.feedback.resolve(right.object);
				} catch (Exception e) {
					logger.warn("Exception cleaning up feedback ", e);
				}
			}
			if (right.nestedListener != null) {
				right.nestedListener.cleanup();
			}
		}
		for (int i = 0; (children != null) && (i < children.size()); i++) {
			Binding child = children.get(i);
			child.unbind();
		}
		this.bound = false;
	}

	public boolean validate() {
		boolean valid = true;
		if (!bound) {
			return true;
		}
		if ((left != null) && (right != null)) {
			if (left.validator != null) {
				if (leftObjectIsHiddenWidget()) {
					return true;
				}
				try {
					left.validator.validate(left.property.get(left.object));
				} catch (ValidationException ve) {
					valid = false;
					if (left.feedback != null) {
						left.feedback.handleException(left.object, ve);
					}
					if (left.listener instanceof DefaultPropertyChangeListener) {
						((DefaultPropertyChangeListener) left.listener).lastException = ve;
					}
				} catch (Exception e) {
					valid = false;
					logger.warn("Non-validation exception in validator ", e);
				}
			}
			if (right.validator != null) {
				try {
					right.validator.validate(right.property.get(right.object));
				} catch (ValidationException ve) {
					valid = false;
					if (right.feedback != null) {
						right.feedback.handleException(right.object, ve);
					}
					if (right.listener instanceof DefaultPropertyChangeListener) {
						((DefaultPropertyChangeListener) right.listener).lastException = ve;
					}
				} catch (Exception e) {
					valid = false;
					logger.warn("Non-validation exception in validator ", e);
				}
			}
		}
		for (int i = 0; (children != null) && (i < children.size()); i++) {
			Binding child = children.get(i);
			valid = valid & child.validate();
		}
		return valid;
	}

	/**
	 * A data class containing the relevant data for one half of a binding
	 * relationship.
	 */
	public static class BindingInstance {
		/**
		 * The Object being bound.
		 */
		public SourcesPropertyChangeEvents object;

		/**
		 * A converter when needed.
		 */
		public Converter converter;

		/**
		 * The property name being bound.
		 */
		public Property property;

		/**
		 * A ValidationFeedback object when needed.
		 */
		public ValidationFeedback feedback;

		/**
		 * A Validator object when needed.
		 */
		public Validator validator;

		PropertyChangeListener listener;

		private NestedPropertyChangeListener nestedListener = null;

		private BindingInstance() {
			super();
		}
	}

	static class DefaultPropertyChangeListener
			implements PropertyChangeListener {
		private BindingInstance instance;

		private BindingInstance target;

		private ValidationException lastException = null;

		DefaultPropertyChangeListener(BindingInstance instance,
				BindingInstance target) {
			this.instance = instance;
			this.target = target;
		}

		@Override
		public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
			if (instance.converter == IGNORE_CHANGE) {
				return;
			}
			Object value = propertyChangeEvent.getNewValue();
			if (instance.validator != null) {
				try {
					value = instance.validator.validate(value);
				} catch (ValidationException ve) {
					if (instance.feedback != null) {
						if (this.lastException != null) {
							instance.feedback
									.resolve(propertyChangeEvent.getSource());
						}
						instance.feedback.handleException(
								propertyChangeEvent.getSource(), ve);
						this.lastException = ve;
						return;
					} else {
						this.lastException = ve;
						throw new RuntimeException(ve);
					}
				}
			}
			if (this.instance.feedback != null) {
				this.instance.feedback.resolve(propertyChangeEvent.getSource());
			}
			this.lastException = null;
			// Adding this to simplify simple toString conversions.
			// TODO add a nice way to register global converter defaults
			if ((instance.converter == null)
					&& (target.property.getType() == String.class)
					&& (instance.property.getType() != String.class)) {
				instance.converter = Converter.TO_STRING_CONVERTER;
			}
			if (instance.converter != null && instance.validator == null) {
				value = instance.converter.convert(value);
			}
			try {
				target.property.set(target.object, value);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Exception setting property: "
						+ target.property.getName(), e);
				// LOGGER.log(Level.ERROR, "Exception setting property: "
				// + target.property.getName() + " on object "
				// + target.object, e);
			}
		}

		@Override
		public String toString() {
			return "[Listener on : " + this.instance.object + " ] ";
		}
	}

	private class NestedPropertyChangeListener
			implements PropertyChangeListener {
		SourcesPropertyChangeEvents sourceObject;

		BindingInstance target;

		String propertyName;

		SourcesPropertyChangeEvents[] parents;

		String[] propertyNames;

		NestedPropertyChangeListener(BindingInstance target,
				SourcesPropertyChangeEvents sourceObject, String propertyName) {
			this.target = target;
			this.sourceObject = sourceObject;
			this.propertyName = propertyName;
		}

		public void cleanup() {
			for (int i = 0; i < parents.length; i++) {
				parents[i].removePropertyChangeListener(this.propertyNames[i],
						this);
			}
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (bound) {
				unbind();
				bound = true;
			}
			BindingInstance newInstance = createBindingInstance(sourceObject,
					propertyName);
			target.object = newInstance.object;
			target.nestedListener = newInstance.nestedListener;
			target.nestedListener.target = target;
			target.property = newInstance.property;
			if (lastSet == Boolean.TRUE) {
				setLeft();
			} else if (lastSet == Boolean.FALSE) {
				setRight();
			}
			if (bound) {
				bind();
			}
		}

		public void setup() {
			for (int i = 0; i < parents.length; i++) {
				parents[i].addPropertyChangeListener(this.propertyNames[i],
						this);
			}
		}
	}

	public interface PropertyMap {
	}
}
