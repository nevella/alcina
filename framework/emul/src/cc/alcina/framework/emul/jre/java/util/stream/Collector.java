package java.util.stream;
import java.util.function.Function;

public interface Collector<T,A,R> {
	public R collect(Stream<T> stream);
}
