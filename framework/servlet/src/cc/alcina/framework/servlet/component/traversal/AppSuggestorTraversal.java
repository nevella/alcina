package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView.Ui;

@Directed(renderer = DirectedRenderer.Delegating.class)
@Feature.Ref(Feature_TraversalProcessView_AppSuggestorImplementation.class)
public class AppSuggestorTraversal extends AppSuggestor {
	public AppSuggestorTraversal() {
		super(createAppAttributes());
	}

	static AppSuggestor.Attributes createAppAttributes() {
		AppSuggestor.Attributes appSuggestorAttributes = new AppSuggestor.Attributes(
				Ui.get().createAnswerSupplier(null));
		return appSuggestorAttributes;
	}

	@Override
	protected Suggestor.Attributes createSuggestorAttributes() {
		Suggestor.Attributes attributes = super.createSuggestorAttributes();
		attributes.withInputPrompt("Filter selections");
		attributes.withNonOverlaySuggestionResults(false);
		attributes.withSelectAllOnFocus(true);
		return attributes;
	}
}
