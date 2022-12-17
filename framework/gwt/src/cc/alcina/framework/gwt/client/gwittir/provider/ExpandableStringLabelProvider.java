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
package cc.alcina.framework.gwt.client.gwittir.provider;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.gwittir.HasMaxWidth;
import cc.alcina.framework.gwt.client.gwittir.widget.ExpandableLabel;

/**
 *
 * @author Nick Reddel
 */
public class ExpandableStringLabelProvider
		implements BoundWidgetProvider, HasMaxWidth {
	private final int maxWidth;

	private final boolean forceColumnWidth;

	private final boolean showNewlinesAsBreaks;

	private boolean showAsPopup;

	private boolean escapeHtml;

	private Class rendererClass;

	public ExpandableStringLabelProvider(int maxWidth, boolean forceColumnWidth,
			boolean showNewlinesAsBreaks, boolean showAsPopup,
			boolean escapeHtml, Class rendererClass) {
		this.maxWidth = maxWidth;
		this.forceColumnWidth = forceColumnWidth;
		this.showNewlinesAsBreaks = showNewlinesAsBreaks;
		this.showAsPopup = showAsPopup;
		this.escapeHtml = escapeHtml;
		this.rendererClass = rendererClass;
	}

	public BoundWidget get() {
		ExpandableLabel label = new ExpandableLabel(maxWidth);
		label.setShowNewlinesAsBreaks(showNewlinesAsBreaks);
		label.setShowAsPopup(showAsPopup);
		label.setEscapeHtml(escapeHtml);
		if (rendererClass != null) {
			label.setRenderer(
					(Renderer) Reflections.newInstance(rendererClass));
		}
		return label;
	}

	@Override
	public String getColumnWidthString() {
		return null;
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	@Override
	public int getMinPercentOfTable() {
		return 0;
	}

	public boolean isForceColumnWidth() {
		return forceColumnWidth;
	}
}