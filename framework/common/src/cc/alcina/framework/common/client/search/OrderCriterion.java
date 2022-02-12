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

import java.util.Comparator;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public class OrderCriterion extends SearchCriterion {
	private Direction direction = Direction.ASCENDING;

	public String addDirection(String criterionName) {
		return criterionName == null || getDirection() == Direction.ASCENDING
				? criterionName
				: criterionName + " (reverse)";
	}

	@PropertySerialization(path = "dir", defaultProperty = true, serializer = DirectionSerializer.class)
	public Direction getDirection() {
		return this.direction;
	}

	public Comparator reverseIfDescending(Comparator cmp) {
		return getDirection() == Direction.DESCENDING ? cmp.reversed() : cmp;
	}

	public void setDirection(Direction direction) {
		Direction old_direction = this.direction;
		this.direction = direction;
		propertyChangeSupport().firePropertyChange("direction", old_direction,
				direction);
	}

	public OrderCriterion withDirection(Direction direction) {
		setDirection(direction);
		return this;
	}

	@ClientInstantiable
	public static class DirectionSerializer
			implements PropertySerialization.Serializer<Direction> {
		@Override
		public Direction deserializeValue(String value) {
			Objects.requireNonNull(value);
			switch (value) {
			case "asc":
				return Direction.ASCENDING;
			case "desc":
				return Direction.DESCENDING;
			default:
				return CommonUtils.getEnumValueOrNull(Direction.class, value,
						true, null);
			}
		}

		@Override
		public boolean elideDefaultValues(Direction t) {
			return false;
		}

		@Override
		public String serializeValue(Direction t) {
			Objects.requireNonNull(t);
			switch (t) {
			case ASCENDING:
				return "asc";
			case DESCENDING:
				return "desc";
			default:
				throw new UnsupportedOperationException();
			}
		}
	}
}
