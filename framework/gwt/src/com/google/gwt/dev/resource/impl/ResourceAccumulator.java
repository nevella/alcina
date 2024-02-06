/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.resource.impl;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;

import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import cc.alcina.framework.entity.util.fs.FsUtils;

/**
 * Listens for and accumulates resources for a given root and PathPrefixSet.
 */
class ResourceAccumulator {
	private static final boolean WATCH_FILE_CHANGES_DEFAULT = Boolean
			.parseBoolean(System.getProperty("gwt.watchFileChanges", "true"));

	private Map<AbstractResource, ResourceResolution> resolutionsByResource;

	private Multimap<Path, Path> childPathsByParentPath;

	private Path rootDirectory;

	private WeakReference<PathPrefixSet> pathPrefixSetRef;

	private WatchService watchService;

	private boolean watchFileChanges = WATCH_FILE_CHANGES_DEFAULT;

	public ResourceAccumulator(Path rootDirectory,
			PathPrefixSet pathPrefixSet) {
		this.rootDirectory = rootDirectory;
		this.pathPrefixSetRef = new WeakReference<PathPrefixSet>(pathPrefixSet);
	}

	/**
	 * Full refresh clears existing resources and watchers and does a clean
	 * refresh.
	 */
	private void fullRefresh() throws IOException {
		resolutionsByResource = Maps.newIdentityHashMap();
		childPathsByParentPath = ArrayListMultimap.create();
		maybeInitializeWatchService();
		onNewDirectory(rootDirectory);
	}

	private PathPrefixSet getPathPrefixSet() {
		PathPrefixSet pathPrefixSet = pathPrefixSetRef.get();
		// pathPrefixSet can never be null as the life span of this class is
		// bound by it.
		assert pathPrefixSet != null;
		return pathPrefixSet;
	}

	private String getRelativePath(Path directory) {
		// Make sure that paths are exposed "Unix" style to PathPrefixSet.
		return rootDirectory.relativize(directory).toString()
				.replace(File.separator, "/");
	}
	/*
	 * FIXME - gwt - it would be nice to separate inject Alcina dependencies
	 * here rather than call out...but...
	 */

	public Map<AbstractResource, ResourceResolution> getResources() {
		return resolutionsByResource;
	}

	public boolean isWatchServiceActive() {
		return watchService != null;
	}

	private void maybeInitializeWatchService() throws IOException {
		if (watchFileChanges) {
			stopWatchService();
			try {
				watchService = FsUtils.newWatchService();
			} catch (IOException e) {
				watchFileChanges = false;
			}
		}
	}

	private void onNewDirectory(Path directory) throws IOException {
		String relativePath = getRelativePath(directory);
		if (!relativePath.isEmpty()
				&& !getPathPrefixSet().includesDirectory(relativePath)) {
			return;
		}
		if (watchService != null) {
			// Start watching the directory.
			String relativeParent = getRelativePath(directory.getParent());
			if (!FsUtils.supportsTreeNotifications() || !childPathsByParentPath
					.containsKey(directory.getParent())) {
				FsUtils.toWatchablePath(directory).register(watchService,
						ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			}
		}
		try (DirectoryStream<Path> stream = Files
				.newDirectoryStream(directory)) {
			for (Path child : stream) {
				childPathsByParentPath.put(directory, child);
				onNewOrModifiedPath(child);
			}
		}
	}

	private void onNewOrModifiedFile(Path file) {
		FileResource resource = toFileResource(file);
		ResourceResolution resourceResolution = getPathPrefixSet()
				.includesResource(resource.getPath());
		if (resourceResolution != null) {
			resolutionsByResource.put(resource, resourceResolution);
		}
	}

	private void onNewOrModifiedPath(Path path) throws IOException {
		try {
			if (Files.isHidden(path)) {
				return;
			}
			if (Files.isRegularFile(path)) {
				onNewOrModifiedFile(path);
			} else {
				onNewDirectory(path);
			}
		} catch (NoSuchFileException | FileNotFoundException e) {
			// ignore: might happen, e.g., for temporary files used for "safe
			// writes" by IDEs/editors
		}
	}

	private void onRemovedPath(Path path) {
		resolutionsByResource.remove(toFileResource(path));
		for (Path child : childPathsByParentPath.get(path)) {
			onRemovedPath(child);
		}
	}

	private void refresh() throws IOException {
		while (true) {
			WatchKey watchKey = watchService.poll();
			if (watchKey == null) {
				return;
			}
			Path parentDir = (Path) watchKey.watchable();
			for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
				WatchEvent.Kind<?> eventKind = watchEvent.kind();
				if (eventKind == OVERFLOW) {
					fullRefresh();
					return;
				}
				Path child = parentDir.resolve((Path) watchEvent.context());
				if (eventKind == ENTRY_CREATE) {
					onNewOrModifiedPath(child);
				} else if (eventKind == ENTRY_MODIFY) {
					onNewOrModifiedPath(child);
				} else if (eventKind == ENTRY_DELETE) {
					onRemovedPath(child);
				}
			}
			watchKey.reset();
		}
	}

	/**
	 * Make sure the resources associated with this directory and pathPrefixSet
	 * are up-to-date.
	 */
	public void refreshResources() throws IOException {
		if (isWatchServiceActive()) {
			refresh();
		} else {
			fullRefresh();
		}
	}

	public void shutdown() throws IOException {
		// watchService field is not cleared so any attempt to use this class
		// after shutdown will fail.
		stopWatchService();
	}

	private void stopWatchService() throws IOException {
		if (watchService != null) {
			watchService.close();
		}
	}

	private FileResource toFileResource(Path path) {
		String relativePath = getRelativePath(path);
		return FileResource.of(relativePath, path.toFile());
	}
}
