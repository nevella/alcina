package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;

import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focusout;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.MouseUp;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.model.dom.EditSelection;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ContentDecoratorEvents.NodeDelta;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ContentDecoratorEvents.ReferenceSelected;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorSuggestor.BeforeChooserClosed;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

/**
 * <p>
 * A decorator host (HasDecorators implementor) is a contentEditable model which
 * can be decorated
 *
 *
 *
 */
public interface HasDecorators
		extends DecoratorSuggestor.BeforeChooserClosed.Handler,
		DomEvents.Input.Handler, InferredDomEvents.SelectionChanged.Handler,
		ContentDecoratorEvents.ReferenceSelected.Handler,
		ContentDecoratorEvents.NodeDelta.Handler,
		// routes overlay closed events back to the referencedecorators
		ModelEvents.Closed.Handler,
		// routes (contenteditable) commits to the decorators
		ModelEvents.Commit.Handler,
		// routes keydown events to the keyboardNavigation and decorators
		DomEvents.KeyDown.Handler,
		// routes MouseUp events to decorators
		DomEvents.MouseUp.Handler, KeyboardNavigation.Navigation.Handler,
		FragmentModel.Has, DomEvents.Focusout.Handler {
	/*
	 * Marker attribute
	 */
	@Binding(
		type = Type.PROPERTY,
		to = DecoratorBehavior.ExtendKeyboardNavigationAction.ATTR_NAME)
	default boolean isMagicName() {
		return true;
	}

	/*
	 * Marker attribute
	 */
	@Binding(
		type = Type.PROPERTY,
		to = DecoratorBehavior.ModifyNonEditableSelectionBehaviour.ATTR_NAME)
	default boolean isMagicName2() {
		return true;
	}

	default boolean canDecorate(EditSelection editSelection) {
		DomNode focusNode = editSelection.focusNode();
		FragmentNode fragmentNode = provideFragmentModel()
				.getFragmentNode(focusNode);
		if (focusNode.ancestors().has("a") ||
		// the current node really wants to be a text, this will be null if not.
		// the restriction *might* be to drastic
				editSelection.getTriggerableRangePrecedingFocus() == null) {
			return false;
		} else {
			if (fragmentNode == null
					|| fragmentNode.ancestors().has(DecoratorNode.class)) {
				return false;
			} else {
				return true;
			}
		}
	}

	public List<ContentDecorator> getDecorators();

	default boolean hasActiveDecorator() {
		return getDecorators().stream().anyMatch(ContentDecorator::isActive);
	}

	@Override
	default void onSelectionChanged(InferredDomEvents.SelectionChanged event) {
		getDecorators().forEach(d -> d.onSelectionChanged(event));
		new DecoratorEvent().withType(DecoratorEvent.Type.selection_changed)
				.withMessage(Document.get().getSelection().toString())
				.publish();
	}

	@Override
	default void onChooserClosed(BeforeChooserClosed event) {
		validateDecorators();
	}

	@Override
	default void onClosed(Closed event) {
		getDecorators().forEach(d -> d.onClosed(null));
	}

	@Override
	default void onCommit(Commit event) {
		getDecorators().forEach(d -> d.onCommit(event));
	}

	@Override
	default void onInput(Input event) {
		getDecorators().forEach(d -> d.onInput(event));
	}

	@Override
	default void onKeyDown(KeyDown event) {
		getDecorators().forEach(d -> d.onKeyDown(event));
	}

	@Override
	default void onMouseUp(MouseUp event) {
		getDecorators().forEach(d -> d.onMouseUp(event));
	}

	@Override
	default void onNavigation(Navigation event) {
		getDecorators().forEach(d -> d.onNavigation(event));
	}

	@Override
	default void onReferenceSelected(ReferenceSelected event) {
		getDecorators().forEach(d -> d.onReferenceSelected(event));
	}

	@Override
	default void onNodeDelta(NodeDelta event) {
		getDecorators().forEach(d -> d.onNodeDelta(event));
	}

	@Override
	FragmentModel provideFragmentModel();

	void validateDecorators();

	@Override
	default void onFocusout(Focusout event) {
		getDecorators().forEach(d -> d.onFocusout(event));
	}
}
