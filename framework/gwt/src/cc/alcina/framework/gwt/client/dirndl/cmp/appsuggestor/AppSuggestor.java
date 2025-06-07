package cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.AnswerImpl.Invocation;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Opened;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionHandled;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answers;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.SuggestOnBind;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Suggestion;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Suggestion.ModelSuggestion;

@Directed(renderer = DirectedRenderer.Delegating.class)
@TypeSerialization(reflectiveSerializable = false)
@DirectedContextResolver(AppSuggestor.Resolver.class)
@TypedProperties
public class AppSuggestor extends Model.Fields
		implements ModelEvents.SelectionChanged.Handler,
		ModelEvents.Closed.Handler, ModelEvents.Opened.Handler,
		AppSuggestorEvents.Close.Handler, ModelEvents.SelectionHandled.Handler {
	public static transient PackageProperties._AppSuggestor properties = PackageProperties.appSuggestor;

	public static class Attributes {
		public final AnswerSupplier answerSupplier;

		public Attributes(AnswerSupplier answerSupplier) {
			this.answerSupplier = answerSupplier;
		}
	}

	@Directed(tag = "app-suggestion")
	@TypeSerialization(reflectiveSerializable = false)
	public static class AppSuggestionArea extends Model.Fields {
		@Directed.AllProperties
		public class Contents extends Model.Fields {
			String first = suggestion.provideFirst();

			String second = suggestion.secondary();
		}

		static class Transform
				implements ModelTransform<AppSuggestion, AppSuggestionArea> {
			@Override
			public AppSuggestionArea apply(AppSuggestion suggestion) {
				return new AppSuggestionArea(suggestion);
			}
		}

		public AppSuggestion suggestion;

		@Directed
		public Contents contents;

		@Directed
		public Object options = LeafRenderer.OBJECT_INSTANCE;

		@Binding(type = Type.PROPERTY)
		public AppSuggestionCategory category;

		public AppSuggestionArea(AppSuggestion suggestion) {
			this.suggestion = suggestion;
			this.contents = new Contents();
			this.category = suggestion.category();
		}

		@Override
		public String toString() {
			return suggestion.provideFirst();
		}
	}

	public static class SuggestionSelected
			extends ModelEvent<Object, SuggestionSelected.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onSuggestionSelected(SuggestionSelected event);
		}

		@Override
		public void dispatch(SuggestionSelected.Handler handler) {
			handler.onSuggestionSelected(this);
		}
	}

	public interface AnswerSupplier {
		void begin(Invocation invocation);

		default boolean checkEmptyAsk() {
			return false;
		}

		default void processResults(Invocation invocation,
				List<? extends AppSuggestion> appSuggestions) {
			invocation.processResults(appSuggestions);
		}
	}

	/*
	 * Gets a list of SuggestionModel objects (wrapping OmniSuggestion objects)
	 * that match the decorator text
	 */
	public static class AnswerImpl implements Answer<StringAsk> {
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
				answerSupplier.begin(this);
			}

			void processResults(List<? extends AppSuggestion> appSuggestions) {
				List<Suggestion> suggestions = appSuggestions.stream()
						.map(Suggestion.ModelSuggestion::new)
						.collect(Collectors.toList());
				Suggestor.Answers result = new Answers();
				result.setTotal(suggestions.size());
				result.setSuggestions(suggestions);
				answersHandler.accept(result);
			}
		}

		AnswerSupplier answerSupplier;

		public AnswerImpl(AnswerSupplier answerSupplier) {
			this.answerSupplier = answerSupplier;
		}

		@Override
		public void ask(StringAsk ask, Consumer<Answers> answersHandler,
				Consumer<Throwable> exceptionHandler) {
			if (Ax.isBlank(ask.getValue()) && !answerSupplier.checkEmptyAsk()) {
				Suggestor.Answers result = new Answers();
				answersHandler.accept(result);
				return;
			}
			new Invocation(ask, answersHandler, exceptionHandler).invoke();
		}
	}

	public static class Resolver extends ContextResolver.AnnotationCustomiser {
		Resolver() {
			resolveTransform(ModelSuggestion.class, "model")
					.with(AppSuggestionArea.Transform.class);
		}
	}

	@Directed(tag = "app-suggestor")
	public Suggestor suggestor;

	Attributes attributes;

	boolean currentSelectionHandled;

	public AppSuggestor(AppSuggestor.Attributes attributes) {
		this.attributes = attributes;
		Suggestor.Attributes suggestorAttributes = createSuggestorAttributes();
		suggestor = suggestorAttributes.create();
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

	/*
	 * This text will not cause an Ask to be emitted
	 */
	public void setAcceptedFilterText(String acceptedFilterText) {
		suggestor.getEditor().setAcceptedFilterText(acceptedFilterText);
	}

	public void setFilterText(String filterText) {
		suggestor.getEditor().setFilterText(filterText);
	}

	@Override
	public void onSelectionChanged(SelectionChanged event) {
		if (currentSelectionHandled) {
			currentSelectionHandled = false;
		} else {
			AppSuggestionEntry suggestion = (AppSuggestionEntry) suggestor
					.provideSelectedValue();
			if (suggestion.url() != null) {
				History.newItem(suggestion.url());
			} else {
				event.reemitAs(this, suggestion.modelEvent(),
						suggestion.eventData());
			}
		}
		closeAndCleanup(event);
	}

	@Override
	public void onClose(AppSuggestorEvents.Close event) {
		closeAndCleanup(event);
	}

	@Override
	public void onSelectionHandled(SelectionHandled event) {
		this.currentSelectionHandled = true;
	}

	protected Suggestor.Attributes createSuggestorAttributes() {
		Suggestor.Attributes attributes = Suggestor.attributes();
		attributes.withFocusOnBind(true);
		attributes.withCloseSuggestionsOnEmptyAsk(true);
		attributes.withSelectAllOnFocus(true);
		attributes.withSuggestOnBind(SuggestOnBind.NON_EMPTY_VALUE);
		/*
		 * app css should guarantee that the dropdown and the suggestor are
		 * equal-sized, so START (default) is equivalent
		 */
		// attributes.withSuggestionXAlign(Position.CENTER);
		attributes.withLogicalAncestors(List.of(AppSuggestor.class));
		attributes.withAnswer(new AnswerImpl(this.attributes.answerSupplier));
		attributes.withNonOverlaySuggestionResults(true);
		attributes.withCheckEmptyAsk(
				this.attributes.answerSupplier.checkEmptyAsk());
		return attributes;
	}

	protected void closeAndCleanup(ModelEvent event) {
		suggestor.closeSuggestions();
		suggestor.setValue(null);
		event.reemitAs(this, SuggestionSelected.class);
	}
}
