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
package cc.alcina.framework.gwt.client.widget.layout;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author Nick Reddel
 */
public interface HasLayoutInfo {
	public static final LayoutInfo TO_100_PCT_HEIGHT = new LayoutInfo() {
		@Override
		public boolean to100percentOfAvailableHeight() {
			return true;
		}
	};

	public LayoutInfo getLayoutInfo();

	public static class LayoutInfo {
		public void afterLayout() {
		}

		public void beforeLayout() {
		}

		public int getAdjustHeight() {
			return 0;
		}

		public int getAdjustWidth() {
			return 0;
		}

		public int getClientAdjustHeight() {
			return 0;
		}

		public int getClientAdjustWidth() {
			return 0;
		}

		public Iterator<Widget> getLayoutWidgets() {
			return new ArrayList<Widget>().iterator();
		}

		/**
		 * These will be resized to fit...?
		 */
		public Iterator<Widget> getWidgetsToResize() {
			return new ArrayList<Widget>().iterator();
		}

		public boolean ignoreSiblingsForHeight() {
			return false;
		}

		public boolean ignoreSiblingsForWidth() {
			return false;
		}

		public boolean to100percentOfAvailableHeight() {
			return false;
		}

		public boolean to100percentOfAvailableWidth() {
			return false;
		}

		public boolean useBestOffsetForParentHeight() {
			return true;
		}

		public boolean useBestOffsetForParentWidth() {
			return true;
		}
	}
}
