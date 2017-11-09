package cc.alcina.framework.common.client.logic.domain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.util.CommonUtils;

public class HiliHelper {
	public static int compare(HasIdAndLocalId o1, HasIdAndLocalId o2) {
		int i = o1.getClass().getName().compareTo(o2.getClass().getName());
		if (i != 0) {
			return i;
		}
		i = CommonUtils.compareLongs(o1.getId(), o2.getId());
		if (i != 0) {
			return i;
		}
		i = CommonUtils.compareLongs(o1.getLocalId(), o2.getLocalId());
		if (i != 0) {
			return i;
		}
		return CommonUtils.compareInts(o1.hashCode(), o2.hashCode());
	}
	public static int compareLocalsHigh(HasIdAndLocalId o1, HasIdAndLocalId o2) {
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

	public static String toIdString(Collection<? extends HasIdAndLocalId> hilis) {
		StringBuffer sb = new StringBuffer();
		for (HasIdAndLocalId hili : hilis) {
			if (sb.length() != 0) {
				sb.append(",");
			}
			sb.append(hili.getId());
		}
		return sb.toString();
	}

	public static int compareNoLocals(HasIdAndLocalId o1, HasIdAndLocalId o2) {
		int i = o1.getClass().getName().compareTo(o2.getClass().getName());
		if (i != 0) {
			return i;
		}
		return CommonUtils.compareLongs(o1.getId(), o2.getId());
	}

	public static boolean equals(HasIdAndLocalId o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return false;
		}
		if (o1.getClass() != o2.getClass()) {
			return false;
		}
		if (o1.getId() == 0 && o1.getLocalId() == 0) {
			return o1 == o2;
		}
		HasIdAndLocalId hili = (HasIdAndLocalId) o2;
		if (o1.getId() != 0 && o1.getId() == hili.getId()) {
			return true;
		}
		return (hili.getId() == o1.getId()
				&& hili.getLocalId() == o1.getLocalId());
	}

	public static Set<Long>
			toIdSet(Collection<? extends HasIdAndLocalId> hilis) {
		return toIdSet(hilis, new LinkedHashSet<Long>());
	}

	public static Set<Long> toIdSet(Collection<? extends HasIdAndLocalId> hilis,
			Set<Long> set) {
		for (HasIdAndLocalId hili : hilis) {
			set.add(hili.getId());
		}
		return set;
	}

	public static <T extends HasIdAndLocalId> Map<Long, T>
			toIdMap(Collection<T> hilis) {
		return (Map<Long, T>) CollectionFilters
				.map((Collection<HasIdAndLocalId>) hilis, new HiliToIdMapper());
	}

	public static String asDomainPoint(HasId hi) {
		if (hi == null) {
			return null;
		}
		if (hi instanceof HasIdAndLocalId) {
			HasIdAndLocalId hili = (HasIdAndLocalId) hi;
			return CommonUtils.formatJ("%s : %s / %s",
					CommonUtils.simpleClassName(hili.getClass()), hili.getId(),
					hili.getLocalId());
		}
		return CommonUtils.formatJ("%s : %s ",
				CommonUtils.simpleClassName(hi.getClass()), hi.getId());
	}

	public static Long getIdOrNull(HasId hi) {
		return hi == null ? null : hi.getId();
	}

	public static long getIdOrZero(HasId hi) {
		return hi == null ? 0 : hi.getId();
	}

	public static <T extends HasId> T getById(Collection<T> values, long id) {
		for (T value : values) {
			if (value.getId() == id) {
				return value;
			}
		}
		return null;
	}

	public static <T extends HasIdAndLocalId> SortedSet<T>
			combineAndOrderById(boolean reverse, Collection<T>... collections) {
		TreeSet<T> join = new TreeSet<T>();
		for (Collection<T> collection : collections) {
			join.addAll(collection);
		}
		return reverse ? join.descendingSet() : join;
	}

	public static String strGetIdOrZero(HasId hasId) {
		return String.valueOf(getIdOrZero(hasId));
	}
}