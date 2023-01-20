package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.DomRect;
import com.google.gwt.dom.client.Element;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.CtrlEnterPressed;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.EscapePressed;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.MouseDownOutside;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Close;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Close.Handler;
import cc.alcina.framework.gwt.client.dirndl.model.HasLinks;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPositions.ContainerOptions;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * Does not use standard HTML dialog support (Safari 15.4 required)
 *
 * Models all popups + dialogs
 *
 * @author nick@alcina.cc
 *
 */
@Directed(
	receives = { ModelEvents.Close.class, InferredDomEvents.EscapePressed.class,
			InferredDomEvents.CtrlEnterPressed.class,
			InferredDomEvents.MouseDownOutside.class })
public class Overlay extends Model.WithNode implements
		ModelEvents.Close.Handler, InferredDomEvents.EscapePressed.Handler,
		InferredDomEvents.CtrlEnterPressed.Handler,
		InferredDomEvents.MouseDownOutside.Handler {
	public static Builder builder() {
		return new Builder();
	}

	private final Model contents;

	private final OverlayPosition position;

	private final Actions actions;

	private Handler closeHandler;

	private boolean modal;

	private boolean removeOnMouseDownOutside;

	private Overlay(Builder builder) {
		contents = builder.contents;
		position = builder.position;
		actions = builder.actions;
		modal = builder.modal;
		closeHandler = builder.closeHandler;
		removeOnMouseDownOutside = builder.removeOnMouseDownOutside;
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
		OverlayPositions.get().hide(this);
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

	@Override
	public void onMouseDownOutside(MouseDownOutside event) {
		if (removeOnMouseDownOutside) {
			close();
		}
	}

	public void open() {
		ContainerOptions options = new ContainerOptions().withModal(modal)
				.withPosition(position);
		OverlayPositions.get().show(this, options);
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
			add(new Link().withModelEvent(ModelEvents.Close.class)
					.withText("Close"));
			return this;
		}
	}

	public static class Builder {
		private Model contents;

		private OverlayPosition position = new OverlayPosition();

		private Actions actions;

		private ModelEvents.Close.Handler closeHandler;

		private boolean modal;

		boolean removeOnMouseDownOutside = true;

		public Overlay build() {
			return new Overlay(this);
		}

		public Builder centerDropdown(DomRect rect, Model model) {
			position.centerDropdown(rect);
			withContents(model);
			withRemoveOnMouseDownOutside(true);
			return this;
		}

		public OverlayPosition getPosition() {
			return this.position;
		}

		public Builder positionViewportCentered() {
			position.viewportCentered(true);
			return this;
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

		public Builder withModal(boolean modal) {
			this.modal = modal;
			return this;
		}

		public Builder
				withRemoveOnMouseDownOutside(boolean removeOnMouseDownOutside) {
			this.removeOnMouseDownOutside = removeOnMouseDownOutside;
			return this;
		}
	}
}
