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
package cc.alcina.framework.common.client.logic.domain;

import java.util.Comparator;
import java.util.function.Function;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.csobjects.AbstractDomainBase;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * Base interface for classes which can be handled by the
 * {@link cc.alcina.framework.common.client.logic.domaintransform.TransformManager
 * TransformManager }. Note that the only id type supported is <code>long</code>
 * .
 * 
 * @author Nick Reddel
 */
public interface HasIdAndLocalId extends HasId {
	public static Function<?, HasIdAndLocalId> caster() {
		return o -> (HasIdAndLocalId) o;
	}

	public static long provideUnpackedLocalId(long packedLocalId) {
		return (-packedLocalId) & 0x7FFFFFFF;
	}

	/**
	 * Used for object referencing within a client domain. Generated from a
	 * thread-safe increment counter (one counter per domain, not
	 * per-object-type). Can be 'packed' with the lower 31 bits of the
	 * clientInstance id (and negated) to make an effectively globally unique id
	 * (for the jvm lifetime) that can be used in the same set as the id field
	 * (per-class, db-generated) ids
	 */
	public long getLocalId();

	public void setLocalId(long localId);

	default void delete() {
		Domain.delete(this);
	}

	default <V extends AbstractDomainBase> AbstractDomainBase<V>.DomainSupport
			domain() {
		return ((AbstractDomainBase<V>) this).domain();
	}

	default <V extends HasIdAndLocalId> V provideDomainIdentity() {
		return (V) this;
	}

	default Class<? extends HasIdAndLocalId> provideEntityClass() {
		return getClass();
	}

	default boolean provideIsNonDomain() {
		return getId() == 0 && getLocalId() == 0;
	}

	default String provideStringId() {
		return getId() == 0 ? null : String.valueOf(getId());
	}

	default long provideTransactionalId() {
		if (getId() == 0) {
			long clientInstanceId = CommonUtils
					.lv(PermissionsManager.get().getClientInstanceId());
			return -((clientInstanceId << 32) + getLocalId());
		} else {
			return getId();
		}
	}

	default boolean provideWasPersisted() {
		return getId() != 0;
	}

	default String toStringHili() {
		return new HiliLocator(this).toString();
	}

	public static class HiliByIdFilter
			implements CollectionFilter<HasIdAndLocalId> {
		private final boolean allowAllExceptId;

		private final long id;

		public HiliByIdFilter(long id, boolean allowAllExceptId) {
			this.id = id;
			this.allowAllExceptId = allowAllExceptId;
		}

		@Override
		public boolean allow(HasIdAndLocalId o) {
			return o != null && (o.getId() == id ^ allowAllExceptId);
		}
	}

	public static class HiliComparator implements Comparator<HasIdAndLocalId> {
		public static final HiliComparator INSTANCE = new HiliComparator();

		public static final Comparator<HasIdAndLocalId> REVERSED_INSTANCE = new HiliComparator()
				.reversed();

		@Override
		public int compare(HasIdAndLocalId o1, HasIdAndLocalId o2) {
			return HiliHelper.compare(o1, o2);
		}
	}

	public static class HiliComparatorLocalsHigh
			implements Comparator<HasIdAndLocalId> {
		public static final HiliComparatorLocalsHigh INSTANCE = new HiliComparatorLocalsHigh();

		@Override
		public int compare(HasIdAndLocalId o1, HasIdAndLocalId o2) {
			return HiliHelper.compareLocalsHigh(o1, o2);
		}
	}

	public static class HiliComparatorPreferLocals
			implements Comparator<HasIdAndLocalId> {
		@Override
		public int compare(HasIdAndLocalId o1, HasIdAndLocalId o2) {
			int i = o1.getClass().getName().compareTo(o2.getClass().getName());
			if (i != 0) {
				return i;
			}
			i = CommonUtils.compareLongs(o1.getLocalId(), o2.getLocalId());
			if (i != 0) {
				return i;
			}
			i = CommonUtils.compareLongs(o1.getId(), o2.getId());
			if (i != 0) {
				return i;
			}
			return CommonUtils.compareInts(o1.hashCode(), o2.hashCode());
		}
	}

	public static class HiliNoLocalComparator
			implements Comparator<HasIdAndLocalId> {
		public static final HiliNoLocalComparator INSTANCE = new HiliNoLocalComparator();

		@Override
		public int compare(HasIdAndLocalId o1, HasIdAndLocalId o2) {
			return HiliHelper.compareNoLocals(o1, o2);
		}
	}
}
