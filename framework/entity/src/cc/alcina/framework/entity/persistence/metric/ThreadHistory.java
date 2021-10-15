package cc.alcina.framework.entity.persistence.metric;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.entity.persistence.domain.DomainStoreLockState;
import cc.alcina.framework.entity.persistence.domain.DomainStoreWaitStats;

public class ThreadHistory {
	public String note;

	public List<ThreadHistoryElement> elements = new ArrayList<>();

	public int elementCount;

	public int elidedElementCount;

	public void addElement(ThreadInfo info, StackTraceElement[] stackTrace,
			long activeDomainStoreLockTime, long domainStoreWaitTime,
			DomainStoreLockState domainStoreState, int maxStackLines,
			int maxFrames, DomainStoreWaitStats waitStats) {
		ThreadHistoryElement element = new ThreadHistoryElement();
		elements.add(element);
		element.date = new Date();
		element.threadInfo = new ThreadInfoSer(info);
		element.domainCacheWaitTime = domainStoreWaitTime;
		element.domainCacheLockTime = activeDomainStoreLockTime;
		element.lockState = domainStoreState;
		element.waitStats = waitStats;
		element.threadInfo.stackTrace = Arrays.asList(stackTrace).stream()
				.collect(Collectors.toList());
		element.elideIfMoreLinesThan(maxStackLines);
		int size = elements.size();
		if (size > maxFrames) {
			elements = elements.stream().limit(maxFrames)
					.collect(Collectors.toList());
			this.elidedElementCount += size - maxFrames;
		}
		elementCount = elements.size();
	}

	public void clearElements() {
		elements.clear();
		elementCount = 0;
	}

	@JsonIgnore
	public int getElementCount() {
		return elementCount;
	}
}
