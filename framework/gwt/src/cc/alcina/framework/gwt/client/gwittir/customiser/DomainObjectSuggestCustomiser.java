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
package cc.alcina.framework.gwt.client.gwittir.customiser;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.customiser.RenderedLabelCustomiser.RenderedLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracle;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 * 
 *         Note - this *can* be used with non HasLocalId objects -
 *         "domainobject" is more an indication that "comes from server"
 */
public class DomainObjectSuggestCustomiser
		implements Customiser, BoundWidgetProvider {
	public static final String TARGET_CLASS = "targetClass";

	public static final String RENDERER_CLASS = "rendererClass";

	public static final String READONLY_CUSTOMISER_CLASS = "readonlyCustomiserClass";

	public static final String HINT = "hint";

	public static final String SHOW_ON_FOCUS = "showOnFocus";

	public static final String WITH_PLACEHOLDER = "with-placeholder";

	public static final String PLACEHOLDER = "WITH_PLACEHOLDER";

	private Class classValue;

	private Class rendererClassValue;

	private String hintValue;

	private Class readonlyCustomiserClassValue;

	private boolean showOnFocus;

	private boolean withPlaceholder;

	private String placeholderText;

	@Override
	public BoundWidget get() {
		BoundSuggestBox boundSuggestBox = new BoundSuggestBox<>();
		boundSuggestBox.setRenderer(getRenderer());
		boundSuggestBox.setWithPlaceholder(withPlaceholder);
		boundSuggestBox.setPlaceholderText(placeholderText);
		boundSuggestBox.suggestOracle(
				new BoundSuggestOracle().clazz(classValue).hint(hintValue));
		boundSuggestBox.setShowOnFocus(showOnFocus);
		return boundSuggestBox;
	}

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		classValue = NamedParameter.Support
				.getParameter(info.parameters(), TARGET_CLASS).classValue();
		rendererClassValue = NamedParameter.Support.classValue(
				info.parameters(), RENDERER_CLASS,
				BoundSuggestOracleResponseTypeRenderer.class);
		readonlyCustomiserClassValue = NamedParameter.Support
				.classValue(info.parameters(), READONLY_CUSTOMISER_CLASS, null);
		hintValue = NamedParameter.Support.stringValue(info.parameters(), HINT,
				"");
		showOnFocus = NamedParameter.Support.booleanValue(info.parameters(),
				SHOW_ON_FOCUS);
		withPlaceholder = NamedParameter.Support
				.booleanValueDefaultTrue(info.parameters(), WITH_PLACEHOLDER);
		placeholderText = NamedParameter.Support.stringValue(info.parameters(),
				PLACEHOLDER, "Type for suggestions");
		return editable ? this
				: readonlyCustomiserClassValue == null
						? new RenderedLabelProvider(rendererClassValue, null)
						: ((Customiser) Reflections.classLookup()
								.newInstance(readonlyCustomiserClassValue))
										.getProvider(editable, objectClass,
												multiple, info);
	}

	public Renderer getRenderer() {
		return (Renderer) Reflections.classLookup()
				.newInstance(rendererClassValue);
	}

	public boolean isShowOnFocus() {
		return this.showOnFocus;
	}

	public void setShowOnFocus(boolean showOnFocus) {
		this.showOnFocus = showOnFocus;
	}

	@ClientInstantiable
	public static class BoundSuggestOracleResponseTypeRenderer
			implements Renderer<BoundSuggestOracleResponseType, String> {
		@Override
		public String render(BoundSuggestOracleResponseType o) {
			return o == null ? null : o.toSuggestionResultString();
		}
	}

	@ClientInstantiable
	public static class BoundSuggestOracleResponseTypeSuggestionRenderer
			implements Renderer<BoundSuggestOracleResponseType, String> {
		@Override
		public String render(BoundSuggestOracleResponseType o) {
			return o == null ? null : o.toSuggestionString();
		}
	}
}