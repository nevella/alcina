package cc.alcina.framework.gwt.client.dirndl.cmp;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.SuggestOracle;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.AppSuggestor.AnswerImpl.Invocation;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Opened;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answers;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Suggestion;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;

@Directed(renderer = DirectedRenderer.Delegating.class)
public class AppSuggestor extends Model.Fields
		implements ModelEvents.SelectionChanged.Handler,
		ModelEvents.Closed.Handler, ModelEvents.Opened.Handler {
	@Directed(tag = "app-suggestor")
	public Suggestor suggestor;

	Attributes attributes;

	public AppSuggestor(AppSuggestor.Attributes attributes) {
		this.attributes = attributes;
		Suggestor.Attributes suggestorAttributes = createSuggestorAttributes();
		suggestor = suggestorAttributes.build();
	}

	public static class Attributes {
		public final AnswerSupplier answerSupplier;

		public Attributes(AnswerSupplier answerSupplier) {
			this.answerSupplier = answerSupplier;
		}
	}

	protected Suggestor.Attributes createSuggestorAttributes() {
		Suggestor.Attributes attributes = Suggestor.builder();
		attributes.withFocusOnBind(true);
		attributes.withSelectAllOnFocus(true);
		attributes.withSuggestionXAlign(Position.CENTER);
		attributes.withLogicalAncestors(List.of(AppSuggestor.class));
		attributes.withAnswer(new AnswerImpl());
		attributes.withNonOverlaySuggestionResults(true);
		return attributes;
	}

	public void clear() {
		suggestor.clear();
	}

	public void clearSuggestions() {
		suggestor.closeSuggestions();
	}

	public void copyEditorInputFrom(AppSuggestor appSuggestor) {
		suggestor.copyEditorInputFrom(appSuggestor.suggestor);
	}

	public void focus() {
		suggestor.focus();
	}

	@Override
	public void onClosed(Closed event) {
	}

	@Override
	public void onOpened(Opened event) {
	}

	@Override
	public void onSelectionChanged(SelectionChanged event) {
		AppSuggestionView suggestion = (AppSuggestionView) suggestor
				.provideSelectedValue();
		if (suggestion.suggestion.url() != null) {
			History.newItem(suggestion.suggestion.url());
		} else {
			event.reemitAs(this, suggestion.suggestion.modelEvent());
		}
		suggestor.closeSuggestions();
		suggestor.setValue(null);
		event.reemitAs(this, SuggestionSelected.class);
	}

	public interface AppSuggestion extends SuggestOracle.Suggestion {
		String provideFirst();

		Class<? extends ModelEvent> modelEvent();

		String url();

		String secondary();
	}

	@Directed(tag = "app-suggestion")
	public static class AppSuggestionView extends Model.Fields {
		private AppSuggestion suggestion;

		@Directed
		Contents contents;

		@Directed
		Object options = LeafRenderer.OBJECT_INSTANCE;

		AppSuggestionView(AppSuggestion suggestion) {
			this.suggestion = suggestion;
			this.contents = new Contents();
		}

		@Directed.AllProperties
		class Contents extends Model.Fields {
			String first = suggestion.provideFirst();

			String second = suggestion.secondary();
		}

		@Override
		public String toString() {
			return suggestion.provideFirst();
		}
	}

	public static class SuggestionSelected
			extends ModelEvent<Object, SuggestionSelected.Handler> {
		@Override
		public void dispatch(SuggestionSelected.Handler handler) {
			handler.onSuggestionSelected(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSuggestionSelected(SuggestionSelected event);
		}
	}

	public interface AnswerSupplier {
		void begin(Invocation invocation);

		default void processResults(Invocation invocation,
				List<? extends AppSuggestion> appSuggestions) {
			invocation.processResults(appSuggestions);
		}
	}

	/*
	 * Gets a list of SuggestionModel objects (wrapping OmniSuggestion objects)
	 * that match the decorator text
	 */
	public class AnswerImpl implements Answer<StringAsk> {
		public class Invocation {
			public final StringAsk ask;

			public final Consumer<Answers> answersHandler;

			public final Consumer<Throwable> exceptionHandler;

			Invocation(StringAsk ask, Consumer<Answers> answersHandler,
					Consumer<Throwable> exceptionHandler) {
				this.ask = ask;
				this.answersHandler = answersHandler;
				this.exceptionHandler = exceptionHandler;
			}

			void invoke() {
				attributes.answerSupplier.begin(this);
			}

			void processResults(List<? extends AppSuggestion> appSuggestions) {
				List<Suggestion> suggestions = appSuggestions.stream()
						.map(AppSuggestionView::new)
						.map(Suggestion.ModelSuggestion::new)
						.collect(Collectors.toList());
				Suggestor.Answers result = new Answers();
				result.setTotal(suggestions.size());
				result.setSuggestions(suggestions);
				answersHandler.accept(result);
			}
		}

		@Override
		public void ask(StringAsk ask, Consumer<Answers> answersHandler,
				Consumer<Throwable> exceptionHandler) {
			if (Ax.isBlank(ask.getValue())) {
				Suggestor.Answers result = new Answers();
				answersHandler.accept(result);
				return;
			}
			new Invocation(ask, answersHandler, exceptionHandler).invoke();
		}
	}
}
