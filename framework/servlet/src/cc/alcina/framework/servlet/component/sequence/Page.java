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
import cc.alcina.framework.gwt.client.dirndl.model.component.KeyboardShortcutsArea;
import cc.alcina.framework.servlet.component.sequence.HighlightModel.Match;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser.Ui;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.ClearFilter;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.ColumnSetCycle;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.FocusSearch;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.PropertyDisplayCycle;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.ShowKeyboardShortcuts;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.FilterElements;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.HighlightElements;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.LoadSequence;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.NextSelectable;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.PreviousSelectable;
import cc.alcina.framework.servlet.component.sequence.SequenceSettings.ColumnSet;
import cc.alcina.framework.servlet.component.sequence.SequenceSettings.PropertyDisplayMode;
import cc.alcina.framework.servlet.component.sequence.place.SequencePlace;

/*
 * TODO - look at an approach to prevent double-fires of say reloadSequence -
 * the thing is that two things can trigger that, _both_ will be true on startup
 * 
 * <p>The bindings are quite complex - to try and minimise the redraws - but
 * even so, there is the issue above. Possibly the above, add filters to (say)
 * reloadSequence keyed off some sort of 'context original event'
 */
@TypedProperties
@Directed(
	bindings = @Binding(to = "tabIndex", literal = "0", type = Type.PROPERTY))
