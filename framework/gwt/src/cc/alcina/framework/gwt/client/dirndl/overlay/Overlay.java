package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.DomRect;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.HasNativeEvent;
import com.google.gwt.event.shared.GwtEvent;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.CtrlEnterPressed;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.EscapePressed;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.MouseDownOutside;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Close;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirndlAccess;
import cc.alcina.framework.gwt.client.dirndl.model.HasLinks;
import cc.alcina.framework.gwt.client.dirndl.model.HasNode;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPositions.ContainerOptions;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * <p>
 * Does not use standard HTML dialog support (Safari 15.4 required)
 *
 * <p>
 * Models all popups + dialogs
 *
 * <p>
 * CloseHandler fires on any close (including say [esc] key, mousedown outside)
 * - CommitHandler fires on [OK], [Ctrl-Enter]
 *
 * <p>
 * Event routing: because an overlay is outside the dom (and dirndl node)
 * stucture of the originating dirndl node, but is most easily managed by the
 * originating node's model, dirndl routes events that bubble out of the overlay
 * to the originating node's model (since the placement of the overlay in the
 * node tree is due to constraints of HTML absolute positioning and z-order, not
 * the underlying containment logic). Note that only model (not gwt/dom) events
 * are thus rerouted.
 * <p>
 * FIXME - doc - this goes in dirndl events as an edge case
 * <p>
 * FIXME - dirndl 1x1d.0 - commit + close should just be vanilla dirndl events
 * (now that logical routing is implemented) - test all implementations. Note
 * that should emit a "closed" event, not "close". Document when to emit
 * past-tense (when it happened) rather than present/imperative (a gesture
 * indiciating 'do x' happened)
 *
 * @author nick@alcina.cc
 *
 */
@Directed(
	receives = { ModelEvents.Close.class, InferredDomEvents.EscapePressed.class,
			InferredDomEvents.CtrlEnterPressed.class,
			InferredDomEvents.MouseDownOutside.class },
	bindings = @Binding(from = "cssClass", type = Type.CLASS_PROPERTY))
