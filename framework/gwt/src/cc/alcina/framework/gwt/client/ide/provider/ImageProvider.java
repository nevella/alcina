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

package cc.alcina.framework.gwt.client.ide.provider;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;
/**
 * Images specific to the current app
 * @author nick@alcina.cc
 *
 */
public abstract class ImageProvider {
	protected ImageProvider() {
	}

	private static ImageProvider theInstance;
	public static void register (ImageProvider p){
		theInstance = p;
	}
	public AbstractImagePrototype getByName(String s){
		return null;
	}
	public static ImageProvider get() {
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}
	public abstract Image getTransparent();
}
