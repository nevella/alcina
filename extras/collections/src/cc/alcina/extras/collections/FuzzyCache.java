package cc.alcina.extras.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FloatPair;


public class FuzzyCache<I, O> {
	public static ResultConvertor<List, List> LIST_CONVERTER = new ResultConvertor<List, List>() {
		@Override
		public List convert(SortedMap<Float, List> map) {
			List result = new ArrayList();
			for (List l : map.values()) {
				result.addAll(l);
			}
			return result;
		}
	};

	static ResultConvertor<SortedMap, SortedMap> SORTED_MAP_CONVERTER = new ResultConvertor<SortedMap, SortedMap>() {
		@Override
		public SortedMap convert(SortedMap<Float, SortedMap> map) {
			SortedMap result = new TreeMap();
			for (SortedMap m : map.values()) {
				result.putAll(m);
			}
			return result;
		}
	};

	public static ResultConvertor<SortedMap<Object, Integer>, SortedMap<Object, Integer>> SORTED_MAP_CONVERTER_INT = new ResultConvertor<SortedMap<Object, Integer>, SortedMap<Object, Integer>>() {
		@Override
		public SortedMap<Object, Integer>
				convert(SortedMap<Float, SortedMap<Object, Integer>> map) {
			SortedMap<Object, Integer> result = new TreeMap();
			for (SortedMap<Object, Integer> m : map.values()) {
				for (Object key : m.keySet()) {
					Integer i = m.get(key);
					if (!result.containsKey(key)) {
						result.put(key, 0);
					}
					result.put(key, result.get(key) + i);
				}
			}
			return result;
		}
	};

	public static Integer countNear(SortedMap<Float, Integer> map, float around,
			float fuzz) {
		SortedMap<Float, Integer> subMap = map.subMap(around - fuzz,
				around + fuzz);
		int result = 0;
		for (Integer v : subMap.values()) {
			result += v;
		}
		return result;
	}

	public static Float weightedMean(SortedMap<Float, List> map) {
		float total = 0;
		float count = 0;
		for (Float key : map.keySet()) {
			List list = map.get(key);
			count += list.size();
			total += key * list.size();
		}
		return total / count;
	}

	private final SortedMap<Float, I> map;

	private final Float fuzz;

	private final ResultConvertor<I, O> converter;

	private List<FloatPairWithWeight> sortedRanges;

	public FuzzyCache(SortedMap<Float, I> map, Float fuzz,
			ResultConvertor<I, O> converter) {
		this.map = map;
		this.fuzz = fuzz;
		this.converter = converter;
	}

	public O getFuzzy(Float around) {
		SortedMap<Float, I> subMap = map.subMap(around - fuzz, around + fuzz);
		return converter.convert(subMap);
	}

	public List<FloatPairWithWeight> getSortedRanges() {
		return this.sortedRanges;
	}

	public int indexInRanges(float f) {
		int index = 0;
		for (FloatPairWithWeight fp : sortedRanges) {
			if (fp.range.contains(f)) {
				break;
			}
			index++;
		}
		return index;
	}

	public void sortRanges(int steps) {
		sortedRanges = new ArrayList<FuzzyCache.FloatPairWithWeight>();
		if (map.size() <= 1) {
			return;
		}
		float last = map.lastKey();
		float step = Math.max((last - map.firstKey()) / steps, (float) 0.0001);
		float firstMatched = Float.MIN_VALUE;
		float lastMatched = Float.MIN_VALUE;
		int total = 0;
		for (float f = map.firstKey(); f <= last; f += step) {
			SortedMap<Float, I> subMap = map.subMap(f, f + step);
			int count = 0;
			for (I v : subMap.values()) {
				if (v instanceof Integer) {
					count += (Integer) v;
				} else if (v instanceof Collection) {
					count += ((Collection) v).size();
				} else {
					count++;
				}
			}
			if (count != 0) {
				if (firstMatched == Float.MIN_VALUE) {
					firstMatched = f;
				}
				lastMatched = f;
				total += count;
			}
			if (firstMatched != Float.MIN_VALUE
					&& (count == 0 || f + step > last)) {
				FloatPair pair = new FloatPair(firstMatched,
						lastMatched + (count == 0 ? 0 : step));
				sortedRanges.add(new FloatPairWithWeight(total, pair));
			}
			if (count == 0) {
				firstMatched = Float.MIN_VALUE;
				lastMatched = Float.MIN_VALUE;
				total = 0;
			}
		}
		Collections.sort(sortedRanges);
	}

	public static class FloatPairWithWeight
			implements Comparable<FloatPairWithWeight> {
		public int count;

		public FloatPair range;

		public FloatPairWithWeight(int count, FloatPair range) {
			this.count = count;
			this.range = range;
		}

		@Override
		public int compareTo(FloatPairWithWeight o) {
			return -CommonUtils.compareInts(count, o.count);
		}

		public int getCount() {
			return this.count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		@Override
		public String toString() {
			return range + ":" + count;
		}
	}

	public interface ResultConvertor<I, O> {
		O convert(SortedMap<Float, I> map);
	}
}
