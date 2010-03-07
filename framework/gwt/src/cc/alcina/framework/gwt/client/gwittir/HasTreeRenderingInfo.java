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

package cc.alcina.framework.gwt.client.gwittir;

import java.util.Collection;

import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;

import com.totsp.gwittir.client.beans.Bindable;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public interface HasTreeRenderingInfo extends Bindable {
	public Collection<? extends HasTreeRenderingInfo> renderableChildren();
	public BoundWidgetProvider renderCustomiser();
	public CollectionProvider collectionProvider();
	public String renderCss();
	public String getDisplayName();
	public String renderablePropertyName();
	public String hint();
	public enum RenderInstruction{
		AS_WIDGET, AS_TITLE, AS_WIDGET_WITH_TITLE_IF_MORE_THAN_ONE_CHILD,IGNORE_AND_DESCEND,NO_RENDER
	}
	public RenderInstruction renderInstruction();
	public boolean renderChildrenHorizontally();
}
