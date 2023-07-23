package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.NativeEvent.NativeBeforeInputEvent;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.BeforeInput;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.MouseUp;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Close;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.dom.RelativeInputModel;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ContentDecoratorEvents.ReferenceSelected;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition;

/**
 * <p>
 * This class supports decoration of a document 'measure' (range). The first
 * uses are '@ mentions' and '# tags'.
 * <p>
 * The whole process is reasonably complex - and a WIP. Here's a sketch:
 * <ul>
 * <li>User enters some triggering key events - say the '@' key
 * <li>BeforeInput event interception marks the next input event as requiring a
 * check (query - can we just check the input data?)(answer - beforeinput is
 * easier, since it tells us about the delta)
 * <li>Input event interception checks if the state is valid for content
 * decoration (particularly that the cursor (DOM selection) is collapsed) - see
 * {@link #onInput(Input)} for details - and, if checks pass:
 * <ul>
 * <li>modifies the dom
 * <li>splits the text node if necessary
 * <li>wraps the '@' in a decorator tag, e.g. {@code <mention>@</mention>}
 * <li>ensures the selection cursor is after the '@'
 * <li>triggers the overlay display
 * </ul>
 * <li>The overlay
 * <ul>
 * <li>Generally displays a {@link Suggestor}, with the filter being the
 * decorator tag (in the ContentEditable DOM subtree)
 * <li>Routes up/down/enter/escape keys (cursor/focus in the CE) to the
 * Suggestor
 * </ul>
 * </ul>
 * <p>
 * Other: see the Feature documentation (and constraints/tests)
 *
 * <p>
 * Validation
 * <ul>
 * <li>Validate the DOM on CE attach, commit and mutation (latter being, for
 * example, clipboard paste, or backspace removing the decorator trigger) (TODO)
 * <li>Validation 1: (decorator tags can't contain tags, basically)
 * <li>Validation 2: decorator tags should be stripped if non-committed (no
 * associated entity data) and not the keyboard focus
 * <li>Validation 3: (server-side, probably) decorator tags should be stripped
 * if invalid entity data (e.g. not permitted)
 * </ul>
 * <p>
 * Implementation notes:
 * <ul>
 * <li>Note the routing of say up/down arrow when showing the a suggestor, in an
 * overlay, to modify the decorator entity. Classic UI event routing!
 * <li>Note that the model which allows content decoration (typically wrapping a
 * contenteditable dom element) should have a KeyboardNavigation member (which
 * initiates the decorator event routing)
 * </ul>
 *
 *
 */
/*
 * Note that BeforeInput support is relatively recent (2021 for FF)
 *
 * FIXME - SelectionDecorator - must avoid input triggering in any
 * SelectionDecorator associated with the contenteditable
 *
 * TODO - general name is 'contentdecorator' - tag abstraction is 'Decorator'
 *
 * overlay hide triggers: trigger tag exit
 */
