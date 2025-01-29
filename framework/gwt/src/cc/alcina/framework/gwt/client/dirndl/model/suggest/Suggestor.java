package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.model.HasSelectedValue;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Suggestion.Markup;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Suggestions.State;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.SuggestorEvents.EditorAsk;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;

/**
 * <p>
 * This class mostly acts to coordinate interactions between the {@code Input}
 * and the {@code Suggestions} children - including routing keyboard events.
 *
 * <p>
 * TODO - there's probably a better general way of handling the perennial "is
 * {@code value} a collection of T or a T" question
 *
 * <p>
 * FIXME - dirndl 1x1dz - switch SuggestorConfiguration to a builder
 *
 *
 *
 */
@Directed(emits = ModelEvents.SelectionChanged.class)
@TypedProperties
public class Suggestor extends Model implements
		SuggestorEvents.EditorAsk.Handler, ModelEvents.SelectionChanged.Handler,
		HasSelectedValue, KeyboardNavigation.Navigation.Handler,
		ModelEvents.Closed.Handler, Model.TransmitState {
	public static PackageProperties._Suggestor properties = PackageProperties.suggestor;

	// FIXME - dirndl 1x1e - add a default impl, which routes via a Debounce
	// (which doesn't send if inflight, but has a timeout)
	public interface Answer<A extends Ask> {
		public void ask(A ask, Consumer<Answers> answersHandler,
				Consumer<Throwable> exceptionHandler);
	}

	public static class Answers {
		private List<Suggestion> suggestions = new ArrayList<>();

		private int total;

		public void add(Suggestion suggestion, IntPair resultRange) {
			if (resultRange == null || resultRange.contains(total)) {
				suggestions.add(suggestion);
			}
			total++;
		}

		public Markup addCreateNewSuggestion(String text) {
			Markup suggestion = new Markup();
			suggestion.setMarkup(SafeHtmlUtils.htmlEscape(text));
			suggestions.add(0, suggestion);
			total++;
			return suggestion;
		}

		public boolean containsExactMatch(String value) {
			return suggestions.stream()
					.anyMatch(s -> Objects.equals(s.toString(), value));
		}

		public List<Suggestion> getSuggestions() {
			return this.suggestions;
		}

		public int getTotal() {
			return this.total;
		}

		public void setSuggestions(List<Suggestion> suggestions) {
			this.suggestions = suggestions;
		}

		public void setTotal(int total) {
			this.total = total;
		}

		@Override
		public String toString() {
			FormatBuilder format = new FormatBuilder();
			format.appendKeyValues("total", total);
			format.newLine();
			format.indent(2);
			format.elementLines(suggestions);
			return format.toString();
		}
	}

	/**
	 * <p>
	 * So named because 'query' is so tired
	 *
	 *
	 *
	 */
	public interface Ask {
		IntPair getResultRange();

		boolean isEmpty();
	}

	public enum SuggestOnBind {
		NO, YES, NON_EMPTY_VALUE
	}

	public static class Attributes {
		String inputPrompt;

		List<Class<? extends Model>> logicalAncestors = List.of();

		boolean focusOnBind;

		boolean selectAllOnFocus;

		Answer<?> answer;

		OverlayPosition.Position suggestionXAlign = Position.START;

		SuggestOnBind suggestOnBind = SuggestOnBind.NO;

		Supplier<? extends Editor> editorSupplier = InputEditor::new;

		boolean inputEditorKeyboardNavigationEnabled = true;

		boolean nonOverlaySuggestionResults;

		/**
		 * Delay before clearing past suggestions and showing a spinner - ms
		 */
		int showSpinnerDelay = 1000;

		String inputText;

		String inputTag;

		boolean inputExpandable;

		boolean closeSuggestionsOnEmptyAsk;

		public String getInputTag() {
			return inputTag;
		}

		public boolean isInputExpandable() {
			return inputExpandable;
		}

		public String getInputText() {
			return inputText;
		}

		public Suggestor create() {
			return new Suggestor(this);
		}

		public Answer getAnswer() {
			return this.answer;
		}

		public String getInputPrompt() {
			return this.inputPrompt;
		}

		public List<Class<? extends Model>> getLogicalAncestors() {
			return this.logicalAncestors;
		}

		public OverlayPosition.Position getSuggestionXAlign() {
			return this.suggestionXAlign;
		}

		public boolean isFocusOnBind() {
			return this.focusOnBind;
		}

		public boolean isInputEditorKeyboardNavigationEnabled() {
			return this.inputEditorKeyboardNavigationEnabled;
		}

		public boolean isNonOverlaySuggestionResults() {
			return this.nonOverlaySuggestionResults;
		}

		public boolean isSelectAllOnFocus() {
			return this.selectAllOnFocus;
		}

		public SuggestOnBind getSuggestOnBind() {
			return this.suggestOnBind;
		}

		public Attributes withAnswer(Answer answer) {
			this.answer = answer;
			return this;
		}

		public Attributes
				withEditorSupplier(Supplier<? extends Editor> editorSupplier) {
			this.editorSupplier = editorSupplier;
			return this;
		}

		public Attributes withFocusOnBind(boolean focusOnBind) {
			this.focusOnBind = focusOnBind;
			return this;
		}

		public Attributes withCloseSuggestionsOnEmptyAsk(
				boolean closeSuggestionsOnEmptyAsk) {
			this.closeSuggestionsOnEmptyAsk = closeSuggestionsOnEmptyAsk;
			return this;
		}

		public Attributes withInputEditorKeyboardNavigationEnabled(
				boolean inputEditorKeyboardNavigationEnabled) {
			this.inputEditorKeyboardNavigationEnabled = inputEditorKeyboardNavigationEnabled;
			return this;
		}

		public Attributes withInputPrompt(String inputPrompt) {
			this.inputPrompt = inputPrompt;
			return this;
		}

		public Attributes withInputTag(String inputTag) {
			this.inputTag = inputTag;
			return this;
		}

		public Attributes withInputExpandable(boolean inputExpandable) {
			this.inputExpandable = inputExpandable;
			return this;
		}

		public Attributes withInputText(String inputText) {
			this.inputText = inputText;
			return this;
		}

		public Attributes withLogicalAncestors(
				List<Class<? extends Model>> logicalAncestors) {
			this.logicalAncestors = logicalAncestors;
			return this;
		}

		public Attributes withNonOverlaySuggestionResults(
				boolean nonOverlaySuggestionResults) {
			this.nonOverlaySuggestionResults = nonOverlaySuggestionResults;
			return this;
		}

		public Attributes withSelectAllOnFocus(boolean selectAllOnFocus) {
			this.selectAllOnFocus = selectAllOnFocus;
			return this;
		}

		public Attributes withShowSpinnerDelay(int showSpinnerDelay) {
			this.showSpinnerDelay = showSpinnerDelay;
			return this;
		}

		public Attributes withSuggestionXAlign(
				OverlayPosition.Position suggestionXAlign) {
			this.suggestionXAlign = suggestionXAlign;
			return this;
		}

		public Attributes withSuggestOnBind(SuggestOnBind suggestOnBind) {
			this.suggestOnBind = suggestOnBind;
			return this;
		}
	}

	/*
	 * Should emit an editorask event on dom input (possibly debounced)
	 *
	 * FIXME - dirndl 1x3 - not sure if @Directed is interface
	 * inheritable/mergeable, but it should be
	 */
	@Directed(emits = EditorAsk.class)
	public interface Editor {
		void clear();

		void copyInputFrom(Editor editor);

		void emitAsk();

		void focus();

		void withSuggestor(Suggestor suggestor);

		void setFilterText(String filterText);

		boolean hasNonEmptyInput();
	}

	public static class StringAsk implements Ask {
		private String value;

		private IntPair resultRange;

		@Override
		public IntPair getResultRange() {
			return this.resultRange;
		}

		public String getValue() {
			return this.value;
		}

		public void setResultRange(IntPair resultRange) {
			this.resultRange = resultRange;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("value", value, "resultRange",
					resultRange);
		}

		@Property.Not
		@Override
		public boolean isEmpty() {
			return Ax.isBlank(getValue());
		}
	}

	/*
	 * Marker for the payload of a Suggestion
	 */
	public interface Suggestion {
		@Directed(
			tag = "suggestion",
			bindings = @Binding(from = "markup", type = Type.INNER_HTML))
		public static class Markup extends Bindable implements Suggestion {
			private String markup;

			private Object model;

			private boolean match;

			public Markup() {
			}

			public String getMarkup() {
				return this.markup;
			}

			@Override
			public Object getModel() {
				return this.model;
			}

			@Override
			public boolean isMatch() {
				return this.match;
			}

			public void setMarkup(String markup) {
				this.markup = markup;
			}

			public void setMatch(boolean match) {
				this.match = match;
			}

			public void setModel(Object model) {
				this.model = model;
			}

			@Override
			public String toString() {
				return FormatBuilder.keyValues("markup", markup);
			}
		}

		@Directed(tag = "suggestion")
		@TypeSerialization(reflectiveSerializable = false)
		public static class ModelSuggestion extends Model.Fields
				implements Suggestion {
			public Object model;

			public boolean match;

			public ModelSuggestion(Object model) {
				this.model = model;
			}

			@Override
			@Directed
			public Object getModel() {
				return model;
			}

			@Override
			public boolean isMatch() {
				return match;
			}

			@Override
			public String toString() {
				return model.toString();
			}
		}

		/*
		 * Typed model (for rendering, either this or markup)
		 *
		 * FIXME - dirndl 1x1g - reflection - rather, specify a reflective hook
		 * to define what is a valid model here (for refl. ser) - e.g.
		 * {IGroup.class, IUser.class}. These general reflective reachability
		 * questions (and there aren't many - say this and 'what is a valid
		 * SearchResult row'?) are *not* necessarily answered best by
		 * annotations, since once of the answers might be...String.class. But
		 * this'll do for a bit
		 */
		// FIXME - today - check reachability against this
		Object getModel();

		/*
		 * Is the suggestion keyboard/mouse selectable? Or a guide?
		 */
		boolean isMatch();
	}

	public interface SuggestionModel {
	}

	@Directed(emits = ModelEvents.SelectionChanged.class)
	public interface Suggestions
			extends HasSelectedValue, ModelEvents.Closed.Handler {
		public static enum State {
			LOADING, LOADED, EXCEPTION, UNBOUND, CLOSED
		}

		void close();

		void onAnswers(Answers answers);

		default void onAskException(Throwable throwsable) {
			throw WrappedRuntimeException.wrap(throwsable);
		}

		void toState(State state);
	}

	public static Attributes attributes() {
		return new Attributes();
	}

	protected Editor editor;

	protected Suggestions suggestions;

	Object nonOverlaySuggestionResults;

	Attributes attributes;

	private Object value;

	private Suggestor(Attributes attributes) {
		this.attributes = attributes;
		initFields();
	}

	public void clear() {
		editor.clear();
	}

	public void closeSuggestions() {
		suggestions.close();
	}

	public void copyEditorInputFrom(Suggestor suggestor) {
		editor.copyInputFrom(suggestor.editor);
	}

	public void focus() {
		editor.focus();
	}

	public Attributes getAttributes() {
		return this.attributes;
	}

	@Directed(tag = "editor")
	public Editor getEditor() {
		return this.editor;
	}

	@Directed
	public Object getNonOverlaySuggestionResults() {
		return this.nonOverlaySuggestionResults;
	}

	public Suggestions getSuggestions() {
		return this.suggestions;
	}

	/**
	 * The suggested value(s)
	 */
	public Object getValue() {
		return this.value;
	}

	@Override
	public void onBind(LayoutEvents.Bind event) {
		if (!event.isBound()) {
			suggestions.toState(State.UNBOUND);
		} else {
			boolean suggest = false;
			switch (attributes.getSuggestOnBind()) {
			case YES:
				suggest = true;
				break;
			case NON_EMPTY_VALUE:
				suggest = editor.hasNonEmptyInput();
				break;
			}
			if (suggest) {
				Client.eventBus().queued().lambda(() -> editor.emitAsk())
						.dispatch();
			}
		}
		super.onBind(event);
	}

	@Override
	public void onClosed(Closed event) {
		if (event.checkReemitted(this)) {
			return;
		}
		suggestions.onClosed(event);
		event.reemit();
	}

	@Override
	public void onEditorAsk(EditorAsk event) {
		if (event.isEmptyAsk() && attributes.closeSuggestionsOnEmptyAsk) {
			suggestions.toState(State.CLOSED);
		} else {
			suggestions.toState(State.LOADING);
		}
		attributes.answer.ask(event.getModel(), this::onAnswers,
				this::onAskException);
	}

	@Override
	public void onNavigation(Navigation event) {
		if (suggestions instanceof KeyboardNavigation.Navigation.Handler) {
			((KeyboardNavigation.Navigation.Handler) suggestions)
					.onNavigation(event);
		}
	}

	@Override
	public void onSelectionChanged(SelectionChanged event) {
		if (event.checkReemitted(this)) {
			return;
		}
		setChosenSuggestions(suggestions.provideSelectedValue());
		event.reemit();
	}

	@Override
	public Object provideSelectedValue() {
		return getValue();
	}

	public void
			setNonOverlaySuggestionResults(Object nonOverlaySuggestionResults) {
		set("nonOverlaySuggestionResults", this.nonOverlaySuggestionResults,
				nonOverlaySuggestionResults,
				() -> this.nonOverlaySuggestionResults = nonOverlaySuggestionResults);
	}

	public void setValue(Object value) {
		var old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	protected void initFields() {
		editor = attributes.editorSupplier.get();
		editor.withSuggestor(this);
		suggestions = new SuggestionChoices(this);
	}

	protected void onAnswers(Answers answers) {
		suggestions.toState(State.LOADED);
		suggestions.onAnswers(answers);
	}

	protected void onAskException(Throwable throwsable) {
		suggestions.toState(State.EXCEPTION);
		suggestions.onAskException(throwsable);
	}

	void setChosenSuggestions(Object value) {
		if (value == null) {
			setValue(null);
		} else if (value instanceof Suggestion) {
			setValue(((Suggestion) value).getModel());
		} else {
			setValue(((List<Suggestion>) value).stream()
					.map(Suggestion::getModel).collect(Collectors.toList()));
		}
	}
}
