package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focusout;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Editor;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.SuggestorEvents.EditorAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.SuggestorEvents.EditorExit;

/**
 * A suggestion editor which suggests based on the contents of a tag in a
 * contenteditable dom subtree
 *
 * It does not directly receive dom events (those are received by the
 * inputcontainer element), instead the events are routed here via the
 * containing contenteditable context
 *
 *
 *
 */
@Directed(emits = { EditorAsk.class, EditorExit.class })
public class TagEditor extends Model
		implements Suggestor.Editor, DomEvents.Input.Handler,
		DomEvents.Focusout.Handler, InferredDomEvents.SelectionChanged.Handler {
	private Element inputContainer;

	boolean attachComplete = false;

	private Function<String, String> askTransformer;

	public TagEditor(Element inputContainer,
			Function<String, String> askTransformer) {
		this.inputContainer = inputContainer;
		this.askTransformer = askTransformer;
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	StringAsk computeAsk() {
		StringAsk ask = new StringAsk();
		ask.setValue(askTransformer.apply(computeTextContent()));
		return ask;
	}

	String computeTextContent() {
		String textContent = inputContainer.getTextContent();
		return textContent;
	}

	@Override
	public void copyInputFrom(Editor editor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void emitAsk() {
		NodeEvent.Context.fromNode(provideNode()).dispatch(EditorAsk.class,
				computeAsk());
	}

	@Override
	public void focus() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onBind(Bind event) {
		Scheduler.get()
				.scheduleDeferred(() -> attachComplete = event.isBound());
		super.onBind(event);
	}

	@Override
	public void onFocusout(Focusout event) {
		if (!attachComplete) {
			return;
		}
	}

	StringAsk lastAsk = null;

	@Override
	public void onInput(DomEvents.Input event) {
		conditionalReemitAsAsk(event);
	}

	void conditionalReemitAsAsk(NodeEvent event) {
		if (!attachComplete) {
			return;
		}
		StringAsk ask = computeAsk();
		if (!Objects.equals(ask, lastAsk)) {
			lastAsk = ask;
			event.reemitAs(this, EditorAsk.class, lastAsk);
		}
	}

	@Override
	public void withSuggestor(Suggestor suggestor) {
	}

	@Override
	public void setFilterText(String filterText) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'setFilterText'");
	}

	@Override
	public boolean hasTriggeringInput() {
		// is not an 'input'
		return false;
	}

	@Override
	public void setAcceptedFilterText(String acceptedFilterText) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'setAcceptedFilterText'");
	}

	@Override
	public void onSelectionChanged(SelectionChanged event) {
		conditionalReemitAsAsk(event);
	}
}