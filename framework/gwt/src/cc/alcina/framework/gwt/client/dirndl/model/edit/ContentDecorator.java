package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.dom.client.Text;
import com.google.gwt.dom.client.behavior.BehaviorRegistry;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeText.SplitResult;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation.Type;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focusout;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.MouseUp;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.dom.EditSelection;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ContentDecoratorEvents.NodeDelta;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ContentDecoratorEvents.ReferenceSelected;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.TextNode;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition;

/**
 * <p>
 * This class supports decoration of a document 'measure' (range). The first
 * uses are '@ mentions' and '# tags'. It's possible that these 'decorations'
 * will always be logically a signifier that the decorated node references
 * something (an entity, a suggestion choice) - so the whole shebang could be
 * named "Reference" - 'ContentReference' etc rather than "Decorator". But
 * "Decorator" is about the _visual_ representation - which is in line with the
 * Dirndl naming convention - 'what it is, not what it does'.
 * <p>
 * The whole process is reasonably complex - and a WIP. Here's a sketch:
 * <ul>
 * <li>User enters some triggering key events - say the '@' key - or a
 * start-of-input if the trigger is empty
 * <li>A {@link InferredDomEvents.SelectionChanged} event interception instructs
 * the decorator to check for editable {@link ContentDecorator} generation (and
 * {@link Suggestor} presentation)
 * <li>SelectionChanged event interception checks if the state is valid for
 * content decoration (particularly that the cursor (DOM selection) is
 * collapsed-ish) - see
 * {@link #onSelectionChanged(InferredDomEvents.SelectionChanged)} for details -
 * and, if checks pass it (the {@link DecoratorNode.Descriptor}):
 * <ul>
 * <li>splits the text node if necessary
 * <li>wraps the trigger (or start-of-input) in a DecoratorNode, e.g.
 * {@code <mention>@</mention>}
 * <li>ensures the selection cursor is after the '@'
 * <li>triggers the overlay display
 * </ul>
 * <li>The overlay
 * <ul>
 * <li>Generally displays a {@link Suggestor}, with the filter input that
 * restricts the suggestions display being the decorator tag contents (in the
 * ContentEditable DOM subtree)
 * <li>Routes up/down/enter/escape keys (cursor/focus in the CE) to the
 * Suggestor
 * </ul>
 * </ul>
 * <p>
 * Other: see the Feature documentation (and constraints/tests)
 * <p>
 * Roles: this class is reponsbile for UI behaviour around the decorator (e.g.
 * if the model is invalid, remove the decorator), the
 * {@code DecoratorNode.Descriptor} is reponsible for domain model behaviour
 * (e.g. is the domain model valid? does the entity exist?)
 * <p>
 * Validation (FIXME - DN)
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
 * <p>
 * Note that BeforeInput support is relatively recent (2021 for FF)
 *
 * overlay hide triggers: trigger tag exit
 *
 * FIXME - minor - if selecting after a mention, make sure the cursor is to the
 * *left* of the ZWS
 *
 * FIXME - less minor - always ensure post deletes that ZWS spaces exist both
 * sides
 */
