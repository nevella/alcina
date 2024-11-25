package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.dom.client.DomRect;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.HasNativeEvent;
import com.google.gwt.event.shared.GwtEvent;

import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.CtrlEnterPressed;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.EscapePressed;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.MouseDownOutside;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Close;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Submit;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirndlAccess;
import cc.alcina.framework.gwt.client.dirndl.model.HasLinks;
import cc.alcina.framework.gwt.client.dirndl.model.HasNode;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.ViewportRelative;
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
 * - SubmitHandler fires on [OK], [Ctrl-Enter]
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
 * Overlay css styles are generated via css-ification of logical parent and
 * optional logical ancestor classes (and doc). Basically,
 * class="cssify(logicalParent.class) cssify(logicalAncestor0.class) etc". This
 * gives *more or less* the same sass keys as would exist for the logical parent
 * (although css classes, not containing tags). FIXME - doc - example
 *
 *
 *
 *
 */
@Directed(
	emits = { ModelEvents.Closed.class, ModelEvents.Submit.class,
			ModelEvents.Opened.class })
public class Overlay extends Model implements ModelEvents.Close.Handler,
		InferredDomEvents.EscapePressed.Handler,
		InferredDomEvents.CtrlEnterPressed.Handler,
		InferredDomEvents.MouseDownOutside.Handler, ModelEvents.Submit.Handler,
		ModelEvents.Closed.Handler {
	public static Builder builder() {
		return new Builder();
	}

	private final Model contents;

	private final OverlayPosition position;

	private final Actions actions;

	private boolean modal;

	private boolean removeOnMouseDownOutside;

	private boolean allowCloseWithoutSubmit = true;

	private boolean open;

	private String cssClass;

	Model logicalParent;

	/*
	 * For event bubbling - if the logicalParent is detached, try the secondary
	 * (if it exists)
	 */
	Model secondaryLogicalEventReroute;

	private List<Class<? extends Model>> logicalAncestors;

	private ModelEvents.Submit.Handler submitHandler;

	private ModelEvents.Closed.Handler closedHandler;

	/*
	 * Don't close this overlay if the child is the event target
	 */
	private Overlay childOverlay;

	private String cssClassParameter;

	private Overlay(Builder builder) {
		contents = builder.contents;
		position = builder.position;
		actions = builder.actions;
		modal = builder.modal;
		allowCloseWithoutSubmit = builder.allowCloseWithoutSubmit;
		removeOnMouseDownOutside = builder.removeOnMouseDownOutside;
		logicalParent = builder.logicalParent;
		secondaryLogicalEventReroute = builder.secondaryLogicalEventReroute;
		logicalAncestors = builder.logicalAncestors;
		submitHandler = builder.submitHandler;
		closedHandler = builder.closedHandler;
		cssClassParameter = builder.cssClass;
		computeCssClass();
	}

	public boolean close() {
		return close(null, false);
	}

	/**
	 * @return true if closed
	 */
	public boolean close(GwtEvent from, boolean submit) {
		if (!open) {
			return true;
		}
		if (!submit && !allowCloseWithoutSubmit) {
			return false;
		}
		if (childOverlay != null) {
			if (!childOverlay.close(from, submit)) {
				return false;
			}
		}
		// double-check open, since childOverlay.close may have re-called
		if (!open) {
			return true;
		}
		open = false;
		Node node = provideNode();
		if (submit) {
			// force commit of focussed element changes (textarea, input) if it
			// is a child of this closing dialog.
			if (Al.isBrowser()) {
				Element focus = WidgetUtils.getFocussedDocumentElement();
				if (focus != null
						&& provideElement().provideIsAncestorOf(focus, true)) {
					WidgetUtils.clearFocussedDocumentElement();
				}
			}
			NodeEvent.Context.fromEvent(from, node)
					.dispatch(ModelEvents.Submit.class, null);
		}
		/*
		 * Any re-emission of events, model triggers should happen in the
		 * BeforeClosed handlers
		 */
		NodeEvent.Context.fromEvent(from, node)
				.dispatch(ModelEvents.BeforeClosed.class, null);
		OverlayPositions.get().hide(this, true);
		/*
		 * node will be removed from the layout at this point, but its parent
		 * refs will still be valid - a little dodgy, but it works
		 */
		NodeEvent.Context.fromEvent(from, node)
				.dispatch(ModelEvents.Closed.class, null);
		return true;
	}

	/*
	 * Compute the overlay class based on logical ancestors + contents
	 */
	void computeCssClass() {
		// deliberately does not try to access @Directed(cssClass) - since
		// overlay creation is imperative and the caller has essentially full
		// control of the class selector (via logicalAncestors)...this is good
		// enough, I think
		//
		// FIXME - dirndl 1x3 - actually, at least for contentdecorator it would
		// be nice
		Stream<String> derivedClasses = Stream
				.concat(logicalAncestors.stream(),
						Stream.of(logicalParent, this, contents))
				.filter(Objects::nonNull).map(CommonUtils::classOrSelf)
				.map(Class::getSimpleName);
		String cssClass = Stream
				.concat(derivedClasses, Stream.of(cssClassParameter))
				.filter(Objects::nonNull).map(Ax::cssify)
				.collect(Collectors.joining(" "));
		setCssClass(cssClass);
	}

	@Directed
	public Actions getActions() {
		return this.actions;
	}

	protected Overlay getChildOverlay() {
		return this.childOverlay;
	}

	@Directed
	public Model getContents() {
		return this.contents;
	}

	@Binding(type = Type.CLASS_PROPERTY)
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
				// and inherit logical ancestry for styling. order is (ancestor)
				// ancestors > parent > overlay > contents
				if (logicalAncestors.isEmpty()) {
					logicalAncestors = Stream
							.concat(ancestorOverlay.logicalAncestors.stream(),
									Stream.of(
											ancestorOverlay.logicalParent
													.getClass(),
											ancestorOverlay.getClass(),
											ancestorOverlay.contents
													.getClass()))
							.collect(Collectors.toList());
					computeCssClass();
				}
			}
		}
		if (event.isBound()) {
			event.reemitAs(this, ModelEvents.Opened.class);
		} else {
		}
	}

	@Override
	public void onClose(Close event) {
		close(event, true);
	}

	boolean reemittingClose;

	@Override
	public void onClosed(Closed event) {
		Node node = provideNode();
		if (node != null && event.getContext().node != node) {
			// child overlay
			return;
		}
		// custom reemission because node will be detached from layout
		if (reemittingClose) {
			event.bubble();
			return;
		}
		if (closedHandler != null) {
			closedHandler.onClosed(event);
		}
		try {
			reemittingClose = true;
			event.reemit();
		} finally {
			reemittingClose = false;
		}
	}

	@Override
	public void onCtrlEnterPressed(CtrlEnterPressed event) {
		// TODO - probably better to route via an internal form, which then
		// fires submit, and close on that
		close(event, true);
	}

	@Override
	public void onEscapePressed(EscapePressed event) {
		close(event, false);
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
			close(event, false);
		}
	}

	@Override
	public void onSubmit(Submit event) {
		if (event.checkReemitted(this)) {
			return;
		}
		if (submitHandler != null) {
			submitHandler.onSubmit(event);
		}
		event.reemit();
	}

	public void open() {
		ContainerOptions options = new ContainerOptions().withModal(modal)
				.withPosition(position);
		OverlayPositions.get().show(this, options);
		open = true;
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

	protected void setChildOverlay(Overlay childOverlay) {
		this.childOverlay = childOverlay;
	}

	public void setCssClass(String cssClass) {
		String old_cssClass = this.cssClass;
		this.cssClass = cssClass;
		propertyChangeSupport().firePropertyChange("cssClass", old_cssClass,
				cssClass);
	}

	@Override
	public String toString() {
		return Ax.format(
				"Overlay:\n\tcontents:     %s\n\tlogicalParent: %s\n\tchildOverlay: %s",
				contents, logicalParent, childOverlay);
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
					.withTextFromModelEvent());
			return this;
		}

		public Actions withOk() {
			add(new Link().withModelEvent(ModelEvents.Close.class)
					.withText("OK"));
			return this;
		}
	}

	public static class Builder {
		Model secondaryLogicalEventReroute;

		List<Class<? extends Model>> logicalAncestors = List.of();

		Model contents;

		OverlayPosition position = new OverlayPosition();

		Actions actions;

		boolean modal;

		boolean removeOnMouseDownOutside = true;

		boolean allowCloseWithoutSubmit = true;

		Model logicalParent;

		ModelEvents.Submit.Handler submitHandler;

		ModelEvents.Closed.Handler closedHandler;

		String cssClass;

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

		public Builder positionViewportCentered() {
			position.viewportCentered();
			return this;
		}

		public Builder
				positionViewportRelative(ViewportRelative viewportRelative) {
			position.viewportRelative(viewportRelative);
			return this;
		}

		public Builder
				withAllowCloseWithoutSubmit(boolean allowCloseWithoutSubmit) {
			this.allowCloseWithoutSubmit = allowCloseWithoutSubmit;
			return this;
		}

		public Builder withClose() {
			actions = Actions.close();
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

		public Builder withLogicalAncestors(
				List<Class<? extends Model>> logicalAncestors) {
			this.logicalAncestors = logicalAncestors;
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

		/*
		 * Will be used as a secondary event bubbler (if the logical parent is
		 * attached)
		 */
		public Builder withSecondaryLogicalEventReroute(
				Model secondaryLogicalEventReroute) {
			this.secondaryLogicalEventReroute = secondaryLogicalEventReroute;
			return this;
		}

		public Builder withModal(boolean modal) {
			this.modal = modal;
			return this;
		}

		public Builder
				withClosedHandler(ModelEvents.Closed.Handler closedHandler) {
			this.closedHandler = closedHandler;
			return this;
		}

		public Builder
				withSubmitHandler(ModelEvents.Submit.Handler submitHandler) {
			this.submitHandler = submitHandler;
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

		public void withFixed(boolean fixed) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'withFixed'");
		}
	}

	public static class Positioned
			extends ModelEvent<OverlayContainer, Positioned.Handler> {
		@Override
		public void dispatch(Positioned.Handler handler) {
			handler.onPositioned(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPositioned(Positioned event);
		}
	}

	public static class PositionedDescendants extends
			ModelEvent.DescendantEvent<Object, PositionedDescendants.Handler, PositionedDescendants.Emitter> {
		@Override
		public void dispatch(PositionedDescendants.Handler handler) {
			handler.onPositionedDescendants(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPositionedDescendants(PositionedDescendants event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}
}
