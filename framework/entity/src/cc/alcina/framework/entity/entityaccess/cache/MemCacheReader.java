package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.SliceProcessor;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.projection.GraphProjections;

public abstract class MemCacheReader<I, O> {
	public O read(I input) {
		try {
			AlcinaMemCache.get().lock(false);
			LooseContext.pushWithBoolean(AlcinaMemCache.CONTEXT_NO_LOCKS);
			return read0(input);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			LooseContext.pop();
			AlcinaMemCache.get().unlock(false);
		}
	}

	protected abstract O read0(I input) throws Exception;

	public static <T> T get(Supplier<T> supplier) {
		return new MemCacheReader<Void, T>() {
			@Override
			protected T read0(Void input) throws Exception {
				return supplier.get();
			}
		}.read(null);
	}

	public static <T> List<T> projectCollectionWithSliceLock(
			GraphProjections projections, List<T> data, int sliceSize) {
		List<T> result = new ArrayList<>();
		/*
		 * A note on coherency - we're reusing the filters, so the datafilter
		 * will cache mappings from memcache objects to projected. Even if the
		 * memcache object changes (between slices), we'll still have a
		 * consistent copy if it was reachable prior to the change.
		 * 
		 * Basically - it makes sense
		 */
		new SliceProcessor<T>().process(data, sliceSize, (sublist, idx) -> {
			MetricLogging.get().start("slice-projection");
			result.addAll(get(() -> projections.project(new ArrayList<T>(
					sublist))));
			MetricLogging.get().end("slice-projection");
		});
		return result;
	}
}