public class Overlay extends Model.WithNode implements
		ModelEvents.Close.Handler, InferredDomEvents.EscapePressed.Handler,
		InferredDomEvents.CtrlEnterPressed.Handler,
		InferredDomEvents.MouseDownOutside.Handler, Model.RerouteBubbledEvents {
	public static Builder builder() {
		return new Builder();
	}

	private final Model contents;

	private final OverlayPosition position;

	private final Actions actions;

	private Close.Handler closeHandler;

	private Commit.Handler commitHandler;

	private boolean modal;

	private boolean removeOnMouseDownOutside;

	private boolean allowCloseWithoutCommit = true;

	private boolean open;

	private String cssClass;

	private Model logicalParent;

	/*
	 * Don't close this overlay if the child is the event target
	 */
	private Overlay childOverlay;

	private Overlay(Builder builder) {
		contents = builder.contents;
		position = builder.position;
		actions = builder.actions;
		modal = builder.modal;
		closeHandler = builder.closeHandler;
		commitHandler = builder.commitHandler;
		allowCloseWithoutCommit = builder.allowCloseWithoutCommit;
		removeOnMouseDownOutside = builder.removeOnMouseDownOutside;
		logicalParent = builder.logicalParent;
		cssClass = builder.cssClass;
	}

	public void close(boolean commit) {
		if (!open) {
			return;
		}
		if (!commit && !allowCloseWithoutCommit) {
			return;
		}
		open = false;
		if (commitHandler != null && commit) {
			// force commit of focussed element (textarea, input) if it is a
			// child of this closing dialog.
			Element focus = WidgetUtils.getFocussedDocumentElement();
			if (focus != null
					&& provideElement().provideIsAncestorOf(focus, true)) {
				WidgetUtils.clearFocussedDocumentElement();
			}
			commitHandler.onCommit(null);
		}
		if (closeHandler != null) {
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

	public String getCssClass() {
		return this.cssClass;
	}

	public OverlayPosition getPosition() {
		return this.position;
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (logicalParent != null && logicalParent instanceof HasNode) {
			Node sourceNode = ((HasNode) logicalParent).provideNode();
			Overlay ancestorOverlay = DirndlAccess.ComponentAncestorAccess
					.getAncestor(sourceNode, this);
			if (ancestorOverlay != null) {
				ancestorOverlay.setChildOverlay(event.isBound() ? this : null);
			}
		}
	}

	@Override
	public void onClose(Close event) {
		close(true);
	}

	@Override
	public void onCtrlEnterPressed(CtrlEnterPressed event) {
		// TODO - probably better to route via an internal form, which then
		// fires submit, and close on that
		close(true);
	}

	@Override
	public void onEscapePressed(EscapePressed event) {
		close(false);
	}

	@Override
	public void onMouseDownOutside(MouseDownOutside event) {
		if (removeOnMouseDownOutside) {
			GwtEvent gwtEvent = event.getContext().getOriginatingGwtEvent();
			// don't close if a descendant overlay received the event
			if (gwtEvent instanceof HasNativeEvent) {
				NativeEvent nativeEvent = ((HasNativeEvent) gwtEvent)
						.getNativeEvent();
				EventTarget eventTarget = nativeEvent.getEventTarget();
				if (eventTarget.isElement()) {
					Element element = eventTarget.asElement();
					if (selfOrDescendantOverlayContains(element)) {
						return;
					}
				}
			}
			close(false);
		}
	}

	public void open() {
		ContainerOptions options = new ContainerOptions().withModal(modal)
				.withPosition(position);
		OverlayPositions.get().show(this, options);
		open = true;
	}

	@Override
	public Model rerouteBubbledEventsTo() {
		return logicalParent;
	}

	@Override
	public String toString() {
		return Ax.format(
				"Overlay:\n\tcontents:     %s\n\tlogicalParent: %s\n\tchildOverlay: %s",
				contents, logicalParent, childOverlay);
	}

	private boolean selfOrDescendantOverlayContains(Element element) {
		if (provideElement().provideIsAncestorOf(element, true)) {
			return true;
		}
		if (childOverlay != null
				&& childOverlay.selfOrDescendantOverlayContains(element)) {
			return true;
		}
		return false;
	}

	protected Overlay getChildOverlay() {
		return this.childOverlay;
	}

	protected void setChildOverlay(Overlay childOverlay) {
		this.childOverlay = childOverlay;
	}

	public static class Actions extends Model implements HasLinks {
		public static Actions close() {
			return new Actions().withClose();
		}

		public static Actions ok() {
			return new Actions().withOk();
		}

		/*
		 * Removed final to avoid GWT serialization warnings - for the moment
		 */
		// private final List<Link> actions = new ArrayList<>();
		private List<Link> actions = new ArrayList<>();

		@Override
		public void add(Link link) {
			actions.add(link);
		}

		@Directed
		public List<Link> getActions() {
			return this.actions;
		}

		public Actions withClose() {
			add(new Link().withModelEvent(ModelEvents.Close.class)
					.withText("Close"));
			return this;
		}

		public Actions withOk() {
			add(new Link().withModelEvent(ModelEvents.Close.class)
					.withText("OK"));
			return this;
		}
	}

	public static class Builder {
		private Model contents;

		private OverlayPosition position = new OverlayPosition();

		private Actions actions;

		private ModelEvents.Close.Handler closeHandler;

		private ModelEvents.Commit.Handler commitHandler;

		private boolean modal;

		boolean removeOnMouseDownOutside = true;

		boolean allowCloseWithoutCommit = true;

		private String cssClass;

		private Model logicalParent;

		public Overlay build() {
			return new Overlay(this);
		}

		public Builder centerDropdown(DomRect rect, Model model) {
			return dropdown(Position.CENTER, rect, null, model);
		}

		public Builder dropdown(OverlayPosition.Position xalign, DomRect rect,
				Model logicalParent, Model contents) {
			position.dropdown(xalign, rect);
			withLogicalParent(logicalParent);
			withContents(contents);
			withRemoveOnMouseDownOutside(true);
			return this;
		}

		public Model getLogicalParent() {
			return this.logicalParent;
		}

		public OverlayPosition getPosition() {
			return this.position;
		}

		public Builder positionViewportCentered() {
			position.viewportCentered(true);
			return this;
		}

		public Builder
				withAllowCloseWithoutCommit(boolean allowCloseWithoutCommit) {
			this.allowCloseWithoutCommit = allowCloseWithoutCommit;
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

		public Builder
				withCommitHandler(ModelEvents.Commit.Handler commitHandler) {
			this.commitHandler = commitHandler;
			return this;
		}

		public Builder withContents(Model contents) {
			this.contents = contents;
			return this;
		}

		public Builder withCssClass(String cssClass) {
			this.cssClass = cssClass;
			return this;
		}

		/*
		 * Will be used as both an event bubbler and a link to the parent
		 * overlay (if any)
		 */
		public Builder withLogicalParent(Model logicalParent) {
			this.logicalParent = logicalParent;
			return this;
		}

		public Builder withModal(boolean modal) {
			this.modal = modal;
			return this;
		}

		public Builder withOk() {
			actions = Actions.ok();
			return this;
		}

		public Builder
				withRemoveOnMouseDownOutside(boolean removeOnMouseDownOutside) {
			this.removeOnMouseDownOutside = removeOnMouseDownOutside;
			return this;
		}
	}
}
