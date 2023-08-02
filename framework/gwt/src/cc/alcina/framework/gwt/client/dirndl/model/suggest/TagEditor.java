package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focusout;
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
public class TagEditor extends Model implements Suggestor.Editor,
		DomEvents.Input.Handler, DomEvents.Focusout.Handler {
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

	@Override
	public void onInput(DomEvents.Input event) {
		if (!attachComplete) {
			return;
		}
		Ax.err("oninput: %s", event.getValue());
		event.reemitAs(this, EditorAsk.class, computeAsk());
	}

	@Override
	public void withSuggestor(Suggestor suggestor) {
	}

	private StringAsk computeAsk() {
		StringAsk ask = new StringAsk();
		ask.setValue(askTransformer.apply(computeTextContent()));
		return ask;
	}

	private String computeTextContent() {
		String textContent = inputContainer.streamChildren()
				.filter(n -> n.getNodeType() == Node.TEXT_NODE)
				.map(Node::getTextContent).collect(Collectors.joining());
		return textContent;
	}
}