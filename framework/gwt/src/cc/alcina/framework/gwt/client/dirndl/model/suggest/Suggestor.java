package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
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
@Directed(
	receives = { SuggestorEvents.EditorAsk.class,
			ModelEvents.SelectionChanged.class,
			// suggestion overlay close
			ModelEvents.Closed.class },
	emits = ModelEvents.SelectionChanged.class)
public class Suggestor extends Model
		implements SuggestorEvents.EditorAsk.Handler,
		ModelEvents.SelectionChanged.Handler, HasSelectedValue,
		KeyboardNavigation.Navigation.Handler, ModelEvents.Closed.Handler {
	public static Builder builder() {
		return new Builder();
	}

	protected Editor editor;

	protected Suggestions suggestions;

	Object nonOverlaySuggestionResults;

	Builder builder;

	private Object value;

	private Suggestor(Builder builder) {
		this.builder = builder;
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

	public Builder getBuilder() {
		return this.builder;
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
			if (builder.isSuggestOnBind()) {
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
		suggestions.toState(State.LOADING);
		builder.answer.ask(event.getModel(), this::onAnswers,
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
		editor = builder.editorSupplier.get();
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
	}

	public static class Builder {
		String inputPrompt;

		List<Class<? extends Model>> logicalAncestors = List.of();

		boolean focusOnBind;

		boolean selectAllOnFocus;

		Answer<?> answer;

		OverlayPosition.Position suggestionXAlign = Position.START;

		boolean suggestOnBind;

		Supplier<? extends Editor> editorSupplier = InputEditor::new;

		boolean inputEditorKeyboardNavigationEnabled = true;

		boolean nonOverlaySuggestionResults;

		public Suggestor build() {
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

		public boolean isSuggestOnBind() {
			return this.suggestOnBind;
		}

		public Builder withAnswer(Answer answer) {
			this.answer = answer;
			return this;
		}

		public Builder
				withEditorSupplier(Supplier<? extends Editor> editorSupplier) {
			this.editorSupplier = editorSupplier;
			return this;
		}

		public Builder withFocusOnBind(boolean focusOnBind) {
			this.focusOnBind = focusOnBind;
			return this;
		}

		public Builder withInputEditorKeyboardNavigationEnabled(
				boolean inputEditorKeyboardNavigationEnabled) {
			this.inputEditorKeyboardNavigationEnabled = inputEditorKeyboardNavigationEnabled;
			return this;
		}

		public Builder withInputPrompt(String inputPrompt) {
			this.inputPrompt = inputPrompt;
			return this;
		}

		public Builder withLogicalAncestors(
				List<Class<? extends Model>> logicalAncestors) {
			this.logicalAncestors = logicalAncestors;
			return this;
		}

		public Builder withNonOverlaySuggestionResults(
				boolean nonOverlaySuggestionResults) {
			this.nonOverlaySuggestionResults = nonOverlaySuggestionResults;
			return this;
		}

		public Builder withSelectAllOnFocus(boolean selectAllOnFocus) {
			this.selectAllOnFocus = selectAllOnFocus;
			return this;
		}

		public Builder withSuggestionXAlign(
				OverlayPosition.Position suggestionXAlign) {
			this.suggestionXAlign = suggestionXAlign;
			return this;
		}

		public Builder withSuggestOnBind(boolean suggestOnBind) {
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
	}

	public enum Property implements PropertyEnum {
		value
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
	}

	/*
	 * Marker for the payload of a Suggestion
	 */
	public interface Suggestion {
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
		public static class ModelSuggestion extends Model.Fields
				implements Suggestion {
			public Model model;

			public boolean match;

			public ModelSuggestion(Model model) {
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
		}
	}

	public interface SuggestionModel {
	}

	@Directed(emits = ModelEvents.SelectionChanged.class)
	public interface Suggestions
			extends HasSelectedValue, ModelEvents.Closed.Handler {
		void close();

		void onAnswers(Answers answers);

		default void onAskException(Throwable throwsable) {
			throw WrappedRuntimeException.wrap(throwsable);
		}

		void toState(State state);

		public static enum State {
			LOADING, LOADED, EXCEPTION, UNBOUND
		}
	}
}
