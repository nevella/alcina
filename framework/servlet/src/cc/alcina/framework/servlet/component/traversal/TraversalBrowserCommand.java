package cc.alcina.framework.servlet.component.traversal;

import com.google.gwt.dom.client.NativeEvent.Modifier;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

@Feature.Ref(Feature_TraversalBrowser_AppSuggestorImplementation.Shortcuts.class)
@AppSuggestorCommand(
	contexts = TraversalBrowser.CommandContext.class,
	name = "view")
public abstract class TraversalBrowserCommand<T, H extends NodeEvent.Handler>
		extends ModelEvent<T, H> implements AppSuggestorEvent {
	@AppSuggestorCommand(
		parent = TraversalBrowserCommand.class,
		name = "selection filter mode: view",
		description = "Selection filter mode: view")
	@KeyBinding(key = "v")
	public static class SelectionFilterModelView extends
			TraversalBrowserCommand<Object, SelectionFilterModelView.Handler> {
		@Override
		public void dispatch(SelectionFilterModelView.Handler handler) {
			handler.onSelectionFilterModelView(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionFilterModelView(SelectionFilterModelView event);
		}
	}

	@AppSuggestorCommand(
		parent = TraversalBrowserCommand.class,
		name = "selection filter mode: descendant",
		description = "Selection filter mode: descendant")
	@KeyBinding(key = "d")
	public static class SelectionFilterModelDescendant extends
			TraversalBrowserCommand<Object, SelectionFilterModelDescendant.Handler> {
		@Override
		public void dispatch(SelectionFilterModelDescendant.Handler handler) {
			handler.onSelectionFilterModelDescendant(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionFilterModelDescendant(
					SelectionFilterModelDescendant event);
		}
	}

	@AppSuggestorCommand(
		parent = TraversalBrowserCommand.class,
		name = "selection filter mode: containment",
		description = "Selection filter mode: containment")
	@KeyBinding(key = "c")
	public static class SelectionFilterModelContainment extends
			TraversalBrowserCommand<Object, SelectionFilterModelContainment.Handler> {
		@Override
		public void dispatch(SelectionFilterModelContainment.Handler handler) {
			handler.onSelectionFilterModelContainment(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionFilterModelContainment(
					SelectionFilterModelContainment event);
		}
	}

	@AppSuggestorCommand(
		parent = TraversalBrowserCommand.class,
		name = "property display: cycle",
		description = "Property display: cycle mode")
	@KeyBinding(key = "p")
	public static class PropertyDisplayCycle extends
			TraversalBrowserCommand<Object, PropertyDisplayCycle.Handler> {
		@Override
		public void dispatch(PropertyDisplayCycle.Handler handler) {
			handler.onPropertyDisplayCycle(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPropertyDisplayCycle(PropertyDisplayCycle event);
		}
	}

	@AppSuggestorCommand(
		parent = TraversalBrowserCommand.class,
		name = "input/output: cycle",
		description = "Cycle through the input/output display modes")
	@KeyBinding(key = "i")
	public static class InputOutputCycle
			extends TraversalBrowserCommand<Object, InputOutputCycle.Handler> {
		@Override
		public void dispatch(InputOutputCycle.Handler handler) {
			handler.onInputOutputCycle(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onInputOutputCycle(InputOutputCycle event);
		}
	}
}
