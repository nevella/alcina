package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.SuggestorEvents.EditorAsk;

/**
 * A suggestion editor which suggests based on the contents of an input tag
 *
 * @author nick@alcina.cc
 *
 */
@Directed(receives = ModelEvents.Input.class, emits = EditorAsk.class)
public class InputEditor extends Model
		implements Suggestor.Editor, ModelEvents.Input.Handler {
	private StringInput input;

	@Override
	public void emitAsk() {
		NodeEvent.Context.fromNode(provideNode()).dispatch(EditorAsk.class,
				computeAsk());
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
	public void withBuilder(Suggestor.Builder builder) {
		input = new StringInput();
		input.setPlaceholder(builder.getInputPrompt());
		input.setFocusOnBind(builder.isFocusOnBind());
		input.setSelectAllOnBind(builder.isSelectAllOnBind());
	}

	private StringAsk computeAsk() {
		StringAsk ask = new StringAsk();
		ask.setValue(input.getCurrentValue());
		return ask;
	}
}