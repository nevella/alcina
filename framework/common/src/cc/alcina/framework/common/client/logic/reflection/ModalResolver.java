package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.ModalDisplay.Modal;
import cc.alcina.framework.common.client.logic.reflection.ModalDisplay.Mode;
import cc.alcina.framework.common.client.logic.reflection.ModalDisplay.ModeTransformer;
import cc.alcina.framework.common.client.logic.reflection.ModalDisplay.RequireSpecified;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;

/**
 * Applies only to the children of the node on which this is set (Form, Table)
 *
 * It returns different declarative info based on the container state
 * (read/write, single/multiple)
 *
 */
public class ModalResolver extends ContextResolver implements FormModel.Has {
	public static ModalResolver multiple(Node node, boolean readOnly) {
		return new ModalResolver(node,
				readOnly ? Mode.MULTIPLE_READ : Mode.MULTIPLE_WRITE);
	}

	public static ModalResolver single(Node node, boolean readOnly) {
		return new ModalResolver(node,
				readOnly ? Mode.SINGLE_READ : Mode.SINGLE_WRITE);
	}

	private FormModel formModel;

	private TableModel tableModel;

	private final Mode mode;

	private Node node;

	private ModalResolver(Node node, Mode mode) {
		super();
		init(node);
		this.node = node;
		this.mode = Registry.impl(ModeTransformer.class).apply(mode);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ModalResolver
				&& ((ModalResolver) obj).mode == mode;
	}

	@Override
	public FormModel getFormModel() {
		return this.formModel;
	}

	public TableModel getTableModel() {
		return this.tableModel;
	}

	@Override
	public int hashCode() {
		return mode.hashCode();
	}

	@Override
	public synchronized <A extends Annotation> List<A> resolveAnnotations(
			Class<A> annotationClass, AnnotationLocation location) {
		if (location == node.getAnnotationLocation()) {
			return parent.resolveAnnotations(annotationClass, location);
		}
		boolean customResolution = annotationClass == Display.class
				|| annotationClass == Custom.class
				|| annotationClass == Validator.class
				|| annotationClass == Directed.class
				|| annotationClass == Directed.Transform.class;
		List<A> defaultResolution = super.resolveAnnotations(annotationClass,
				location);
		if (customResolution) {
			RequireSpecified requireSpecified = Reflections
					.at(location.classLocation)
					.annotation(RequireSpecified.class);
			ModalDisplay modalDisplay = super.resolveAnnotation(
					ModalDisplay.class, location);
			A modalResolution = null;
			Optional<Modal> matchingModal = Optional.empty();
			if (modalDisplay != null) {
				matchingModal = Arrays.stream(modalDisplay.value())
						.filter(m -> m.mode().matches(this.mode)).findFirst();
				if (matchingModal.isPresent()) {
					Modal modal = matchingModal.get();
					if (annotationClass == Display.class) {
						modalResolution = (A) getFirst(modal.display());
					} else if (annotationClass == Custom.class) {
						modalResolution = (A) getFirst(modal.custom());
					} else if (annotationClass == Validator.class) {
						modalResolution = (A) getFirst(modal.validator());
					}
					Preconditions.checkState(modal.custom().length <= 1);
				}
			}
			if (modalResolution != null) {
				return List.of(modalResolution);
			} else {
				if (requireSpecified == null
						|| Arrays.stream(requireSpecified.value())
								.noneMatch(m -> m.matches(mode))
						|| matchingModal.isPresent()) {
					return defaultResolution;
				} else {
					return List.of();
				}
			}
		} else {
			return defaultResolution;
		}
	}

	public void setFormModel(FormModel formModel) {
		this.formModel = formModel;
	}

	public void setTableModel(TableModel tableModel) {
		this.tableModel = tableModel;
	}

	private <T> T getFirst(T[] array) {
		Preconditions.checkArgument(array.length <= 1);
		return array.length == 0 ? null : array[0];
	}
}