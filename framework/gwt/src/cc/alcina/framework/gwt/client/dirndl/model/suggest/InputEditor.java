package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.SuggestorEvents.EditorAsk;

/**
 * <p>
 * A suggestion editor which suggests based on the contents of an input tag
 *
 * <p>
 * If enabled (and it is by default), this editor it also routes keyboard
 * navigation events to the suggestor
 *
 * @author nick@alcina.cc
 *
 */
@Directed(
	receives = { ModelEvents.Input.class, DomEvents.KeyDown.class,
			KeyboardNavigation.Navigation.class },
	emits = EditorAsk.class)
public class InputEditor extends Model
		implements Suggestor.Editor, ModelEvents.Input.Handler,
		// routes keydown events to the keyboardNavigation and
		DomEvents.KeyDown.Handler,
		// routes keyboardNavigation events to suggestor
		KeyboardNavigation.Navigation.Handler {
	StringInput input;

	KeyboardNavigation keyboardNavigation;

	Suggestor suggestor;

	@Override
	public void clear() {
		input.clear();
	}

	@Override
	public void emitAsk() {
		NodeEvent.Context.fromNode(provideNode()).dispatch(EditorAsk.class,
				computeAsk());
	}

	@Override
	public void focus() {
		input.focus();
	}

	@Directed
	public StringInput getInput() {
		return this.input;
	}

	@Override
	public void onInput(ModelEvents.Input event) {
		event.reemitAs(this, EditorAsk.class, computeAsk());
	}

	@Override
	public void onKeyDown(KeyDown event) {
		keyboardNavigation.onKeyDown(event);
	}

	@Override
	public void onNavigation(Navigation event) {
		suggestor.onNavigation(event);
	}

	@Override
	public void withSuggestor(Suggestor suggestor) {
		this.suggestor = suggestor;
		input = new StringInput();
		Suggestor.Builder builder = suggestor.getBuilder();
		input.setPlaceholder(builder.getInputPrompt());
		input.setFocusOnBind(builder.isFocusOnBind());
		input.setSelectAllOnBind(builder.isSelectAllOnBind());
		if (builder.isInputEditorKeyboardNavigationEnabled()) {
			keyboardNavigation = new KeyboardNavigation(this);
		}
	}

	private StringAsk computeAsk() {
		StringAsk ask = new StringAsk();
		ask.setValue(input.getCurrentValue());
		return ask;
	}
}