package cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor;

import java.io.Serializable;
import java.util.List;

import com.google.gwt.user.client.ui.SuggestOracle;

public class AppSuggestorResponse extends SuggestOracle.Response
		implements Serializable {
	public List<? extends AppSuggestionEntry> appSuggestions() {
		return (List<? extends AppSuggestionEntry>) getSuggestions();
	}
}
