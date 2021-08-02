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

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;

/**
 *
 * @author Nick Reddel
 */
public abstract class OrderGroup extends CriteriaGroup<OrderCriterion> {
	@Override
	public void addCriterion(OrderCriterion criterion) {
		if (getCriteria().size() > 0) {
			Ax.err("Warn - adding order criterion to group with existing criterion");
		}
		super.addCriterion(criterion);
	}

	@Override
	public Class entityClass() {
		return null;
	}

	@XmlTransient
	@AlcinaTransient
	public OrderCriterion getSoleCriterion() {
		if (getCriteria().iterator().hasNext()) {
			return getCriteria().iterator().next();
		}
		return null;
	}

	public void setSoleCriterion(OrderCriterion soleCriterion) {
		OrderCriterion old_soleCriterion = getSoleCriterion();
		getCriteria().clear();
		if (soleCriterion != null) {
			getCriteria().add(soleCriterion);
		}
		propertyChangeSupport().firePropertyChange("soleCriterion",
				old_soleCriterion, soleCriterion);
	}

	@Override
	public TreeSerializable.Customiser treeSerializationCustomiser() {
		return new Customiser(this);
	}

	@Override
	public /**
			 * Either subclass, or rely on property mappings. No real risk of
			 * information leakage 'ere
			 */
	String validatePermissions() {
		return null;
	}

	private static class Customiser
			extends TreeSerializable.Customiser<OrderGroup> {
		public Customiser(OrderGroup serializable) {
			super(serializable);
		}

		@Override
		public void onBeforeTreeSerialize() {
			Preconditions.checkState(
					serializable.getCriteria().size() <= 1 || LooseContext
							.is(TreeSerializable.CONTEXT_IGNORE_CUSTOM_CHECKS));
		}
	}
}
