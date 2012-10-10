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

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Tree.Resources;

/**
 * 
 * @author Nick Reddel
 */
public interface StandardDataImages extends ClientBundle, Resources {
	ImageResource backupRoot();

	ImageResource folder();

	ImageResource file();

	ImageResource treeLeaf();

	ImageResource transparent();

	ImageResource maximise();

	ImageResource collapse();

	ImageResource minimise();

	ImageResource minimise2();

	ImageResource minimise2over();

	ImageResource maximise2();

	ImageResource maximise2over();

	ImageResource error();

	ImageResource errorSmall();

	ImageResource warning();

	ImageResource info();

	ImageResource deleteItem();
	
	ImageResource downGrey();
}
