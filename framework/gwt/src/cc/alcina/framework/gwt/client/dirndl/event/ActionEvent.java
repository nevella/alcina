package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TagClass;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.PropertyNameTags;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * <p>
 * Intended as an inversion of control/variance helper. All subclasses are
 * handled by the nested handler class, which defaults to calling
 * event.handle(T) where T is the container - so essentially the *handler* code
 * is in the event and the *target* is the container
 *
 * <p>
 * TODO - Doc - an example or two. This is particularly useful with multiple
 * handlers (e.g. a context menu)
 *
 * 
 *
 * @param <T>
 */
public abstract class ActionEvent<T> extends ModelEvent<T, ActionEvent.Handler>
		implements Permissible {
	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.EVERYONE;
	}

	public Action asAction() {
		return new Action(this);
	}

	@Override
	public void dispatch(ActionEvent.Handler handler) {
		handler.onActionEvent(this);
	}

	public String getDescription() {
		return CommonUtils.deInfix(getClass().getSimpleName()).trim();
	}

	
	@Override
	public Class<? extends ModelEvent> getReceiverType() {
		return ActionEvent.class;
	}

	public String name() {
		String cssClassName = Ax
				.cssify(getClass().getSimpleName().replace("-event", ""));
		return cssClassName;
	}

	protected abstract void handle(T actionContext);

	/**
	 * <p>
	 * Presents an action as icon - text
	 */
	@Directed(
		bindings = @Binding(from = "title", type = Binding.Type.PROPERTY),
		receives = DomEvents.Click.class)
	@PropertyNameTags
	public static class Action extends Model
			implements DomEvents.Click.Handler {
		private final TagClass icon;

		private final String label;

		private ActionEvent event;

		public Action(ActionEvent event) {
			this.event = event;
			this.label = CommonUtils.capitaliseFirst(event.name());
			String className = Ax.format("icon %s", event.name());
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
			String description = event.getDescription();
			// only display if != text
			return Objects.equals(description, getLabel()) ? null : description;
		}

		@Override
		public void onClick(Click event) {
			WidgetUtils.squelchCurrentEvent();
			event.reemitAs(this, this.event.getClass());
		}
	}

	public static class ActionTransform
			extends AbstractModelTransform<ActionEvent, Action> {
		@Override
		public Action apply(ActionEvent t) {
			return t.asAction();
		}
	}

	public static interface ActionContextProvider<T> {
		public T provideActionContext();
	}

	public interface Handler<T> extends NodeEvent.Handler {
		default void onActionEvent(ActionEvent event) {
			Object instance = null;
			if (this instanceof ActionContextProvider) {
				instance = ((ActionContextProvider) this).provideActionContext();
			} else {
				instance = this;
			}
			event.handle((T) instance);
		}
	}
}