@Feature.Ref(Feature_Dirndl_ContentDecorator.class)
public class ContentDecorator<T> implements DomEvents.Input.Handler,
		DomEvents.MouseUp.Handler, DomEvents.KeyDown.Handler,
		ContentDecoratorEvents.ReferenceSelected.Handler,
		ContentDecoratorEvents.NodeDelta.Handler,
		KeyboardNavigation.Navigation.Handler, ModelEvents.Closed.Handler,
		ModelEvents.Commit.Handler, InferredDomEvents.SelectionChanged.Handler,
		DomEvents.Focusout.Handler {
	public static class Builder<T> {
		HasDecorators decoratorParent;

		BiFunction<ContentDecorator, DomNode, DecoratorSuggestor> suggestorProvider;

		DecoratorNode.Descriptor<?, ?, ?> descriptor;

		public ContentDecorator build() {
			Preconditions.checkNotNull(decoratorParent);
			Preconditions.checkState(decoratorParent instanceof Model);
			Preconditions.checkNotNull(suggestorProvider);
			Preconditions.checkNotNull(descriptor);
			return new ContentDecorator(this);
		}

		public void setSuggestorProvider(
				BiFunction<ContentDecorator, DomNode, DecoratorSuggestor> suggestorProvider) {
			this.suggestorProvider = suggestorProvider;
		}

		public void setDecoratorParent(HasDecorators decoratorParent) {
			this.decoratorParent = decoratorParent;
		}

		public void
				setDescriptor(DecoratorNode.Descriptor<?, ?, ?> descriptor) {
			this.descriptor = descriptor;
		}
	}

	class CancelledDecoratorSuggestion {
	}

	public static ContentDecorator.Builder builder() {
		return new Builder();
	}

	/*
	 * Used to route (keyboard navigation) events from the HasDecorators -> this
	 * -> the suggestor
	 */
	Topic<Input> topicInput = Topic.create();

	/*
	 * Used to route selection change events
	 */
	Topic<InferredDomEvents.SelectionChanged> topicSelectionChanged = Topic
			.create();

	/*
	 * The suggestor used to edit the current decorator
	 */
	DecoratorSuggestor suggestor;

	BiFunction<ContentDecorator, DomNode, DecoratorSuggestor> suggestorProvider;

	/*
	 * The controller responsible for routing dom events to here, etc
	 */
	HasDecorators decoratorParent;

	/*
	 * The overlay containing the suggestor
	 */
	Overlay overlay;

	/*
	 * Models the characteristics of the decorator - what the trigger key
	 * sequence is etc
	 */
	DecoratorNode.Descriptor<?, ?, ?> descriptor;

	CancelledDecoratorSuggestion cancelledDecoratorSuggestion = new CancelledDecoratorSuggestion();

	/* reference the runnable via a field to ensure distinct() works */
	Runnable checkTrigger0Runnable = this::checkTrigger0;

	Runnable validateSelection0Runnable = this::validateSelection0;

	/*
	 * The SuggestingNode currently being edited
	 */
	SuggestingNode suggestingNode;

	DomNode overlayEditNode;

	/**
	 * Should only be called by the builder
	 * 
	 * @param builder
	 */
	private ContentDecorator(ContentDecorator.Builder builder) {
		/*
		 * AttributeBehaviorHandler registration is required
		 */
		Preconditions.checkState(BehaviorRegistry.isInitialised());
		this.descriptor = builder.descriptor;
		this.suggestorProvider = builder.suggestorProvider;
		this.decoratorParent = builder.decoratorParent;
	}

	public boolean isActive() {
		return suggestor != null;
	}

	@Override
	public void onClosed(Closed event) {
		if (isActive()) {
			suggestor.onClosed(null);
			new DecoratorEvent().withType(DecoratorEvent.Type.overlay_closed)
					.publish();
			suggestor = null;
			overlay = null;
			overlayEditNode = null;
		}
	}

	@Override
	public void onCommit(Commit event) {
		descriptor.onCommit(event);
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
		checkTrigger();
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
		if (suggestor != null) {
			suggestor.suggestor.onNavigation(event);
		}
		// FIXME - ui2 - there's probably a better way to do this. but not
		// super-obvious. Possibly suggestor -> non-overlay results
		if (overlay != null) {
			if (event.getModel() == Type.CANCEL
					|| event.getModel() == Type.COMMIT) {
				closeOverlay();
			}
		}
	}

	@Override
	public void onReferenceSelected(ReferenceSelected event) {
		if (event.getContext().getPrevious().node.getModel() == suggestor) {
			DecoratorNode decorator = descriptor.createNode();
			decorator.putReferenced(event.getModel());
			suggestingNode.nodes().insertAfterThis(decorator);
			suggestingNode.nodes().removeFromParent();
			decorator.positionCursorPostReferencedSelection();
		}
	}

	@Override
	public void onSelectionChanged(InferredDomEvents.SelectionChanged event) {
		checkOverlayValid();
		checkTrigger();
		topicSelectionChanged.publish(event);
	}

	@Override
	public void onNodeDelta(NodeDelta event) {
		refreshOverlayIfShowing();
	}

	@Override
	public void onFocusout(Focusout event) {
		if (overlay != null) {
			closeOverlay();
		}
	}

	protected void validateSelection0() {
		EditSelection selection = new EditSelection();
		if (!selection.hasSelection()) {
			return;
		}
		FragmentModel fragmentModel = decoratorParent.provideFragmentModel();
		List<? extends FragmentNode> list = fragmentModel.stream().toList();
		if (suggestor == null) {
			/*
			 * ensure the selection doesn't contain a partial decoratornode (in
			 * dom terms it's totally fine, but not in FN terms)
			 */
			Optional<DomNode> partiallySelectedAncestor = selection
					.getFocusNodePartiallySelectedAncestor(n -> fragmentModel
							.getFragmentNode(n) instanceof DecoratorNode);
			if (partiallySelectedAncestor.isPresent()) {
				DomNode node = partiallySelectedAncestor.get();
				DecoratorNode decoratorNode = (DecoratorNode) fragmentModel
						.getFragmentNode(node);
				if (!decoratorNode.allowPartialSelection()) {
					// FIXME - FN
					selection.extendSelectionToIncludeAllOf(node);
				}
			}
			/*
			 * Strip any editable DNs (they'll have not been assigned a
			 * referent/representable)
			 */
			fragmentModel.byTypeAssignable(DecoratorNode.class)
					.filter(dn -> dn.contentEditable)
					.forEach(fragmentNode -> fragmentNode.nodes().strip());
		}
	}

	boolean canDecorate(EditSelection relativeInput) {
		return decoratorParent.canDecorate(relativeInput);
	}

	boolean isSpaceOrLeftBracketish(String characterString) {
		return characterString != null
				&& characterString.matches("[ \\u200B({\\[]");
	}

	void checkTrigger() {
		/*
		 * handling requires mutations to be processed (and FN mutations), so
		 * defer
		 * 
		 * FIXME - fn.isolate - check if this causes two sends in romcom. It may
		 * not, if it does, look at an 'after scheduleFinally' queue
		 */
		Client.eventBus().queued().lambda(checkTrigger0Runnable).distinct()
				.deferred().dispatch();
	}

	/*
 * @formatter:off
 * 
 * - populate the EditSelection
 * - if collapsed
 * - if currenttextnode matches:
 * -- preceding text matches trigger
 * -- text prior to trigger is ok punctuation (space; start-of-node; left-paren)
 * - wrap in decorator node
 * - reposition selection (ROMCOM!)
 * - show suggestion-choices
 * 
 * * @formatter:on
 */
	void checkTrigger0() {
		validateSelection0();
		EditSelection editSelection = new EditSelection();
		DomNode focusNode = editSelection.focusNode();
		if (focusNode == null) {
			return;
		}
		FragmentModel fragmentModel = decoratorParent.provideFragmentModel();
		FragmentNode focussedFragment = fragmentModel
				.getFragmentNode(focusNode);
		if (focussedFragment != null) {
			/*
			 * behavior. on cursor validation, strip any suggesting nodes that
			 * are not ancestors of the selection
			 * 
			 * better - strip exactly the source suggesting node on overlay
			 * close
			 */
			fragmentModel.byType(SuggestingNode.class).filter(
					n -> !focussedFragment.ancestors().has(n2 -> n2 == n))
					.forEach(n -> n.nodes().strip());
		}
		if (editSelection.isTriggerable() && suggestor == null) {
			if (focussedFragment != null
					&& focussedFragment.ancestors().has(SuggestingNode.class)) {
				/*
				 * transitional, transforming from accepted suggesing to
				 * decorator
				 */
				return;
			}
			validateSelection0();
			String triggerSequence = null;
			if (!decoratorParent.canDecorate(editSelection)) {
			} else {
				triggerSequence = descriptor.getTriggerSequence(editSelection);
			}
			// if triggerable, wrap in the decorator tag (possiby splitting
			// the source text node) and connect the suggestor overlay
			// split
			if (triggerSequence != null) {
				suggestingNode = createSuggestingNode(editSelection);
				showOverlay(suggestingNode.domNode());
			}
		}
	}

	SuggestingNode createSuggestingNode(EditSelection editSelection) {
		FragmentModel fragmentModel = decoratorParent.provideFragmentModel();
		LocalDom.flushLocalMutations();
		String triggerSequence = descriptor.getTriggerSequence(editSelection);
		SplitResult splits = editSelection.splitAtTriggerRange(triggerSequence);
		DomNode splitContents = splits.contents;
		// may need to flush (to populate FNs) - note for romcom, want to
		// not force remote
		LocalDom.flushLocalMutations();
		TextNode textFragment = (TextNode) fragmentModel
				.getFragmentNode(splitContents);
		FragmentNode parent = textFragment.parent();
		SuggestingNode created = new SuggestingNode(descriptor, textFragment);
		textFragment.nodes().insertBeforeThis(created);
		LocalDom.flush();
		Selection selection = Document.get().getSelection();
		Text text = (Text) splits.contents.w3cNode();
		selection.collapse(text, text.getLength());
		return created;
	}

	void closeOverlay() {
		overlay.close(null, false);
	}

	void refreshOverlayIfShowing() {
		if (overlay != null) {
			overlay.refreshPosition();
			suggestor.refresh();
		}
	}

	void showOverlay(DomNode decoratorDomNode) {
		LocalDom.flush();
		DomNode parent = decoratorDomNode.parent();
		if (parent.tagIs("font")) {
			// Webkit style-preserving?
		}
		Overlay.Attributes attributes = Overlay.attributes();
		Element domElement = (Element) decoratorDomNode.w3cElement();
		suggestor = suggestorProvider.apply(this, decoratorDomNode);
		attributes.withCssClass("decorator-suggestor");
		attributes.withConsumeSubmit(true).withFocusOnBind(false);
		overlay = attributes
				.dropdown(OverlayPosition.Position.START,
						domElement.getBoundingClientRect(),
						(Model) decoratorParent, suggestor)
				.withRectSourceElement(domElement)
				.withPeerModels(List.of(this.suggestingNode)).create();
		new DecoratorEvent().withType(DecoratorEvent.Type.overlay_opened)
				.publish();
		overlay.open();
		this.overlayEditNode = decoratorDomNode;
	}

	@Feature.Ref(Feature_Dirndl_ContentDecorator.Constraint_NonSuggesting_DecoratorTag_Selection.class)
	void validateSelection() {
		if (suggestor == null) {
			/*
			 * Defer helps handle alt-shift-arrow Keyboard cursor selection -
			 * and also adds a check for suggestor creation
			 */
			Client.eventBus().queued().lambda(validateSelection0Runnable)
					.distinct().dispatch();
		}
	}

	void checkOverlayValid() {
		if (overlayEditNode != null) {
			/*
			 * wip - decorator - possibly should be false, but there are
			 * 'spurious' - possibly intermediate - selection events (where the
			 * selection target was removed from dom) that can cause the overlay
			 * to be incorrectly closed.
			 * 
			 * revisit once the editarea state is described with invariants
			 */
			boolean retain = true;
			Selection selection = Document.get().getSelection();
			if (selection.hasSelection()
					&& selection.getAnchorLocation() != null
					&& selection.getFocusLocation() != null) {
				retain = selection.getAnchorLocation().getContainingNode()
						.ancestors().orSelf().has(overlayEditNode)
						&& selection.getFocusLocation().getContainingNode()
								.ancestors().orSelf().has(overlayEditNode);
			}
			if (!retain) {
				closeOverlay();
			}
		}
	}
}
