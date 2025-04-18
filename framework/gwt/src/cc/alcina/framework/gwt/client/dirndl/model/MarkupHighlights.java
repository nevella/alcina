package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

/**
 * <p>
 * Renders some DOM with highlights + scroll to
 * <ul>
 * <li>Initializer can take plain text (it'll be wrapped in a pre)
 * <li>If the markup is invalid (so when setInnerHtml is called in the browser,
 * the resultant DOM structure != the local structure), you'll need to fix the
 * incoming markup, there's no way to be consistent in the presence of those
 * sort of errors
 * </ul>
 */
@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class MarkupHighlights extends Model.Fields {
	private static final String MARKUP_HIGHLIGHT = "__markup_highlight";

	private static final String SELECTED_MARKUP_HIGHLIGHT = "__selected_markup_highlight";

	@Directed
	HighlitMarkup highlitMarkup;

	List<WrappedRange> wrappedRanges = new ArrayList<>();

	WrappedRange selected;

	List<IntPair> numericRanges;

	int goToRangeIndex;

	public MarkupHighlights(String content, String classNames, boolean markup,
			List<IntPair> numericRanges, int goToRangeIndex) {
		putRanges(numericRanges, goToRangeIndex);
		Document.get().asDomNode().document.setReadonly(true);
		if (markup) {
		} else {
			DomDocument doc = DomDocument.from("<pre><code/></pre>");
			doc.getDocumentElementNode().children.firstElement()
					.setText(content);
			content = doc.fullToString();
		}
		highlitMarkup = new HighlitMarkup(content, classNames);
	}

	public static class MarkupClick
			extends ModelEvent<Object, MarkupClick.Handler> {
		@Override
		public void dispatch(MarkupClick.Handler handler) {
			handler.onMarkupClick(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onMarkupClick(MarkupClick event);
		}
	}

	@Directed(tag = "div")
	class HighlitMarkup extends Model.All implements DomEvents.Click.Handler {
		@Binding(type = Type.INNER_HTML)
		String markup;

		@Binding(type = Type.CLASS_PROPERTY)
		String classNames;

		HighlitMarkup(String markup, String classNames) {
			this.markup = markup;
			this.classNames = classNames;
		}

		@Override
		public void onClick(Click event) {
			event.reemitAs(this, MarkupClick.class);
		}
	}

	public void putRanges(List<IntPair> numericRanges, int goToRangeIndex) {
		this.numericRanges = numericRanges;
		this.goToRangeIndex = goToRangeIndex;
	}

	public void wrapRangesAndGo() {
		Client.eventBus().queued().deferred()
				.lambda(this::wrapRangesAndGoDeferred).dispatch();
	}

	void wrapRangesAndGoDeferred() {
		LocalDom.flush();
		wrappedRanges.forEach(WrappedRange::unwrap);
		wrappedRanges = numericRanges.stream().map(WrappedRange::new)
				.collect(Collectors.toList());
		wrappedRanges.forEach(WrappedRange::wrap);
		goToRange(goToRangeIndex);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			wrapRangesAndGo();
		}
	}

	class HighlightedRange {
		int idx;
	}

	class WrappedRange {
		IntPair numericRange;

		List<DomNode> wrapped = new ArrayList<>();

		WrappedRange(IntPair range) {
			this.numericRange = range;
		}

		void unwrap() {
			// wrapped will be span/text structures. This merges split texts
			// (preserving a localdom requirement)
			List<DomNode> texts = wrapped.stream().map(n -> n.strip())
					.collect(Collectors.toList());
			for (int idx = texts.size() - 1; idx > 0; idx--) {
				DomNode node = texts.get(idx);
				DomNode previous = texts.get(idx - 1);
				if (node.relative().previousSibling() == previous) {
					previous.setText(
							previous.textContent() + node.textContent());
					node.removeFromParent();
				}
			}
		}

		void wrap() {
			if (!highlitMarkup.provideIsBound()) {
				return;
			}
			DomNode domNode = highlitMarkup.provideElement().asDomNode();
			// not strictly true, but mutation invalidation is handled manually
			domNode.document.invalidateLocations();
			Range range = domNode.asRange();
			// split the boundary text nodes
			Range truncated = range.truncateRelative(numericRange.i1,
					numericRange.i2);
			truncated.start.toTextLocation(true).ensureAtBoundary();
			range = domNode.asRange();
			truncated = range.truncateRelative(numericRange.i1,
					numericRange.i2);
			domNode.document.invalidateLocations();
			range = domNode.asRange();
			truncated.end.toTextLocation(true).ensureAtBoundary();
			domNode.document.invalidateLocations();
			List<DomNode> wrap = domNode.stream().filter(n -> {
				int index = n.asDomNode().asLocation().getIndex()
						- domNode.asLocation().getIndex();
				return n.isText() && index >= numericRange.i1
						&& index < numericRange.i2;
			}).filter(t ->
			// FIXME - hard-coded - this should go in the element, poss
			// derived from html spec?
			!t.parent().tagIsOneOf("table", "thead", "tbody", "tr", "tfoot"))
					.collect(Collectors.toList());
			wrapped = wrap.stream()
					.map(t -> t.builder().tag("span")
							.className(MARKUP_HIGHLIGHT).wrap())
					.collect(Collectors.toList());
		}

		void scrollTo() {
			if (wrapped.isEmpty()) {
				return;
			}
			Element gwtElement = wrapped.get(0).gwtElement();
			// FIXME - this is to workaround out-of-order client processing -
			// revisit with romcom.trans
			Timer.Provider.get()
					.getTimer(() -> gwtElement.scrollIntoView(0, 75))
					.schedule(100);
		}

		void setSelected(boolean selected) {
			wrapped.forEach(node -> node.css()
					.putClass(SELECTED_MARKUP_HIGHLIGHT, selected));
		}
	}

	public void goToRange(int rangeIdx) {
		if (provideIsUnbound() || rangeIdx < 0
				|| rangeIdx >= wrappedRanges.size()) {
			return;
		}
		if (selected != null) {
			selected.setSelected(false);
		}
		selected = wrappedRanges.get(rangeIdx);
		selected.scrollTo();
		selected.setSelected(true);
	}

	public void updateRanges(List<IntPair> numericRanges) {
		if (Objects.equals(numericRanges, this.numericRanges)) {
			return;
		} else {
			putRanges(numericRanges, 0);
			wrapRangesAndGo();
		}
	}
}
