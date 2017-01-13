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
import cc.alcina.framework.gwt.client.gwittir.renderer.IdToStringRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracle;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class DomainObjectSuggestCustomiser
		implements Customiser, BoundWidgetProvider {
	public static final String TARGET_CLASS = "targetClass";

	public static final String RENDERER_CLASS = "rendererClass";

	public static final String READONLY_CUSTOMISER_CLASS = "readonlyCustomiserClass";

	public static final String HINT = "hint";

	public static final String SHOW_ON_FOCUS = "showOnFocus";

	private Class classValue;

	private Class rendererClassValue;

	private String hintValue;

	private Class readonlyCustomiserClassValue;

	private boolean showOnFocus;

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
		return editable ? this
				: readonlyCustomiserClassValue == null
						? new RenderedLabelProvider(rendererClassValue, null)
						: ((Customiser) Reflections.classLookup()
								.newInstance(readonlyCustomiserClassValue))
										.getProvider(editable, objectClass,
												multiple, info);
	}

	@ClientInstantiable
	public static class BoundSuggestOracleResponseTypeRenderer
			implements Renderer<BoundSuggestOracleResponseType, String> {
		@Override
		public String render(BoundSuggestOracleResponseType o) {
			return o == null ? null : o.toSuggestionString();
		}
	}

	@Override
	public BoundWidget get() {
		BoundSuggestBox boundSuggestBox = new BoundSuggestBox<>();
		boundSuggestBox.setRenderer((Renderer) Reflections.classLookup()
				.newInstance(rendererClassValue));
		boundSuggestBox.suggestOracle(
				new BoundSuggestOracle().clazz(classValue).hint(hintValue));
		boundSuggestBox.setShowOnFocus(showOnFocus);
		return boundSuggestBox;
	}
}