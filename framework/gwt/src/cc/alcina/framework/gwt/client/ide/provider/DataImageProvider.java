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


import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;

import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 *
 * @author Nick Reddel
 */

 public class DataImageProvider {
	protected DataImageProvider() {
	}

	private static DataImageProvider theInstance;
	public static void register (DataImageProvider p){
		theInstance = p;
	}
	public AbstractImagePrototype getByName(String s){
		return null;
	}
	public StandardDataImages getDataImages(){
		return null;
	}
	public static DataImageProvider get() {
		if (theInstance == null) {
			theInstance = new DataImageProvider();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}
}
