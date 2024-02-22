package au.com.barnet.jade.jadex.client.module.common.cmp.header;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.AppSuggestionEntry;
import cc.alcina.framework.gwt.client.dirndl.cmp.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.cmp.AppSuggestor.AnswerImpl.Invocation;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalProcessView;

@Directed(renderer = DirectedRenderer.Delegating.class)
@Feature.Ref(Feature_TraversalProcessView.AppSuggestorImplementation.class)
public class AppSuggestorTraversal extends AppSuggestor {
	public AppSuggestorTraversal() {
		super(createAppAttributes());
	}

	static AppSuggestor.Attributes createAppAttributes() {
		AppSuggestor.Attributes appSuggestorAttributes = new AppSuggestor.Attributes(
				new AnswerSupplierImpl());
		return appSuggestorAttributes;
	}

	static class AnswerSupplierImpl implements AppSuggestor.AnswerSupplier {
		protected AsyncCallback runningCallback = null;

		@Override
		public void begin(Invocation invocation) {
			// runningCallback = Async.<OmniResponse> callbackBuilder().success(
			// response -> handleSuggestionResponse(invocation, response))
			// .withCancelInflight(runningCallback).build();
			// OmniRequest request = new OmniRequest()
			// .withQuery(invocation.ask.getValue());
			// request.populateContext();
			// ReflectiveOmniRemoteServiceAsync.get().omniRequest(request,
			// runningCallback);
			AppSuggestionEntry suggestion = new AppSuggestionEntry();
			suggestion.match = "Filter: " + invocation.ask.getValue();
			processResults(invocation, List.of(suggestion));
		}
	}

	@Override
	protected Suggestor.Attributes createSuggestorAttributes() {
		Suggestor.Attributes attributes = super.createSuggestorAttributes();
		attributes.withInputPrompt("Filter selections");
		attributes.withNonOverlaySuggestionResults(true);
		// FIXME - romcom - back to true, but will require
		attributes.withSelectAllOnFocus(false);
		return attributes;
	}
}
