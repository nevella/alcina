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

	ImageResource bubbleArrow();

	ImageResource collapse();

	ImageResource deleteItem();

	ImageResource downGrey();

	ImageResource error();

	ImageResource errorSmall();

	ImageResource file();

	ImageResource folder();

	ImageResource info();

	ImageResource maximise();

	ImageResource maximise2();

	ImageResource maximise2over();

	ImageResource minimise();

	ImageResource minimise2();

	ImageResource minimise2over();

	ImageResource transparent();

	ImageResource treeLeaf();

	ImageResource warning();
}
