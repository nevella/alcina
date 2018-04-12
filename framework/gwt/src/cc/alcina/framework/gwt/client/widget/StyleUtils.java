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

package cc.alcina.framework.gwt.client.widget;

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;

/**
 *
 * @author Nick Reddel
 */

 public class StyleUtils {
	public static void floatLeft(Widget w) {
		w.getElement().getStyle().setProperty("float", "left");
		w.getElement().getStyle().setProperty("cssFloat", "left");
		w.getElement().getStyle().setProperty("styleFloat", "left");
	}

	public static void floatRight(Widget w) {
		w.getElement().getStyle().setProperty("float", "right");
		w.getElement().getStyle().setProperty("cssFloat", "right");
		w.getElement().getStyle().setProperty("styleFloat", "right");
	}

	public static void setWordWrap(Widget w, boolean wrap) {
		w.getElement().getStyle().setProperty("whiteSpace",
				wrap ? "normal" : "nowrap");
	}

	
}
