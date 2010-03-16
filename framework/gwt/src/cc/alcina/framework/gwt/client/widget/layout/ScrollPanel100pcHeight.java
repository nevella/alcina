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

import java.util.Iterator;

import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author Nick Reddel
 */

 public class ScrollPanel100pcHeight extends ScrollPanel implements
		HasLayoutInfo {
	public ScrollPanel100pcHeight(Widget child) {
		super(child);
		setHeight("300px");
		/*this averts overlong children forcing the parent to lengthen, which
		 * means the scroll panel will never be laid out correctly 
		 */
	}

	public LayoutInfo getLayoutInfo() {
		return new LayoutInfo() {
			@Override
			/*
			 * blocks checking (potentially long) child list
			 */
			public Iterator<Widget> getLayoutWidgets() {
				// TODO Auto-generated method stub
				return super.getLayoutWidgets();
			}

			public boolean to100percentOfAvailableHeight() {
				return true;
			}
		};
	}
}