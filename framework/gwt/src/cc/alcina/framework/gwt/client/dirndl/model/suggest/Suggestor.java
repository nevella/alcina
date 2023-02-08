package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
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
 * @author nick@alcina.cc
 *
 */
@Directed(
	receives = { SuggestorEvents.EditorAsk.class,
			ModelEvents.SelectionChanged.class },
	emits = ModelEvents.SelectionChanged.class)
public class Suggestor extends Model.WithNode
		implements SuggestorEvents.EditorAsk.Handler,
		ModelEvents.SelectionChanged.Handler, HasSelectedValue {
	protected Editor editor;

	protected Suggestions suggestions;

	protected Configuration configuration;

	private Object value;

	public Suggestor(Configuration configuration) {
		this.configuration = configuration;
		initFields();
	}

	@Directed(tag = "editor")
	public Editor getEditor() {
		return this.editor;
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
			if (configuration.isSuggestOnBind()) {
				Client.eventBus().queued().lambda(() -> editor.emitAsk())
						.dispatch();
				;
			}
		}
		super.onBind(event);
	}

	@Override
	public void onEditorAsk(EditorAsk event) {
		suggestions.toState(State.LOADING);
		configuration.answer.ask(event.getModel(), this::onAnswers,
				this::onAskException);
	}

	@Override
	public void onSelectionChanged(SelectionChanged event) {
		if (event.checkReemitted(node)) {
			return;
		}
		setChosenSuggestions(suggestions.provideSelectedValue());
		event.reemit();
	}

	@Override
	public Object provideSelectedValue() {
		return getValue();
	}

	public void setValue(Object value) {
		var old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	protected void initFields() {
		editor = new SimpleEditor(configuration);
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
	}

	/**
	 * <p>
	 * So named because 'query' is so tired
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public interface Ask {
		IntPair getResultRange();
	}

	public static class Configuration {
		private String inputPrompt;

		List<Class<? extends Model>> logicalAncestors = List.of();

		private boolean focusOnBind;

		private boolean selectAllOnBind;

		private Answer<?> answer;

		private OverlayPosition.Position suggestionXAlign = Position.START;

		private boolean suggestOnBind;

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

		public boolean isSelectAllOnBind() {
			return this.selectAllOnBind;
		}

		public boolean isSuggestOnBind() {
			return this.suggestOnBind;
		}

		public Configuration withAnswer(Answer answer) {
			this.answer = answer;
			return this;
		}

		public Configuration withFocusOnBind(boolean focusOnBind) {
			this.focusOnBind = focusOnBind;
			return this;
		}

		public Configuration withInputPrompt(String inputPrompt) {
			this.inputPrompt = inputPrompt;
			return this;
		}

		public Configuration withLogicalAncestors(
				List<Class<? extends Model>> logicalAncestors) {
			this.logicalAncestors = logicalAncestors;
			return this;
		}

		public Configuration withSelectAllOnBind(boolean selectAllOnBind) {
			this.selectAllOnBind = selectAllOnBind;
			return this;
		}

		public Configuration withSuggestionXAlign(
				OverlayPosition.Position suggestionXAlign) {
			this.suggestionXAlign = suggestionXAlign;
			return this;
		}

		public Configuration withSuggestOnBind(boolean suggestOnBind) {
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
		void emitAsk();
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
		String getMarkup();

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
		public static class Default extends Bindable implements Suggestion {
			private String markup;

			private Object model;

			private boolean match;

			@Override
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

			public static class StringAskAnswer<T> {
				public Answers ask(StringAsk ask, List<T> models,
						Function<T, String> stringRepresentation) {
					Answers result = new Answers();
					models.stream().map(model -> {
						String string = stringRepresentation.apply(model);
						MarkupMatch match = new MarkupMatch(string,
								ask.getValue());
						if (match.hasMatches()) {
							Suggestion.Default suggestion = new Suggestion.Default();
							suggestion.setMarkup(match.toMarkup());
							suggestion.setMatch(true);
							suggestion.setModel(model);
							return suggestion;
						} else {
							return null;
						}
					}).filter(Objects::nonNull).forEach(suggestion -> result
							.add(suggestion, ask.getResultRange()));
					return result;
				}

				static class MarkupMatch {
					List<IntPair> matches = new ArrayList<>();

					private String string;

					boolean emptyMatch = false;

					public MarkupMatch(String string, String value) {
						this.string = string;
						if (value.isEmpty()) {
							emptyMatch = true;
							return;
						}
						String matchString = string.toLowerCase();
						String matchValue = value.toLowerCase();
						int idx = 0;
						while (true) {
							int matchIdx = matchString.indexOf(matchValue, idx);
							if (matchIdx == -1) {
								break;
							} else {
								int end = matchIdx + value.length();
								matches.add(new IntPair(matchIdx, end));
								idx = end;
							}
						}
					}

					boolean hasMatches() {
						return emptyMatch || matches.size() > 0;
					}

					String toMarkup() {
						SafeHtmlBuilder builder = new SafeHtmlBuilder();
						int idx = 0;
						for (IntPair match : matches) {
							builder.appendEscaped(
									string.substring(idx, match.i1));
							builder.appendHtmlConstant("<match>");
							builder.appendEscaped(
									string.substring(match.i1, match.i2));
							builder.appendHtmlConstant("</match>");
							idx = match.i2;
						}
						builder.appendEscaped(string.substring(idx));
						return builder.toSafeHtml().asString();
					}
				}
			}
		}
	}

	public interface SuggestionModel {
	}

	@Directed(emits = ModelEvents.SelectionChanged.class)
	public interface Suggestions extends HasSelectedValue {
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
