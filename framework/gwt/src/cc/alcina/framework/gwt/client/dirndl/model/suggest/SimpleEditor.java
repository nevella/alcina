package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.SuggestorEvents.EditorAsk;

@Directed(receives = DomEvents.Input.class, emits = EditorAsk.class)
public class SimpleEditor extends Model.WithNode
		implements Suggestor.Editor, DomEvents.Input.Handler {
	private final StringInput input;

	public SimpleEditor(Suggestor.Configuration configuration) {
		input = new StringInput();
		input.setPlaceholder(configuration.getInputPrompt());
		input.setFocusOnBind(configuration.isFocusOnBind());
		input.setSelectAllOnBind(configuration.isSelectAllOnBind());
	}

	@Override
	public void emitAsk() {
		NodeEvent.Context.newNodeContext(node).fire(EditorAsk.class,
				computeAsk());
	}

	@Directed
	public StringInput getInput() {
		return this.input;
	}

	@Override
	public void onInput(Input event) {
		event.reemitAs(EditorAsk.class, computeAsk());
	}

	private StringAsk computeAsk() {
		StringAsk ask = new StringAsk();
		ask.setValue(input.getCurrentValue());
		return ask;
	}
}