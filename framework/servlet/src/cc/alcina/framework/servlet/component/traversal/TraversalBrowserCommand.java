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
	@KeyBinding(key = "m")
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
		name = "secondary mode: cycle",
		description = "Cycle through the secondary area display modes")
	@KeyBinding(key = "s")
	public static class SecondaryAreaDisplayCycle extends
			TraversalBrowserCommand<Object, SecondaryAreaDisplayCycle.Handler> {
		@Override
		public void dispatch(SecondaryAreaDisplayCycle.Handler handler) {
			handler.onSecondaryAreaDisplayCycle(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSecondaryAreaDisplayCycle(SecondaryAreaDisplayCycle event);
		}
	}

	@AppSuggestorCommand(
		parent = TraversalBrowserCommand.class,
		name = "help",
		description = "Show the application help panel")
	@KeyBinding(key = "?", modifiers = Modifier.SHIFT)
	public static class ToggleHelp
			extends ModelEvent<Object, ToggleHelp.Handler> {
		@Override
		public void dispatch(ToggleHelp.Handler handler) {
			handler.onToggleHelp(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onToggleHelp(ToggleHelp event);
		}
	}
}
