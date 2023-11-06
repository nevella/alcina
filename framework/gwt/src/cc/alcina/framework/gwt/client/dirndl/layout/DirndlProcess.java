package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.event.shared.GwtEvent;

import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.FormatBuilder;

public class DirndlProcess {
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
			FormatBuilder format = new FormatBuilder();
			format.appendKeyValues("property", node.getProperty(), "node", node,
					"eventBinding", eventBinding, "gwtEvent", gwtEvent);
			return format.toString();
		}
	}
}