class Page extends Model.Fields
		implements SequenceEvents.FilterElements.Handler,
		SequenceEvents.HighlightElements.Handler,
		SequenceEvents.NextSelectable.Handler,
		SequenceEvents.PreviousSelectable.Handler,
		SequenceEvents.LoadSequence.Handler,
		SequenceBrowserCommand.ClearFilter.Handler,
		SequenceBrowserCommand.PropertyDisplayCycle.Handler,
		SequenceBrowserCommand.ColumnSetCycle.Handler,
		SequenceBrowserCommand.FocusSearch.Handler,
		SequenceEvents.HighlightModelChanged.Emitter,
		SequenceEvents.SelectedIndexChanged.Emitter,
		SequenceBrowserCommand.ShowKeyboardShortcuts.Handler {
	class IndexPredicate implements Predicate {
		int index = 0;

		IntPair selectedRange;

		IndexPredicate(IntPair selectedRange) {
			this.selectedRange = selectedRange;
		}

		@Override
		public boolean test(Object o) {
			if (selectedRange == null) {
				return true;
			}
			return selectedRange.contains(index++);
		}
	}

	static PackageProperties._Page properties = PackageProperties.page;

	@Directed
	Header header;

	@Directed
	SequenceArea sequenceArea;

	@Directed
	DetailArea detailArea;

	Sequence<?> sequence;

	List<?> filteredSequenceElements;

	Ui ui;

	HighlightModel highlightModel;

	StyleElement styleElement;

	SequencePlace lastFilterTestPlace = null;

	SequencePlace lastHighlightTestPlace = null;

	SequencePlace lastSelectedIndexChangePlace = null;

	Page() {
		this.ui = Ui.get();
		header = new Header(this);
		bindings().addBindHandler(ui::bindKeyboardShortcuts);
		bindings().from(ui.settings).on(SequenceSettings.properties.sequenceKey)
				.signal(this::reloadSequence);
		bindings().from(ui).on(Ui.properties.place)
				// todo - add ignoreable change filter
				.filter(this::filterUnchangedSequencePlaceChange)
				.signal(this::reloadSequence);
		bindings().from(this).on(properties.sequence)
				.signal(this::computeHighlightModel);
		bindings().from(ui).on(Ui.properties.place)
				// todo - add ignoreable change filter
				.filter(this::filterUnchangedHighlightPlaceChange)
				.signal(this::computeHighlightModel);
		bindings().from(this).on(properties.sequence).value(this).debug()
				.map(SequenceArea::new).to(this).on(properties.sequenceArea)
				.oneWay();
		bindings().from(this).on(properties.sequence).value(this)
				.map(DetailArea::new).to(this).on(properties.detailArea)
				.oneWay();
		bindings().from(ui).on(Ui.properties.place)
				.filter(this::filterSelectedIndexChange)
				.signal(this::onSelectedIndexChange);
		bindings().from(SequenceBrowser.Ui.get().settings)
				.accept(this::updateStyles);
	}

	void onSelectedIndexChange() {
		properties.detailArea.set(this, new DetailArea(this));
		emitEvent(SequenceEvents.SelectedIndexChanged.class);
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

	@Override
	public void onHighlightElements(HighlightElements event) {
		ui.place.copy().withHighlight(event.getModel()).go();
	}

	@Override
	public void onPreviousSelectable(PreviousSelectable event) {
		if (highlightModel.hasMatches()) {
			highlightModel.moveIndex(getSelectedSequenceElement(), -1);
			goToHighlightModelIndex();
		} else {
			Ui.place().copy()
					.deltaSelectedRow(-1, filteredSequenceElements.size()).go();
		}
	}

	@Override
	public void onNextSelectable(NextSelectable event) {
		if (highlightModel.hasMatches()) {
			highlightModel.moveIndex(getSelectedSequenceElement(), +1);
			goToHighlightModelIndex();
		} else {
			Ui.place().copy()
					.deltaSelectedRow(1, filteredSequenceElements.size()).go();
		}
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
		if (selectedElementIdx == -1
				|| selectedElementIdx > filteredSequenceElements.size() - 1) {
			return null;
		} else {
			return filteredSequenceElements.get(selectedElementIdx);
		}
	}

	/*
	 * The sequence will be changed if the filter is changed (essentially)
	 */
	boolean filterUnchangedSequencePlaceChange(SequencePlace place) {
		boolean result = place.hasFilterChange(lastFilterTestPlace);
		lastFilterTestPlace = place;
		return result;
	}

	/*
	 * Test for unchanged highlight model
	 */
	boolean filterUnchangedHighlightPlaceChange(SequencePlace place) {
		boolean result = place.hasHighlightChange(lastHighlightTestPlace);
		lastHighlightTestPlace = place;
		return result;
	}

	/*
	 * Test for unchanged selected index
	 */
	boolean filterSelectedIndexChange(SequencePlace place) {
		boolean result = place
				.hasSelectedIndexChange(lastSelectedIndexChangePlace);
		lastSelectedIndexChangePlace = place;
		return result;
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
		emitEvent(SequenceEvents.HighlightModelChanged.class);
	}

	//
	void reloadSequence() {
		String sequenceKey = Ax.blankToEmpty(ui.settings.sequenceKey);
		Sequence.Loader loader = Sequence.Loader.getLoader(sequenceKey);
		Sequence<?> sequence = loader.load(sequenceKey);
		filteredSequenceElements = filteredSequenceElements(sequence);
		properties.sequence.set(this, sequence);
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
			case FULL_WIDTH:
				rows.add("props props props props");
				builder.line("body > page > sequence{display: none;}");
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

	void goToHighlightModelIndex() {
		if (!highlightModel.hasMatches()) {
			return;
		}
		Object highlightedSequenceElement = highlightModel
				.getHighlightedElement();
		ui.place.copy().withHighlightIndicies(highlightModel.highlightIndex,
				filteredSequenceElements.indexOf(highlightedSequenceElement))
				.go();
	}

	@Override
	public void onColumnSetCycle(ColumnSetCycle event) {
		SequenceSettings settings = SequenceBrowser.Ui.get().settings;
		ColumnSet next = settings.nextColumnSet();
		StatusModule.get()
				.showMessageTransitional(Ax.format("Column set -> %s", next));
		reloadSequence();
	}

	@Override
	public void onShowKeyboardShortcuts(ShowKeyboardShortcuts event) {
		KeyboardShortcutsArea
				.show(SequenceBrowser.Ui.get().getKeybindingsHandler());
	}
}
