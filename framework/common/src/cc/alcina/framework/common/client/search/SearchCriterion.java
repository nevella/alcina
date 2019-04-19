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
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasReflectiveEquivalence;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

@Bean(displayNamePropertyName = "displayName", allPropertiesVisualisable = true)
@ObjectPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.EVERYONE))
@RegistryLocation(registryPoint = JaxbContextRegistration.class)
public abstract class SearchCriterion extends BaseBindable
        implements TreeRenderable, HasReflectiveEquivalence<SearchCriterion> {
    public static final transient String CONTEXT_ENSURE_DISPLAY_NAME = SearchCriterion.class
            + ".CONTEXT_ENSURE_DISPLAY_NAME";

    // TODO: great big injection hole here - should be checked server-side
    // FIXED: - transient, and set in the server validation phase
    private transient String targetPropertyName;

    private Direction direction = Direction.ASCENDING;

    private String displayName;

    private StandardSearchOperator operator;

    public SearchCriterion() {
    }

    public SearchCriterion(String displayName) {
        this.displayName = displayName;
    }

    public CollectionProvider collectionProvider() {
        return null;
    }

    public boolean emptyCriterion() {
        if ((this instanceof HasValue)) {
            Object value = ((HasValue) this).getValue();
            if (value instanceof Collection) {
                return ((Collection) value).isEmpty();
            } else {
                return value == null;
            }
        }
        return false;
    }

    public EqlWithParameters eql() {
        return null;
    }

    public Direction getDirection() {
        return this.direction;
    }

    @Override
    public String getDisplayName() {
        if (CommonUtils.isNullOrEmpty(displayName)
                && LooseContext.is(CONTEXT_ENSURE_DISPLAY_NAME)) {
            return CommonUtils.simpleClassName(getClass());
        }
        return this.displayName;
    }

    public StandardSearchOperator getOperator() {
        return this.operator;
    }

    public String getTargetPropertyName() {
        return targetPropertyName;
    }

    public void setDirection(Direction direction) {
        Direction old_direction = this.direction;
        this.direction = direction;
        propertyChangeSupport().firePropertyChange("direction", old_direction,
                direction);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setOperator(StandardSearchOperator operator) {
        StandardSearchOperator old_operator = this.operator;
        this.operator = operator;
        propertyChangeSupport().firePropertyChange("operator", old_operator,
                operator);
    }

    public void setTargetPropertyName(String propertyName) {
        this.targetPropertyName = propertyName;
    }

    public String toHtml() {
        return toString();
    }

    public SearchCriterion withDirection(Direction direction) {
        setDirection(direction);
        return this;
    }

    public SearchCriterion withOperator(StandardSearchOperator operator) {
        setOperator(operator);
        return this;
    }

    protected String targetPropertyNameWithTable() {
        String targetPropertyName = getTargetPropertyName();
        if (targetPropertyName == null || targetPropertyName.contains(".")) {
            return targetPropertyName;
        }
        return "t." + targetPropertyName;
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