@Feature.Ref(Feature_Dirndl_ContentDecorator.class)
public class ContentDecorator<T>
		implements DomEvents.BeforeInput.Handler, DomEvents.Input.Handler,
		DomEvents.MouseUp.Handler, DomEvents.KeyDown.Handler,
		ContentDecoratorEvents.ReferenceSelected.Handler,
		KeyboardNavigation.Navigation.Handler, ModelEvents.Close.Handler,
		HasDecorator {
	public static ContentDecorator.Builder builder() {
		return new Builder();
	}

	String triggerSequence;

	boolean checkNextInput = false;

	String tag;

	Model logicalParent;

	Topic<Input> topicInput = Topic.create();

	DecoratorNodeOld decorator;

	DecoratorChooser chooser;

	BiFunction<ContentDecorator, DomNode, DecoratorChooser> chooserProvider;

	Function<T, String> itemRenderer;

	HasDecorator decoratorParent;

	private ContentDecorator(ContentDecorator.Builder builder) {
		this.logicalParent = builder.logicalParent;
		this.tag = builder.tag;
		this.chooserProvider = builder.chooserProvider;
		this.triggerSequence = builder.triggerSequence;
		Preconditions.checkState(triggerSequence.length() == 1);
		this.itemRenderer = builder.itemRenderer;
		this.decoratorParent = builder.decoratorParent;
	}

	/*
	 * Pass up to the container (since there may be multiple decorators)
	 */
	@Override
	public boolean hasDecorator(DomNode node) {
		return decoratorParent.hasDecorator(node);
	}

	public boolean isActive() {
		return chooser != null;
	}

	@Override
	public void onBeforeInput(BeforeInput event) {
		NativeBeforeInputEvent nativeBefore = event.getNativeBeforeInputEvent();
		String data = nativeBefore.getData();
		checkNextInput = Objects.equals(data, triggerSequence);
	}

	@Override
	public void onClose(Close event) {
		if (chooser != null) {
			chooser.onClose(event);
			chooser = null;
		}
	}

	/**
	 * <p>
	 * Check if the selection context is valid for decorator creation, and if
	 * so, perform the decorator creation sequence. The checks are:
	 * <ul>
	 * <li>Check if the text content is valid. Either the trigger sequence
	 * begins a text node, or is preceded by one of {' ', '(', '['}
	 * <li>Check if any ancestor cannot contain a decorator. Invalid ancestors
	 * include at least other decorators and {@code <a>} tags [WIP - use the
	 * fragment model for this]
	 *
	 * </ul>
	 */
	@Override
	public void onInput(Input event) {
		if (checkNextInput) {
			checkNextInput = false;
			RelativeInputModel relativeInput = new RelativeInputModel();
			if (relativeInput.isTriggerable()) {
				/*
				 * requires mutations to be processed, so schedule
				 */
				Scheduler.get().scheduleFinally(() -> onInput0(relativeInput));
			}
		}
		topicInput.publish(event);
	}

	@Override
	public void onKeyDown(KeyDown event) {
		validateSelection();
	}

	@Override
	public void onMouseUp(MouseUp event) {
		validateSelection();
	}

	@Override
	public void onNavigation(Navigation event) {
		if (chooser != null) {
			chooser.suggestor.onNavigation(event);
		}
	}

	@Override
	public void onReferenceSelected(ReferenceSelected event) {
		if (event.getContext().getPrevious().node.getModel() == chooser) {
			decorator.toNonEditable();
			T model = (T) event.getModel();
			decorator.setModel(model, itemRenderer.apply(model));
		}
	}

	/*
	 * This isn't ideal - but input beahviour is so complex, it's easier to do a
	 * cleanup of all rather than try and monitor active decorators
	 */
	void cleanupInvalidDecorators() {
		DomNode.from(logicalParent.provideElement()).stream()
				.filter(n -> n.tagIs(tag))
				.map(n -> new DecoratorNodeOld(this, n))
				.collect(Collectors.toList())
				.forEach(DecoratorNodeOld::stripIfInvalid);
	}

	boolean isSpaceOrLeftBracketish(String characterString) {
		return characterString != null && characterString.matches("[ ({\\[]");
	}

	Function<?, String> itemRenderer() {
		return itemRenderer;
	}

	void onInput0(RelativeInputModel relativeInput) {
		boolean trigger = false;
		if (relativeInput.focusNode().ancestors().has("a")) {
		} else if (hasDecorator(relativeInput.focusNode())) {
		} else {
			String relativeString = relativeInput.relativeString(-1, 0);
			if (Objects.equals(relativeString, triggerSequence)) {
				boolean startOfTextNode = relativeInput.getFocusOffset() == 1;
				String relativeContextLeftString = relativeInput
						.relativeString(-2, -1);
				// relativeContextLeftString null check is probably
				// redundant (since startOfTextNode check will be true
				// in that case)
				// but left for clarity - true means cursor is at [start
				// of contenteditable+1])
				/*
				 * this checks that the surrounding text of the entered trigger
				 * sequence permit decorator insert
				 */
				if (relativeContextLeftString == null || startOfTextNode
						|| isSpaceOrLeftBracketish(relativeContextLeftString)) {
					trigger = true;
				}
			}
		}
		// if triggerable, wrap in the decorator tag (possiby splitting
		// the source text node) and connect the suggestor overlay
		// split
		if (trigger) {
			decorator = new DecoratorNodeOld(this, relativeInput);
			decorator.splitAndWrap();
			showOverlay(decorator.node);
		}
	}

	void showOverlay(DomNode decorator) {
		LocalDom.flush();
		DomNode parent = decorator.parent();
		if (parent.tagIs("font")) {
			// Webkit style-preserving?
			parent.strip();
		}
		Overlay.Builder builder = Overlay.builder();
		Element domElement = (Element) decorator.w3cElement();
		chooser = chooserProvider.apply(this, decorator);
		builder.withCssClass("decorator-chooser");
		Overlay overlay = builder.dropdown(OverlayPosition.Position.START,
				domElement.getBoundingClientRect(), logicalParent, chooser)
				.build();
		overlay.open();
	}

	@Feature.Ref(Feature_Dirndl_ContentDecorator.Constraint_NonSuggesting_DecoratorTag_Selection.class)
	void validateSelection() {
		if (chooser == null) {
			/*
			 * Defer helps handle alt-shift-arrow Keyboard cursor selection -
			 * and also adds a check for chooser creation
			 */
			Scheduler.get().scheduleDeferred(() -> {
				if (chooser == null) {
					RelativeInputModel relativeInput = new RelativeInputModel();
					new DecoratorNodeOld(this, relativeInput)
							.validateSelection();
				}
			});
		}
	}

	public static class Builder<T> {
		Model logicalParent;

		HasDecorator decoratorParent;

		BiFunction<ContentDecorator, DomNode, DecoratorChooser> chooserProvider;

		String triggerSequence;

		String tag;

		Function<T, String> itemRenderer;

		public ContentDecorator build() {
			Preconditions.checkNotNull(logicalParent);
			Preconditions.checkNotNull(decoratorParent);
			Preconditions.checkNotNull(chooserProvider);
			Preconditions.checkNotNull(triggerSequence);
			Preconditions.checkNotNull(tag);
			Preconditions.checkNotNull(itemRenderer);
			return new ContentDecorator(this);
		}

		public void setChooserProvider(
				BiFunction<ContentDecorator, DomNode, DecoratorChooser> chooserProvider) {
			this.chooserProvider = chooserProvider;
		}

		public void setDecoratorParent(HasDecorator decoratorParent) {
			this.decoratorParent = decoratorParent;
		}

		public void setItemRenderer(Function<T, String> itemRenderer) {
			this.itemRenderer = itemRenderer;
		}

		public void setLogicalParent(Model logicalParent) {
			this.logicalParent = logicalParent;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		public void setTriggerSequence(String triggerSequence) {
			this.triggerSequence = triggerSequence;
		}
	}
}
