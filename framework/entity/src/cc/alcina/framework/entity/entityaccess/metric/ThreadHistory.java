package cc.alcina.framework.entity.entityaccess.metric;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.entity.entityaccess.cache.DomainCacheLockState;

public class ThreadHistory {
	public String note;

	public List<ThreadHistoryElement> elements = new ArrayList<>();

	public void addElement(ThreadInfo info, StackTraceElement[] stackTrace,
			long activeMemcacheLockTime, long memcacheWaitTime,
			DomainCacheLockState memcacheState) {
		ThreadHistoryElement element = new ThreadHistoryElement();
		elements.add(element);
		element.date = new Date();
		element.threadInfo = new ThreadInfoSer(info);
		element.domainCacheWaitTime = memcacheWaitTime;
		element.domainCacheLockTime = activeMemcacheLockTime;
		element.lockState = memcacheState;
		element.threadInfo.stackTrace = Arrays.asList(stackTrace).stream()
				.collect(Collectors.toList());
	}

	@JsonIgnore
	public int getElementCount() {
		return elements.size();
	}
}
