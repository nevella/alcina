package cc.alcina.extras.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class FuzzyCache<I, O> {
	private final SortedMap<Float, I> map;

	private final Float fuzz;

	private final ResultConvertor<I, O> converter;

	public FuzzyCache(SortedMap<Float, I> map, Float fuzz,
			ResultConvertor<I, O> converter) {
		this.map = map;
		this.fuzz = fuzz;
		this.converter = converter;
	}

	interface ResultConvertor<I, O> {
		O convert(SortedMap<Float, I> map);
	}

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
			for(SortedMap m:map.values()){
				
				result.putAll(m);
			}
			return result;
		}
	};
	public static ResultConvertor<SortedMap<Object,Integer>, SortedMap<Object,Integer>> SORTED_MAP_CONVERTER_INT = new ResultConvertor<SortedMap<Object,Integer>, SortedMap<Object,Integer>>() {
		@Override
		public SortedMap<Object,Integer> convert(SortedMap<Float, SortedMap<Object,Integer>> map) {
			SortedMap<Object,Integer> result = new TreeMap();
			for(SortedMap<Object,Integer> m:map.values()){
				for(Object key:m.keySet()){
					Integer i = m.get(key);
					if(!result.containsKey(key)){
						result.put(key, 0);
					}
					result.put(key,result.get(key)+i);
				}
			}
			return result;
		}
	};

	public O getFuzzy(Float around) {
		SortedMap<Float, I> subMap = map.subMap(around - fuzz, around + fuzz);
		return converter.convert(subMap);
	}

	public static Integer countNear(SortedMap<Float, Integer> map, float around,
			float fuzz) {
		SortedMap<Float, Integer> subMap = map.subMap(around - fuzz, around + fuzz);
		int result=0;
		for(Integer v:subMap.values()){
			result+=v;
		}
		return result;
	}
}
