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
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.widget.FileSelector;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class FileSelectorCustomiser implements Customiser {
	public static final String ACCEPT_FILTER = "ACCEPT_FILTER";
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		NamedParameter[] parameters = info.parameters();
		String accept = NamedParameter.Support.stringValue(parameters,
				ACCEPT_FILTER, "");
		return new FileSelectorProvider(editable,accept);
	}

	public static class FileSelectorProvider implements BoundWidgetProvider {
		private boolean editable;
		private String accept;

		public FileSelectorProvider(boolean editable,String accept) {
			this.editable = editable;
			this.accept=accept;
		}

		public BoundWidget get() {
			FileSelector selector = new FileSelector();
			selector.setEnabled(editable);
			selector.setAccept(accept);
			return selector;
		}
	}
}
