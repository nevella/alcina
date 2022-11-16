package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.InferredDomEvents.CtrlEnterPressed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.InferredDomEvents.EscapePressed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.ModelEvents.Close;
import cc.alcina.framework.gwt.client.dirndl.behaviour.ModelEvents.Close.Handler;
import cc.alcina.framework.gwt.client.dirndl.model.HasLinks;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * Does not use standard HTML dialog support (Safari 15.4 required)
 *
 * @author nick@alcina.cc
 *
 */
@Directed(
	tag = "dl-dialog",
	receives = { ModelEvents.Close.class, InferredDomEvents.EscapePressed.class,
			InferredDomEvents.CtrlEnterPressed.class })
public class Dialog extends Model.WithNode implements ModelEvents.Close.Handler,
		InferredDomEvents.EscapePressed.Handler,
		InferredDomEvents.CtrlEnterPressed.Handler {
	public static Builder builder() {
		return new Builder();
	}

	private final Model contents;

	private final OverlayPosition position;

	private final Actions actions;

	private Handler closeHandler;

	private Dialog(Builder builder) {
		contents = builder.contents;
		position = builder.position;
		actions = builder.actions;
		closeHandler = builder.closeHandler;
	}

	public void close() {
		if (closeHandler != null) {
			// force commit of focussed element (textarea, input) if it is a
			// child of this closing dialog.
			Element focus = WidgetUtils.getFocussedDocumentElement();
			if (focus != null
					&& provideElement().provideIsAncestorOf(focus, true)) {
				WidgetUtils.clearFocussedDocumentElement();
			}
			closeHandler.onClose(null);
		}
		OverlayPositions.get().show(this, false);
	}

	@Directed
	public Actions getActions() {
		return this.actions;
	}

	@Directed
	public Model getContents() {
		return this.contents;
	}

	public OverlayPosition getPosition() {
		return this.position;
	}

	@Override
	public void onClose(Close event) {
		close();
	}

	@Override
	public void onCtrlEnterPressed(CtrlEnterPressed event) {
		// TODO - probably better to route via an internal form, which then
		// fires submit, and close on that
		close();
	}

	@Override
	public void onEscapePressed(EscapePressed event) {
		close();
	}

	public void open() {
		OverlayPositions.get().show(this, true);
	}

	public static class Actions implements HasLinks {
		public static Actions close() {
			return new Actions().withClose();
		}

		private List<Link> actions = new ArrayList<>();

		@Override
		public void add(Link link) {
			actions.add(link);
		}

		public Actions withClose() {
			add(new Link().withModelEvent(ModelEvents.Close.class));
			return this;
		}
	}

	public static class Builder {
		private Model contents;

		private OverlayPosition position;

		private Actions actions;

		private ModelEvents.Close.Handler closeHandler;

		public Dialog build() {
			return new Dialog(this);
		}

		public Builder withClose() {
			actions = Actions.close();
			return this;
		}

		public Builder
				withCloseHandler(ModelEvents.Close.Handler closeHandler) {
			this.closeHandler = closeHandler;
			return this;
		}

		public Builder withContents(Model contents) {
			this.contents = contents;
			return this;
		}
	}
}
