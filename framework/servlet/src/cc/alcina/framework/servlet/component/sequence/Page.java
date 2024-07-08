package cc.alcina.framework.servlet.component.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Text;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasFilterableText;
import cc.alcina.framework.common.client.util.HasFilterableText.Query;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.sequence.HighlightModel.Match;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser.Ui;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.ClearFilter;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.FocusSearch;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.PropertyDisplayCycle;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.FilterElements;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.HighlightElements;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.LoadSequence;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.NextHighlight;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.PreviousHighlight;
import cc.alcina.framework.servlet.component.sequence.SequenceSettings.PropertyDisplayMode;
import cc.alcina.framework.servlet.component.sequence.place.SequencePlace;

@TypedProperties
@Directed(
	bindings = @Binding(to = "tabIndex", literal = "0", type = Type.PROPERTY))
class Page extends Model.All implements SequenceEvents.FilterElements.Handler,
		SequenceEvents.HighlightElements.Handler,
		SequenceEvents.NextHighlight.Handler,
		SequenceEvents.PreviousHighlight.Handler,
		SequenceEvents.LoadSequence.Handler,
		SequenceBrowserCommand.ClearFilter.Handler,
		SequenceBrowserCommand.PropertyDisplayCycle.Handler,
		SequenceBrowserCommand.FocusSearch.Handler {
	static PackageProperties._Page properties = PackageProperties.page;

	Header header;

	SequenceArea sequenceArea;

	DetailArea detailArea;

	@Directed.Exclude
	Sequence<?> sequence;

	@Property.Not
	List<?> filteredSequenceElements;

	@Property.Not
	Ui ui;

	@Property.Not
	HighlightModel highlightModel;

	private StyleElement styleElement;

	Page() {
		this.ui = Ui.get();
		header = new Header(this);
		bindings().addBindHandler(ui::bindKeyboardShortcuts);
		bindings().from(ui.settings).on(SequenceSettings.properties.sequenceKey)
				.signal(this::reloadSequence);
		bindings().from(ui).on(Ui.properties.place)
				// todo - add ignoreable change filter
				.signal(this::reloadSequence);
		bindings().from(this).on(properties.sequence)
				.signal(this::computeHighlightModel);
		bindings().from(this).on(properties.sequence).value(this)
				.map(SequenceArea::new).to(this).on(properties.sequenceArea)
				.oneWay();
		bindings().from(this).on(properties.sequence).value(this)
				.map(DetailArea::new).to(this).on(properties.detailArea)
				.oneWay();
		bindings().from(ui).on(Ui.properties.place).value(this)
				.map(DetailArea::new).to(this).on(properties.detailArea)
				.oneWay();
		bindings().from(SequenceBrowser.Ui.get().settings)
				.accept(this::updateStyles);
	}

	void computeHighlightModel() {
		highlightModel = new HighlightModel(filteredSequenceElements,
				(Function) sequence.getDetailTransform(), ui.place.highlight,
				ui.place.highlightIdx);
		highlightModel.computeMatches();
		if (highlightModel.hasMatches()
				&& highlightModel.highlightIndex == -1) {
			highlightModel.goTo(0);
			goToHighlightModelIndex();
		}
	}

	void reloadSequence() {
		String sequenceKey = Ax.blankToEmpty(ui.settings.sequenceKey);
		Sequence.Loader loader = Sequence.Loader.getLoader(sequenceKey);
		Sequence<?> sequence = loader.load(sequenceKey);
		filteredSequenceElements = filteredSequenceElements(sequence);
		properties.sequence.set(this, sequence);
	}

	@Override
	public void onClearFilter(ClearFilter event) {
		header.mid.suggestor.clear();
		new SequencePlace().go();
	}

	@Override
	public void onPropertyDisplayCycle(PropertyDisplayCycle event) {
		SequenceSettings settings = SequenceBrowser.Ui.get().settings;
		PropertyDisplayMode next = settings.nextPropertyDisplayMode();
		StatusModule.get().showMessageTransitional(
				Ax.format("Property display mode -> %s", next));
	}

	@Override
	public void onFocusSearch(FocusSearch event) {
		header.mid.suggestor.focus();
	}

	@Override
	public void onFilterElements(FilterElements event) {
		new SequencePlace().withFilter(event.getModel()).go();
	}

	@Override
	public void onLoadSequence(LoadSequence event) {
		SequenceSettings.properties.sequenceKey.set(ui.settings,
				event.getModel());
	}

	void updateStyles(SequenceSettings settings) {
		FormatBuilder builder = new FormatBuilder();
		{
			/*
			 * body > page grid-template-areas - default:
			 * "header header header header" "sequence sequence sequence props"
			 * "input input output output";
			 */
			List<String> rows = new ArrayList<>();
			rows.add("header header header header");
			switch (settings.propertyDisplayMode) {
			case QUARTER_WIDTH:
				rows.add("sequence sequence sequence props");
				break;
			case HALF_WIDTH:
				rows.add("sequence sequence props props");
				break;
			case NONE:
				rows.add("sequence sequence sequence sequence");
				builder.line("body > page > properties{display: none;}");
				break;
			default:
				throw new UnsupportedOperationException();
			}
			builder.line("body > page {grid-template-rows: 50px 1fr}");
			//
			String areas = rows.stream().map(s -> Ax.format("\"%s\"", s))
					.collect(Collectors.joining(" "));
			builder.line("body > page {grid-template-areas: %s;}", areas);
		}
		String text = builder.toString();
		if (styleElement == null) {
			styleElement = StyleInjector.createAndAttachElement(text);
		} else {
			((Text) styleElement.getChild(0)).setTextContent(text);
		}
	}

	List<?> filteredSequenceElements(Sequence sequence) {
		/*
		 * because filtering works better on the transformed elts, transform to
		 * test - but the elements of SequenceArea.filteredElements are the
		 * original sequence elements.
		 * 
		 * There's a double-transform cost there, but it preserves the dirndl
		 * way of 'delay transformation til yr at the edge', and makes
		 * back-propagation (e.g. what event was selected?) easier
		 */
		Query<Model> query = HasFilterableText.Query.of(ui.place.filter)
				.withCaseInsensitive(true).withRegex(true);
		ModelTransform sequenceRowTransform = sequence.getRowTransform();
		List<?> filteredElements = (List<?>) sequence.getElements().stream()
				.filter(new IndexPredicate(ui.place.selectedRange))
				.filter(e -> query.test(sequenceRowTransform.apply(e)))
				.collect(Collectors.toList());
		return filteredElements;
	}

	class IndexPredicate implements Predicate {
		int index = 0;

		IntPair selectedRange;

		IndexPredicate(IntPair selectedRange) {
			this.selectedRange = selectedRange;
		}

		public boolean test(Object o) {
			if (selectedRange == null) {
				return true;
			}
			return selectedRange.contains(index++);
		}
	}

	@Override
	public void onHighlightElements(HighlightElements event) {
		ui.place.copy().withHighlight(event.getModel()).go();
	}

	@Override
	public void onPreviousHighlight(PreviousHighlight event) {
		highlightModel.move(-1);
		goToHighlightModelIndex();
	}

	void goToHighlightModelIndex() {
		if (!highlightModel.hasMatches()) {
			return;
		}
		Object highlightedSequenceElement = highlightModel
				.getHighlightedElement();
		ui.place.copy()
				.withHighlightIndicies(highlightModel.highlightIndex, sequence
						.getElements().indexOf(highlightedSequenceElement))
				.go();
	}

	@Override
	public void onNextHighlight(NextHighlight event) {
		highlightModel.move(1);
		goToHighlightModelIndex();
	}

	@Property.Not
	public List<IntPair> getSelectedElementHighlights() {
		return highlightModel.elementMatches
				.getAndEnsure(getSelectedSequenceElement()).stream()
				.map(m -> m.range).collect(Collectors.toList());
	}

	@Property.Not
	public int getSelectedElementHighlightIndex() {
		Match match = highlightModel.getMatch(ui.place.highlightIdx);
		return match == null ? -1 : match.getIndexInSelectedElementMatches();
	}

	@Property.Not
	public Object getSelectedSequenceElement() {
		int selectedElementIdx = ui.place.selectedElementIdx;
		if (selectedElementIdx == -1) {
			return null;
		} else {
			return sequence.getElements().get(selectedElementIdx);
		}
	}
}
