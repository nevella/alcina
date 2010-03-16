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

package cc.alcina.framework.common.client.search;

import java.util.Collection;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter;


/**
 *
 * @author Nick Reddel
 */

 public class LocalSearchDefinition extends SearchDefinition {
	protected CollectionFilter buildFilter(){
		return null;
	}
	@SuppressWarnings("unchecked")
	public Collection search(){
		CollectionFilter filter = buildFilter();
		return TransformManager.get().filter(getResultClass(), filter);
	}
	public void setResultClass(Class resultClass) {
		this.resultClass = resultClass;
	}
	public Class getResultClass() {
		return resultClass;
	}
	private Class resultClass;
}
