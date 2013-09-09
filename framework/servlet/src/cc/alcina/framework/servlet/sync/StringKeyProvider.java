package cc.alcina.framework.servlet.sync;

import java.util.List;

public interface StringKeyProvider<T>  {
	public String firstKey(T object);
	public List<String> allKeys(T object);
}
