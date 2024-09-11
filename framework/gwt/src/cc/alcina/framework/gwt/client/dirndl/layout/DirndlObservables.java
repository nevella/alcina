package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;

import com.google.gwt.event.shared.GwtEvent;

import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;

/**
 * A container class for ({@link ProcessObservable} subtype) observables emitted
 * by the layout process
 */
public class DirndlObservables {
	public static class DomBindingBind extends Observable {
		DomBindingBind(NodeEventBinding binding) {
			super(binding);
		}
	}

	public static class DomBindingFire extends Observable {
		DomBindingFire(NodeEventBinding binding, GwtEvent gwtEvent) {
			super(binding);
			this.gwtEvent = gwtEvent;
		}
	}

	public static class DomBindingUnbind extends Observable {
		DomBindingUnbind(NodeEventBinding binding) {
			super(binding);
		}
	}

	public static class RenderElement extends Observable {
		RenderElement(DirectedLayout.Node layoutNode) {
			super(layoutNode);
		}
	}

	public static class ResolveAnnotations0 implements ProcessObservable {
		public Class<? extends Annotation> annotationClass;

		public AnnotationLocation location;

		ResolveAnnotations0(Class<? extends Annotation> annotationClass,
				AnnotationLocation location) {
			this.annotationClass = annotationClass;
			this.location = location;
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("annotationClass",
					NestedName.get(annotationClass), "location", location);
		}
	}

	public static class Observable implements ProcessObservable {
		final DirectedLayout.Node node;

		DirectedLayout.RendererInput input;

		NodeEventBinding eventBinding;

		GwtEvent gwtEvent;

		public Observable(DirectedLayout.Node node) {
			this.node = node;
		}

		public Observable(DirectedLayout.RendererInput input) {
			this.input = input;
			this.node = input.node;
		}

		public Observable(NodeEventBinding binding) {
			this.eventBinding = binding;
			this.node = binding.getNode();
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("property", node.getProperty(),
					"node", node, "eventBinding", eventBinding, "gwtEvent",
					gwtEvent);
		}
	}
}
