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

import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 *         <p>
 *         See JVM bugs, particularly
 *         http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4477877
 *         </p>
 *         <p>
 *         For JVMIntrospector (not compiled gwt) to work, make sure the
 *         <b>getValue, setValue</b> overrides in children are the <i>first</i>
 *         bean methods in source - i.e.<br>
 *         getValue()<br>
 *         setValue()<br>
 *         getMyEnum()<br>
 *         setMyEnum()<br>
 *         </p>
 */
public abstract class EnumCriterion<E extends Enum> extends SearchCriterion
		implements HasWithNull {
	private boolean withNull = true;

	public EnumCriterion() {
	}

	public boolean equivalentTo(SearchCriterion other) {
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		EnumCriterion otherImpl = (EnumCriterion) other;
		return otherImpl.getDirection() == getDirection()
				&& otherImpl.isWithNull() == isWithNull()
				&& otherImpl.getValue() == getValue();
	}

	/**
	 * If the enum is serialised in the db as a string, set to true
	 */
	protected boolean valueAsString() {
		return false;
	}

	public EnumCriterion(String criteriaDisplayName, boolean withNull) {
		super(criteriaDisplayName);
		this.withNull = withNull;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		E value = getValue();
		if (value != null
				&& !CommonUtils.isNullOrEmpty(getTargetPropertyName())) {
			result.eql = "t." + getTargetPropertyName() + " = ? ";
			result.parameters.add(valueAsString() ? value.toString() : value);
		}
		return result;
	}

	// @Override
	// public boolean equals(Object obj) {
	// if (obj instanceof EnumCriterion) {
	// EnumCriterion ec = (EnumCriterion) obj;
	// return getClass() == ec.getClass() && ec.getValue() == getValue();
	// }
	// return super.equals(obj);
	// }
	//
	// @Override
	// public int hashCode() {
	// E value = getValue();
	// return getClass().hashCode() ^ (value == null ? 0 : value.hashCode());
	// }
	@XmlTransient
	public abstract E getValue();

	/**
	 * add property change firing to the subclass implementation, if you care
	 */
	public abstract void setValue(E value);

	public void setWithNull(boolean withNull) {
		this.withNull = withNull;
	}

	public boolean isWithNull() {
		return withNull;
	}
}