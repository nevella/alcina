package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.SliceProcessor;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.ThrowingSupplier;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.projection.GraphProjections;

public abstract class DomainReader<I, O> {
	public static <T> T get(ThrowingSupplier<T> supplier) {
		return new DomainReader<Void, T>() {
			@Override
			protected T read0(Void input) throws Exception {
				return supplier.get();
			}
		}.read(null);
	}

	public static <T> T getThrowing(ThrowingSupplier<T> supplier)
			throws Exception {
		return new DomainReader<Void, T>() {
			@Override
			protected T read0(Void input) throws Exception {
				return supplier.get();
			}
		}.readThrowing(null);
	}

	public static <T> List<T> projectCollectionWithSliceLock(
			GraphProjections projections, List<T> data, int sliceSize) {
		List<T> result = new ArrayList<>();
		/*
		 * A note on coherency - we're reusing the projection filters, so the
		 * datafilter will cache mappings from domainStore objects to projected.
		 * Even if the domainStore object changes (between slices), we'll still
		 * have a consistent copy if it was reachable prior to the change.
		 * 
		 * Basically - it makes sense
		 */
		new SliceProcessor<T>().process(data, sliceSize, (sublist, idx) -> {
			MetricLogging.get().start("slice-projection");
			result.addAll(
					get(() -> projections.project(new ArrayList<T>(sublist))));
			MetricLogging.get().end("slice-projection");
		});
		return result;
	}

	public O read(I input) {
		int initialDepth = LooseContext.depth();
		boolean noLocksWasSet = LooseContext.is(DomainStore.CONTEXT_NO_LOCKS);
		try {
			DomainStore.get().lock(false);
			if (initialDepth == 0) {
				LooseContext.push();
			}
			LooseContext.setTrue(DomainStore.CONTEXT_NO_LOCKS);
			return read0(input);
		} catch (Exception e) {
			Ax.out("Exception in domainStore reader - start trace");
			e.printStackTrace();
			Ax.out("Exception in domainStore reader - end trace");
			throw new WrappedRuntimeException(e);
		} finally {
			if (!noLocksWasSet) {
				LooseContext.remove(DomainStore.CONTEXT_NO_LOCKS);
			}
			if (initialDepth == 0) {
				LooseContext.pop();
			}
			DomainStore.get().unlock(false);
		}
	}

	public O readThrowing(I input) throws Exception {
		int initialDepth = LooseContext.depth();
		boolean noLocksWasSet = LooseContext.is(DomainStore.CONTEXT_NO_LOCKS);
		try {
			DomainStore.get().lock(false);
			if (initialDepth == 0) {
				LooseContext.push();
			}
			LooseContext.setTrue(DomainStore.CONTEXT_NO_LOCKS);
			return read0(input);
		} finally {
			if (!noLocksWasSet) {
				LooseContext.remove(DomainStore.CONTEXT_NO_LOCKS);
			}
			if (initialDepth == 0) {
				LooseContext.pop();
			}
			DomainStore.get().unlock(false);
		}
	}

	protected abstract O read0(I input) throws Exception;
}
