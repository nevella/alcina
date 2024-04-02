package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

@Feature.Ref(Feature_TraversalProcessView_AppSuggestorImplementation.Shortcuts.class)
@AppSuggestorCommand(contexts = TraversalViewContext.class, name = "view")
public abstract class TraversalViewCommands<T, H extends NodeEvent.Handler>
		extends ModelEvent<T, H> implements AppSuggestorEvent {
	@AppSuggestorCommand(
		parent = TraversalViewCommands.class,
		name = "selection filter mode: view",
		description = "Selection filter mode: view")
	@KeyBinding(key = "v")
	public static class SelectionFilterModelView extends
			TraversalViewCommands<Object, SelectionFilterModelView.Handler> {
		@Override
		public void dispatch(SelectionFilterModelView.Handler handler) {
			handler.onSelectionFilterModelView(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionFilterModelView(SelectionFilterModelView event);
		}
	}

	@AppSuggestorCommand(
		parent = TraversalViewCommands.class,
		name = "selection filter mode: descendant",
		description = "Selection filter mode: descendant")
	@KeyBinding(key = "d")
	public static class SelectionFilterModelDescendant extends
			TraversalViewCommands<Object, SelectionFilterModelDescendant.Handler> {
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
		parent = TraversalViewCommands.class,
		name = "selection filter mode: containment",
		description = "Selection filter mode: containment")
	@KeyBinding(key = "c")
	public static class SelectionFilterModelContainment extends
			TraversalViewCommands<Object, SelectionFilterModelContainment.Handler> {
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
		parent = TraversalViewCommands.class,
		name = "property display: cycle",
		description = "Property display: cycle mode")
	@KeyBinding(key = "p")
	public static class PropertyDisplayCycle extends
			TraversalViewCommands<Object, PropertyDisplayCycle.Handler> {
		@Override
		public void dispatch(PropertyDisplayCycle.Handler handler) {
			handler.onPropertyDisplayCycle(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPropertyDisplayCycle(PropertyDisplayCycle event);
		}
	}
}
