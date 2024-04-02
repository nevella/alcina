package cc.alcina.framework.gwt.client.dirndl.cmp.status;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed(
	className = "element",
	reemits = { DomEvents.Click.class, StatusElement.ElementClicked.class })
/*
 * TODO - only one of text,html, model is permissible
 */
public class StatusElement extends Model {
	long delay;

	private String text;

	long time;

	private boolean removing;

	private boolean closeOnClick = true;

	private Model model;

	public long getDelay() {
		return this.delay;
	}

	@Directed
	public Model getModel() {
		return this.model;
	}

	@Binding(type = Type.INNER_TEXT)
	public String getText() {
		return this.text;
	}

	public boolean isCloseOnClick() {
		return this.closeOnClick;
	}

	@Binding(type = Type.CSS_CLASS)
	public boolean isRemoving() {
		return this.removing;
	}

	public void setCloseOnClick(boolean closeOnClick) {
		this.closeOnClick = closeOnClick;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public void setRemoving(boolean removing) {
		boolean old_removing = this.removing;
		this.removing = removing;
		propertyChangeSupport().firePropertyChange("removing", old_removing,
				removing);
	}

	public void setText(String text) {
		this.text = text;
	}

	long getNext() {
		return Math.max(0L,
				time + delay - (removing ? 0 : StatusModel.removingMillis)
						- System.currentTimeMillis());
	}

	void maybeRemoving() {
		if (!removing && getNext() == 0) {
			setRemoving(true);
		}
	}

	boolean notExpired() {
		return !removing || getNext() != 0;
	}

	public static class CloseElement
			extends ModelEvent<StatusElement, CloseElement.Handler> {
		@Override
		public void dispatch(CloseElement.Handler handler) {
			handler.onCloseElement(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onCloseElement(CloseElement event);
		}
	}

	public static class ElementClicked
			extends ModelEvent<StatusElement, ElementClicked.Handler> {
		@Override
		public void dispatch(ElementClicked.Handler handler) {
			handler.onElementClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onElementClicked(StatusElement.ElementClicked event);
		}
	}
}