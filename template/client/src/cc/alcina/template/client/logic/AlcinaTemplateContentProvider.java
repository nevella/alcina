package cc.alcina.template.client.logic;

import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.csobjects.ContentNode;
import cc.alcina.framework.gwt.client.ide.provider.ContentProvider.ContentProviderSource;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
/**
 * Sample - unused - but see MainCmp.showContent
 *
 */
public class AlcinaTemplateContentProvider implements ContentProviderSource {

	public static final String HOME_BLURB = "blurb";

	public static final String HOME_FEATURES = "features";


	public static final String CONTACT_US = "contact us";

	public static final String TERMS_OF_USE = "terms of use";

	public static final String PRIVACY_STATEMENT = "privacy statement";

	public static final String WELCOME_MESSAGE = "welcome message";


	public static final String PROBLEMS_LOGGING_IN = "Problems logging in";

	public static final String HELP_ROOT = "Help";

	Map<String, ContentNode> contentMap = new HashMap<String, ContentNode>();

	public AlcinaTemplateContentProvider() {
	}

	public String getContent(String key) {
		key = key.toLowerCase();
		return contentMap.containsKey(key) ? WidgetUtils
				.stripStructuralTags(contentMap.get(key).getContent())
				: "No content for key: " + key;
	}

	public ContentNode getNode(String key) {
		key = key.toLowerCase();
		if (contentMap.containsKey(key)) {
			return contentMap.get(key);
		}
		throw new WrappedRuntimeException("No content for key " + key,
				SuggestedAction.NOTIFY_ERROR);
	}

	public void refresh() {
		//contentMap = AlcinaTemplateObjects.current().getContentRoot().createTitleMap();
	}
}
