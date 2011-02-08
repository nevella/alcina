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

package cc.alcina.framework.gwt.client.stdlayout.image;


import cc.alcina.framework.gwt.client.ide.provider.DataImageProvider;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 *
 * @author Nick Reddel
 */

 public class StandardDataImageProvider extends DataImageProvider {
	private StandardDataImages dataImages;

	public AbstractImagePrototype getByName(String s) {
		s = (s == null) ? "" : s;
		if (s.equals("leaf")) {
			return AbstractImagePrototype.create(dataImages.file());
		}
		return AbstractImagePrototype.create(dataImages.file());
	}

	@Override
	public StandardDataImages getDataImages() {
		return dataImages;
	}

	public static StandardDataImageProvider get() {
		StandardDataImageProvider dip = new StandardDataImageProvider();
		dip.dataImages = GWT.create(StandardDataImages.class);
		return dip;
	}
}
