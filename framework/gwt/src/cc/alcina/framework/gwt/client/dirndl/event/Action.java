package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TagClass;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * <p>
 * Action implements a simple action/handler pattern ideal for context menus and
 * dropdowns. The action defines its visual elemets (name, description, css) and
 * code to execute onclick (perform())
 *
 * <p>
 * The parent container (say a dropdown) receives the click->ActionEvent
 * modelEvent, then dispatches back to the Action.perform method *with the
 * appropriate context* - it kills two birds with one stone, pretty much -
 * handling is action-specific and dispatch is nice (no switch, just class
 * based), but with the (generally necessary) capacity of the parent to offer
 * context to the handling code
 *
 * *
 * <p>
 * TODO - Doc - an example or two. This is particularly useful with multiple
 * handlers (e.g. a context menu)
 *
 *
 * @param <T>
 *            the context used by the action handler. Either the action
 *            container or an arbitrary object (if the container implements
 *            ActionContextProvider)
 */
@Reflected
public abstract class Action<T> implements Permissible {
	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.EVERYONE;
	}

	public ActionArea asArea() {
		return new ActionArea(this);
	}

	public String getDescription() {
		return CommonUtils.deInfix(getClass().getSimpleName()).trim();
	}

	public String name() {
		String cssClassName = Ax.cssify(getClass().getSimpleName());
		return cssClassName;
	}

	public abstract void perform(T actionContext);

	/**
	 * <p>
	 * Presents an action as icon - text
	 */
	@Directed(
		tag = "action",
		bindings = @Binding(from = "title", type = Binding.Type.PROPERTY))
	@Directed.PropertyNameTags
	public static class ActionArea extends Model
			implements DomEvents.Click.Handler {
		private final TagClass icon;

		private final String label;

		private Action action;

		public ActionArea(Action action) {
			this.action = action;
			this.label = CommonUtils.capitaliseFirst(action.name());
			String className = Ax.format("icon %s", action.name());
			this.icon = new TagClass("icon", className);
		}

		@Directed
		public TagClass getIcon() {
			return icon;
		}

		@Directed
		public String getLabel() {
			return label;
		}

		public String getTitle() {
			String description = action.getDescription();
			// only display if != text
			return Objects.equals(description, getLabel()) ? null : description;
		}

		@Override
		public void onClick(Click event) {
			WidgetUtils.squelchCurrentEvent();
			event.reemitAs(this, ActionEvent.class, action);
		}
	}

	public static interface ActionContextProvider<T> {
		public T provideActionContext();
	}

	public static final class ActionEvent
			extends ModelEvent<Action, ActionEvent.Handler> {
		@Override
		public void dispatch(ActionEvent.Handler handler) {
			handler.onActionEvent(this);
		}

		public interface Handler<T> extends NodeEvent.Handler {
			default void onActionEvent(ActionEvent event) {
				Object actionContext = null;
				if (this instanceof ActionContextProvider) {
					actionContext = ((ActionContextProvider) this)
							.provideActionContext();
				} else {
					actionContext = this;
				}
				Action action = event.getModel();
				action.perform((T) actionContext);
			}
		}
	}

	public static class ActionTransform
			extends AbstractModelTransform<Action, ActionArea> {
		@Override
		public ActionArea apply(Action t) {
			return t.asArea();
		}
	}
}
