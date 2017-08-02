package cc.alcina.framework.entity.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface IToCsvRow<T> extends Function<T, List<String>> {
	List<String> headers();

	default  void setCustom(Object custom) {
	}

	default List<ArrayList<String>> doConvert(List<T> objects){
		return (List) objects
		.stream().map(r -> apply(r))
		.collect(Collectors.toList());
	}

	default String suggestFileName(String prefix){
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss");
		return String.format("%s-%s", prefix,df.format(new Date()));
	}
}