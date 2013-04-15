/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.ide.provider;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;

import com.google.gwt.user.client.ui.HTML;

/**
 * 
 * @author Nick Reddel
 */
public class ContentProvider {
	private static ContentProviderSource provider;

	public static ContentProviderSource getProvider() {
		if (provider != null) {
			return provider;
		}
		throw new WrappedRuntimeException("No content provider registered",
				SuggestedAction.NOTIFY_ERROR);
	}

	public static void registerProvider(ContentProviderSource p) {
		provider = p;
	}

	public static HTML getWidget(String key) {
		return getWidget(key, null);
	}

	public static HTML getWidget(String key, String styleClassName) {
		if (provider != null) {
			if (provider instanceof AsyncContentProvider) {
				return ((AsyncContentProvider) provider).getWidget(key,
						styleClassName);
			} else {
				String content = provider.getContent(key);
				content = content == null ? "[" + key + "]" : content;
				HTML html = new HTML(content);
				if (styleClassName != null) {
					html.setStyleName(styleClassName);
				}
				return html;
			}
		}
		throw new WrappedRuntimeException("No content provider registered",
				SuggestedAction.NOTIFY_ERROR);
	}

	public static String getContent(String key) {
		if (provider != null) {
			return provider.getContent(key);
		}
		throw new WrappedRuntimeException("No content provider registered",
				SuggestedAction.NOTIFY_ERROR);
	}

	public static boolean hasContent(String key) {
		if (provider != null) {
			String trim = getContent(key).trim();
			return !trim.isEmpty() && !trim.startsWith("No content for key");
		}
		throw new WrappedRuntimeException("No content provider registered",
				SuggestedAction.NOTIFY_ERROR);
	}

	public static void refresh() {
		if (provider != null) {
			provider.refresh();
			return;
		}
		throw new WrappedRuntimeException("No content provider registered",
				SuggestedAction.NOTIFY_ERROR);
	}

	public interface ContentProviderSource {
		public String getContent(String key);

		public void refresh();
	}
}
