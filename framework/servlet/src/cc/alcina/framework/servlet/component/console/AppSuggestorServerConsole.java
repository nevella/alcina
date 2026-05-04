package cc.alcina.framework.servlet.component.console;

import cc.alcina.framework.servlet.component.console.ServerConsoleBrowser.Ui;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;

@Directed(renderer = DirectedRenderer.Delegating.class)
public class AppSuggestorServerConsole extends AppSuggestor {
	public AppSuggestorServerConsole() {
		super(createAppAttributes());
	}

	static AppSuggestor.Attributes createAppAttributes() {
		AppSuggestor.Attributes appSuggestorAttributes = new AppSuggestor.Attributes(
				Ui.get().createAnswerSupplier());
		return appSuggestorAttributes;
	}

	@Override
	protected Suggestor.Attributes createSuggestorAttributes() {
		Suggestor.Attributes attributes = super.createSuggestorAttributes();
		attributes.withInputPrompt("Filter, invoke...");
		attributes.withNonOverlaySuggestionResults(false);
		attributes.withSelectAllOnFocus(true);
		return attributes;
	}
}
