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

import cc.alcina.framework.common.client.logic.domain.EntityDataObject.OneToManyMultipleSummary;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingHtml;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class OneToManyMultipleSummaryCustomiser
		implements Customiser, BoundWidgetProvider {
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		return this;
	}

	@Override
	public BoundWidget get() {
		RenderingHtml html = new RenderingHtml();
		html.setRenderer(new OneToManyMultipleSummaryToHtmlRenderer(html));
		html.setStyleName("");
		return html;
	}

	private static class OneToManyMultipleSummaryToHtmlRenderer
			implements Renderer<OneToManyMultipleSummary, String> {
		private RenderingHtml html;

		public OneToManyMultipleSummaryToHtmlRenderer(RenderingHtml html) {
			this.html = html;
		}

		@Override
		public String render(OneToManyMultipleSummary o) {
			if (o.getSize() == 0) {
				return "";
			}
			String template = "<a href='#%s'>%s</a>";
			String token = o.getPlace().toTokenString();
			return Ax.format(template, token,
					o.getPlace().provideCategoryString(o.getSize()));
		}
	}
}
