package cc.alcina.framework.entity.registry;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.EncryptionUtils;
import cc.alcina.framework.entity.Io;

public class ClassMetadata<CM extends ClassMetadata> implements Serializable {
	public static boolean USE_MD5_CHANGE_CHECK;

	public static ClassMetadata fromRelativeSourcePath(String relativeClassPath,
			URL url, InputStream inputStream, long modificationDate) {
		String cName = relativeClassPath
				.substring(0, relativeClassPath.length() - 6).replace('/', '.');
		if (cName.startsWith(".")) {
			cName = cName.substring(1);
		}
		ClassMetadata item = new ClassMetadata();
		item.className = cName;
		item.date = new Date(modificationDate);
		if (url != null) {
			item.url = url;
			item.urlString = url.toString();
		} else {
			// ignore straight jars
			// item.evalMd5(inputStream);
		}
		return item;
	}

	public Date date;

	public String md5;

	public transient URL url;

	public String urlString;

	public String className;

	public boolean invalid = false;

	// so small, a list is probably more efficient than set
	public List<String> parents = new ArrayList<>();

	public ClassMetadata() {
	}

	public ClassMetadata(String className) {
		this.className = className;
	}

	public void copyMetaFrom(ClassMetadata other) {
		this.date = other.date;
		if (other.md5 != null) {
			this.md5 = other.md5;
		}
	}

	public String ensureMd5(CachingScanner scanner) {
		if (md5 == null) {
			try {
				if (url() == null) {
					md5 = String.valueOf(System.currentTimeMillis());
				} else {
					InputStream stream = scanner.getStreamForMd5(this);
					evalMd5(stream);
				}
			} catch (Exception e) {
				md5 = String.valueOf(System.currentTimeMillis());
				e.printStackTrace();
			}
		}
		return md5;
	}

	public void ensureParents(Class clazz) {
		Optional.ofNullable(clazz.getSuperclass()).map(Class::getName)
				.ifPresent(parents::add);
		Arrays.stream(clazz.getInterfaces()).map(Class::getName)
				.forEach(parents::add);
	}

	public void evalMd5(InputStream stream) {
		try {
			byte[] bytes = Io.read().fromStream(stream).asBytes();
			md5 = EncryptionUtils.MD5(bytes);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public CM fromUrl(ClassMetadata found) {
		this.date = found.date;
		this.md5 = found.md5;
		this.url = found.url;
		this.urlString = found.urlString;
		return (CM) this;
	}

	public boolean hasInvalidatedParent(Set<String> invalidated) {
		if (invalidated.isEmpty()) {
			return false;
		}
		for (String s : parents) {
			if (invalidated.contains(s)) {
				return true;
			}
		}
		return false;
	}

	public boolean isUnchangedFrom(ClassMetadata found,
			CachingScanner scanner) {
		if (date.getTime() >= found.date.getTime()) {
			return true;
		}
		if (USE_MD5_CHANGE_CHECK && md5 != null
				&& md5.equals(found.ensureMd5(scanner))) {
			return true;
		}
		return false;
	}

	public URL url() {
		if (url == null && urlString != null) {
			try {
				url = new URL(urlString);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return url;
	}
}