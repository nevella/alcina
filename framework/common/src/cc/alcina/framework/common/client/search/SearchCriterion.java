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

import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;

@BeanInfo(displayNamePropertyName = "displayName", allPropertiesVisualisable = true)
@ObjectPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.EVERYONE))
@RegistryLocation(registryPoint = JaxbContextRegistration.class)
public class SearchCriterion extends BaseBindable implements TreeRenderable {
	// TODO: great big injection hole here - should be checked server-side
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

	public String getDisplayName() {
		return this.displayName;
	}

	public String getTargetPropertyName() {
		return targetPropertyName;
	}

	public void setDirection(Direction direction) {
		Direction old_direction = this.direction;
		this.direction = direction;
		propertyChangeSupport.firePropertyChange("direction", old_direction,
				direction);
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
	 * Can also apply to things like date criteria, not just order - so leave
	 * here rather than in OrderCriterion
	 * 
	 * @author nick@alcina.cc
	 * 
	 */
	public enum Direction {
		ASCENDING, DESCENDING
	}
}
