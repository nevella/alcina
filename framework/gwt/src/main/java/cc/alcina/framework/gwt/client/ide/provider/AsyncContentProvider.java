package cc.alcina.framework.gwt.client.ide.provider;

import com.google.gwt.user.client.ui.HTML;

public interface AsyncContentProvider {
	HTML getWidget(String key, String styleClassName);
}
