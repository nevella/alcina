package cc.alcina.framework.entity.registry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.ResourceUtilities;

public class JarHelper {
	void listPaths(URL jarFileUrl) {
		ZipFile inZip = null;
		String jarPath = jarFileUrl.toString().replaceFirst("jar:file:(.+?)!.+",
				"$1");
		try {
			inZip = new ZipFile(jarPath);
			Enumeration<? extends ZipEntry> enumeration = inZip.entries();
			for (ZipEntry in; enumeration.hasMoreElements();) {
				in = enumeration.nextElement();
				ZipEntry outEntry;
				InputStream source;
				String name = in.getName();
				System.out.println(name);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				inZip.close();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public byte[] read(URL jarFileUrl) {
		File result = null;
		ZipFile inZip = null;
		ZipOutputStream outZip = null;
		String jarPath = jarFileUrl.toString().replaceFirst("jar:file:(.+?)!.+",
				"$1");
		String entryPath = jarFileUrl.toString()
				.replaceFirst("jar:file:.+?!(.+)", "$1");
		try {
			try {
				inZip = new ZipFile(jarPath);
				ZipEntry entry = inZip
						.getEntry(entryPath.replaceFirst("^/?(.+)", "$1"));
				return ResourceUtilities
						.readStreamToByteArray(inZip.getInputStream(entry));
			} finally {
				inZip.close();
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void replace(URL jarFileUrl, byte[] content) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		File result = null;
		ZipFile inZip = null;
		ZipOutputStream outZip = null;
		String jarPath = jarFileUrl.toString().replaceFirst("jar:file:(.+?)!.+",
				"$1");
		String entryPath = jarFileUrl.toString()
				.replaceFirst("jar:file:.+?!(.+)", "$1");
		try {
			inZip = new ZipFile(jarPath);
			outZip = new ZipOutputStream(out);
			Enumeration<? extends ZipEntry> enumeration = inZip.entries();
			for (ZipEntry in; enumeration.hasMoreElements();) {
				in = enumeration.nextElement();
				ZipEntry outEntry;
				InputStream source;
				String name = "/" + in.getName();
				if (name.equals(entryPath)) {
					outEntry = new ZipEntry(in.getName());
					outEntry.setTime(System.currentTimeMillis());
					outEntry.setLastModifiedTime(
							FileTime.fromMillis(outEntry.getTime()));
					outEntry.setSize(content.length);
					source = new ByteArrayInputStream(content);
				} else {
					outEntry = new ZipEntry(in);
					source = inZip.getInputStream(in);
				}
				outZip.putNextEntry(outEntry);
				if (in.isDirectory()) {
				} else {
					ResourceUtilities.writeStreamToStream(source, outZip, true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				inZip.close();
				outZip.close();
				String jarPath2 = jarPath.replace(".jar", ".2.jar");
				File tmpOut = File.createTempFile("persistent", ".jar");
				ResourceUtilities.writeBytesToFile(out.toByteArray(), tmpOut);
				new File(jarPath).delete();
				tmpOut.renameTo(new File(jarPath));
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public InputStream openStream(URL url) {
		return new ByteArrayInputStream(read(url));
	}
}