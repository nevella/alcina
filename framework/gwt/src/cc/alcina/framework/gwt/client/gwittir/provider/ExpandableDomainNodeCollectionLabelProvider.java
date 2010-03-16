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

import cc.alcina.framework.gwt.client.gwittir.HasMaxWidth;
import cc.alcina.framework.gwt.client.gwittir.widget.ExpandableLabel;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 *
 * @author Nick Reddel
 */

 public class ExpandableDomainNodeCollectionLabelProvider implements
		BoundWidgetProvider, HasMaxWidth {
	private final int maxWidth;

	private final boolean forceColumnWidth;

	public ExpandableDomainNodeCollectionLabelProvider(int maxWidth,
			boolean forceColumnWidth) {
		this.maxWidth = maxWidth;
		this.forceColumnWidth = forceColumnWidth;
	}

	public BoundWidget get() {
		return new ExpandableLabel(maxWidth);
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public boolean isForceColumnWidth() {
		return forceColumnWidth;
	}
}