package cc.alcina.framework.gwt.client.dirndl.event;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.model.Link;

/**
 * <p>
 * Intended as an inversion of control helper. All subclasses are handled by the
 * nested handler class, which defaults to calling event.handle(T) where T is
 * the container - so essentially the *handler* code is in the event and the
 * *target* is the container
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public abstract class ActionEvent<T> extends ModelEvent<T, ActionEvent.Handler>
		implements Permissible {
	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.EVERYONE;
	}

	public Link asIconLink() {
		return asIconLink(false);
	}

	public Link asIconLink(boolean withText) {
		String cssClassName = Ax
				.cssify(getClass().getSimpleName().replace("-event", ""));
		return asLink("icon " + cssClassName, withText);
	}

	public Link asLink() {
		return asLink(null, true);
	}

	public Link asLink(String cssClassName, boolean withText) {
		return new Link().withTag("action").withModelEvent(getClass())
				.withText(withText ? name() : null).withClassName(cssClassName);
	}

	@Override
	public void dispatch(ActionEvent.Handler handler) {
		handler.onActionEvent(this);
	}

	public String getDescription() {
		return CommonUtils.deInfix(getClass().getSimpleName()).trim();
	}

	@Override
	public Class<ActionEvent.Handler> getHandlerClass() {
		return ActionEvent.Handler.class;
	}

	@Override
	public Class<? extends ModelEvent> getReceiverType() {
		return ActionEvent.class;
	}

	public String name() {
		return getClass().getSimpleName();
	}

	protected abstract void handle(T t);

	public static interface ContextProvider<T> {
		public T provideActionEventContext();
	}

	public interface Handler<T> extends NodeEvent.Handler {
		default void onActionEvent(ActionEvent event) {
			Object instance = null;
			if (this instanceof ContextProvider) {
				instance = ((ContextProvider) this).provideActionEventContext();
			} else {
				instance = this;
			}
			event.handle((T) instance);
		}
	}
}
