package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;

import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.MouseUp;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.model.dom.RelativeSelection;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ContentDecoratorEvents.ReferenceSelected;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorChooser.BeforeChooserClosed;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;

/**
 * <p>
 * A decorator host (a contentEditable model which can be decorated)
 *
 *
 *
 */
public interface HasDecorators
		extends DecoratorChooser.BeforeChooserClosed.Handler,
		DomEvents.Input.Handler, DomEvents.SelectionChanged.Handler,
		ContentDecoratorEvents.ReferenceSelected.Handler,
		// routes overlay closed events back to the referencedecorators
		ModelEvents.Closed.Handler,
		// routes (contenteditable) commits to the decorators
		ModelEvents.Commit.Handler,
		// routes keydown events to the keyboardNavigation and decorators
		DomEvents.KeyDown.Handler,
		// routes MouseUp events to decorators
		DomEvents.MouseUp.Handler, KeyboardNavigation.Navigation.Handler,
		FragmentModel.Has {
	boolean canDecorate(RelativeSelection relativeInput);

	public List<ContentDecorator> getDecorators();

	default boolean hasActiveDecorator() {
		return getDecorators().stream().anyMatch(ContentDecorator::isActive);
	}

	@Override
	default void onSelectionChanged(DomEvents.SelectionChanged event) {
		getDecorators().forEach(d -> d.onSelectionChanged(event));
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
	FragmentModel provideFragmentModel();

	void validateDecorators();
}
