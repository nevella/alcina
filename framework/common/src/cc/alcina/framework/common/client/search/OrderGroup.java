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

import javax.xml.bind.annotation.XmlTransient;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;

/**
 *
 * @author Nick Reddel
 */
public abstract class OrderGroup extends CriteriaGroup<OrderCriterion> {

    @XmlTransient
    @AlcinaTransient
    public OrderCriterion getSoleCriterion() {
        if (getCriteria().iterator().hasNext()) {
            return getCriteria().iterator().next();
        }
        return null;
    }

    @Override
    public Class getEntityClass() {
        return null;
    }

    public void setSoleCriterion(OrderCriterion soleCriterion) {
        OrderCriterion old_soleCriterion = getSoleCriterion();
        getCriteria().clear();
        if (soleCriterion != null) {
            getCriteria().add(soleCriterion);
        }
        propertyChangeSupport().firePropertyChange("soleCriterion", old_soleCriterion, soleCriterion);
    }

    @Override
    public /**
	 * Either subclass, or rely on property mappings. No real risk of information leakage 'ere
	 */
    String validatePermissions() {
        return null;
    }
}
