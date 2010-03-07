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

import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.gwt.client.gwittir.HasTreeRenderingInfo;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;

import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

@BeanInfo(displayNamePropertyName = "displayName", allPropertiesVisualisable = true)
@ObjectPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.EVERYONE))
@RegistryLocation(registryPoint = JaxbContextRegistration.class)
public class SearchCriterion extends BaseBindable implements
		HasTreeRenderingInfo {
	//TODO: great big injection hole here -  should be checked server-side
	private String targetPropertyName;

	private Direction direction = Direction.ASCENDING;

	private String displayName;

	public SearchCriterion() {
	}

	public SearchCriterion(String displayName) {
		this(displayName, null);
	}

	public SearchCriterion(String displayName, String propertyName) {
		this.displayName = displayName;
		this.targetPropertyName = propertyName;
	}

	public CollectionProvider collectionProvider() {
		return null;
	}

	public EqlWithParameters eql() {
		return null;
	}

	public Direction getDirection() {
		return this.direction;
	}

	// $name, $displayName, $table, $sql = "", $criteriaParameter = ""
	public String getDisplayName() {
		return this.displayName;
	}

	public String getTargetPropertyName() {
		return targetPropertyName;
	}

	public String hint() {
		return null;
	}

	public Collection<? extends HasTreeRenderingInfo> renderableChildren() {
		return null;
	}

	public String renderablePropertyName() {
		return null;
	}

	public boolean renderChildrenHorizontally() {
		return false;
	}

	public String renderCss() {
		return null;
	}

	public BoundWidgetProvider renderCustomiser() {
		return null;
	}

	public RenderInstruction renderInstruction() {
		return RenderInstruction.AS_WIDGET_WITH_TITLE_IF_MORE_THAN_ONE_CHILD;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setTargetPropertyName(String propertyName) {
		this.targetPropertyName = propertyName;
	}

	public String toHtml() {
		return toString();
	}

	/**
	 * Order should really be defined in OrderCriterion subclass, but easier
	 * here
	 * 
	 * @author nreddel@barnet.com.au
	 * 
	 */
	public enum Direction {
		ASCENDING, DESCENDING
	}
}
