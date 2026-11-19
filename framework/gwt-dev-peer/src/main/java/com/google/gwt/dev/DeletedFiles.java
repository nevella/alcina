package com.google.gwt.dev;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.util.Topic;

/*
 * A simple whole-app cache of deleted files, to save double-watching from the
 * comiplation unit builder
 */
public class DeletedFiles {
	/*
	 * Signalled by ResourceAccumulator, monitored by
	 */
	public static Topic<File> topicFileDeleted = Topic.create();

	static Set<File> deletedFiles = Collections
			.synchronizedSet(new LinkedHashSet<>());
	static {
		topicFileDeleted.add(file -> {
			try {
				deletedFiles.add(file.getCanonicalFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	static Map<File, File> fileToCanonical = new ConcurrentHashMap<>();

	public static boolean wasDeleted(String location) {
		File file = new File(location.substring("file:".length()));
		File canonical = fileToCanonical.computeIfAbsent(file, f -> {
			try {
				return f.getCanonicalFile();
			} catch (Exception e) {
				return f;
			}
		});
		boolean deleted = deletedFiles.contains(canonical);
		return deleted;
	}
}
