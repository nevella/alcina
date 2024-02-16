package au.com.barnet.jade.jadex.client.module.common.cmp.header;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;

import au.com.barnet.jade.jadex.client.module.common.JadeTheme;
import au.com.barnet.jade.jadex.client.module.common.JadexModel;
import au.com.barnet.jade.jadex.shared.module.appsuggestor.OmniRequest;
import au.com.barnet.jade.jadex.shared.module.appsuggestor.OmniResponse;
import au.com.barnet.jade.jadex.shared.module.appsuggestor.OmniResponse.OmniSuggestion;
import au.com.barnet.jade.jadex.shared.module.appsuggestor.ReflectiveOmniRemoteServiceAsync;
import au.com.barnet.jade.meta.ui2.component.feature.omni.Feature_Omni.Feature_Omni_AppSuggestorImplementation;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.AppSuggestor.AppSuggestion;
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
import cc.alcina.framework.gwt.client.util.Async;

@Directed(renderer = DirectedRenderer.Delegating.class)
@Feature.Ref(Feature_Omni_AppSuggestorImplementation.class)
public class AppSuggestor extends Model.Fields
		implements ModelEvents.SelectionChanged.Handler,
		ModelEvents.Closed.Handler, ModelEvents.Opened.Handler {
	@Directed(tag = "app-suggestor")
	public Suggestor suggestor;

	public AppSuggestor() {
		Suggestor.Builder builder = Suggestor.builder();
		builder.withFocusOnBind(true);
		builder.withSelectAllOnFocus(true);
		builder.withSuggestionXAlign(Position.CENTER);
		builder.withLogicalAncestors(List.of(AppSuggestor.class));
		builder.withInputPrompt(JadeTheme.get().getAppSuggestorHint());
		builder.withAnswer(new AnswerImpl());
		builder.withNonOverlaySuggestionResults(true);
		suggestor = builder.build();
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
		JadexModel.get().setShowingAppSuggestions(false);
	}

	@Override
	public void onOpened(Opened event) {
		JadexModel.get().setShowingAppSuggestions(true);
	}

	@Override
	public void onSelectionChanged(SelectionChanged event) {
		AppSuggestion suggestion = (AppSuggestion) suggestor
				.provideSelectedValue();
		if (suggestion.suggestion.url != null) {
			History.newItem(suggestion.suggestion.url);
		} else {
			event.reemitAs(this, suggestion.suggestion.modelEvent);
		}
		suggestor.closeSuggestions();
		suggestor.setValue(null);
		event.reemitAs(this, SuggestionSelected.class);
	}

	public static class AppSuggestion extends Model.Fields {
		private OmniSuggestion suggestion;

		@Directed
		Contents contents;

		@Directed
		Object options = LeafRenderer.OBJECT_INSTANCE;

		AppSuggestion(OmniSuggestion suggestion) {
			this.suggestion = suggestion;
			this.contents = new Contents();
		}

		@Directed.AllProperties
		class Contents extends Model.Fields {
			String first = suggestion.provideFirst();

			String second = suggestion.secondary;
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

	/*
	 * Gets a list of SuggestionModel objects (wrapping OmniSuggestion objects)
	 * that match the decorator text
	 */
	public class AnswerImpl implements Answer<StringAsk> {
		protected AsyncCallback runningCallback = null;

		@Override
		// FIXME - DCA1x1 - exceptionHandler
		public void ask(StringAsk ask, Consumer<Answers> answersHandler,
				Consumer<Throwable> exceptionHandler) {
			if (Ax.isBlank(ask.getValue())) {
				Suggestor.Answers result = new Answers();
				answersHandler.accept(result);
				return;
			}
			runningCallback = Async.<OmniResponse> callbackBuilder()
					.success(response -> handleSuggestionResponse(ask,
							answersHandler, response))
					.withCancelInflight(runningCallback).build();
			OmniRequest request = new OmniRequest().withQuery(ask.getValue());
			request.populateContext();
			ReflectiveOmniRemoteServiceAsync.get().omniRequest(request,
					runningCallback);
		}

		protected void handleSuggestionResponse(StringAsk ask,
				Consumer<Answers> answersHandler, OmniResponse response) {
			List<Suggestion> suggestions = response.getSuggestions().stream()
					.map(s -> new AppSuggestion((OmniSuggestion) s))
					.map(Suggestion.ModelSuggestion::new)
					.collect(Collectors.toList());
			Suggestor.Answers result = new Answers();
			result.setTotal(suggestions.size());
			result.setSuggestions(suggestions);
			answersHandler.accept(result);
		}
	}

	/**
	 * App suggestor customisation vehicle
	 */
	public static class Peer {
		void handleAsk(AnswerImpl answerImpl, StringAsk ask) {
			// determine suggestions, call handleSuggestionsResponse
		}
	}
}
