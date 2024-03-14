package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;

@Directed(renderer = DirectedRenderer.Delegating.class)
@Feature.Ref(Feature_TraversalProcessView_AppSuggestorImplementation.class)
public class AppSuggestorTraversal extends AppSuggestor {
	public AppSuggestorTraversal() {
		super(createAppAttributes());
	}

	static AppSuggestor.Attributes createAppAttributes() {
		AppSuggestor.Attributes appSuggestorAttributes = new AppSuggestor.Attributes(
				new AnswerSupplierImpl());
		return appSuggestorAttributes;
	}

	@Override
	protected Suggestor.Attributes createSuggestorAttributes() {
		Suggestor.Attributes attributes = super.createSuggestorAttributes();
		attributes.withInputPrompt("Filter selections");
		attributes.withNonOverlaySuggestionResults(false);
		// FIXME - romcom - back to true, but will require
		attributes.withSelectAllOnFocus(true);
		return attributes;
	}
}
