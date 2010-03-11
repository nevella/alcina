/* 
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

package cc.alcina.framework.entity;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CommonUtils;


/**
 * @author nick@alcina.cc
 * 
 */
public class ResourceUtilities {
	private ResourceUtilities() {
		super();
		customProperties = new ArrayList<Properties>();
	}

	private static ResourceUtilities theInstance;

	public static ResourceUtilities singleton() {
		if (theInstance == null) {
			theInstance = new ResourceUtilities();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	List<Properties> customProperties;

	public void registerCustomProperties(InputStream ios) {
		try {
			Properties p = new Properties();
			if (ios != null) {
				p.load(ios);
				ios.close();
				customProperties.add(p);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void registerCustomProperties(String path) {
		InputStream ios = this.getClass().getResourceAsStream(path);
		this.registerCustomProperties(ios);
	}

	public boolean getBoolean(Class clazz, String propertyName) {
		String s = getBundledString(clazz, propertyName);
		return Boolean.valueOf(s);
	}

	public int getInteger(Class clazz, String propertyName, int defaultValue) {
		try {
			String s = getBundledString(clazz, propertyName);
			return Integer.valueOf(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public String getBundledString(Class clazz, String propertyName) {
		for (Properties p : customProperties) {
			String namespacedKey = (clazz == null) ? propertyName : clazz
					.getSimpleName()
					+ "." + propertyName;
			if (p.containsKey(namespacedKey)) {
				return p.getProperty(namespacedKey);
			}
		}
		ResourceBundle b = ResourceBundle.getBundle(clazz.getPackage()
				.getName()
				+ ".Bundle", Locale.getDefault(), clazz.getClassLoader());
		return b.getString(propertyName);
	}
	public static interface BeanInfoHelper{
		BeanInfo postProcessBeanInfo(BeanInfo beanInfo);
	}
	private static BeanInfoHelper helper;
	public static void registerBeanInfoHelper(BeanInfoHelper theHelper){
		helper=theHelper;
	}
	/**
	 * Retrieves the BeanInfo for a Class
	 */
	public static BeanInfo getBeanInfo(Class cls) {
		BeanInfo beanInfo = null;
		try {
			beanInfo = Introspector.getBeanInfo(cls);
			if (helper!=null){
				beanInfo=helper.postProcessBeanInfo(beanInfo);
			}
		} catch (IntrospectionException ex) {
			ex.printStackTrace();
		}
		return beanInfo;
	}

	public static OutputStream scaleImage(InputStream in, int width,
			int height, OutputStream out) throws IOException {
		byte[] b = readStreamToByteArray(in);
		ImageIcon icon = new ImageIcon(b);
		Image image = icon.getImage();
		int thumbWidth = width;
		int thumbHeight = height;
		double thumbRatio = (double) thumbWidth / (double) thumbHeight;
		int imageWidth = image.getWidth(null);
		int imageHeight = image.getHeight(null);
		double imageRatio = (double) imageWidth / (double) imageHeight;
		if (thumbRatio < imageRatio) {
			thumbHeight = (int) (thumbWidth / imageRatio);
		} else {
			thumbWidth = (int) (thumbHeight * imageRatio);
		}
		// draw original image to thumbnail image object and
		// scale it to the new size on-the-fly
		BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = thumbImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
		// save thumbnail image to OUTFILE
		ImageIO.write(thumbImage, "png", out);
		return out;
	}

	public static byte[] readStreamToByteArray(InputStream is)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writeStreamToStream(is, baos);
		return baos.toByteArray();
	}

	public static void writeStreamToStream(InputStream is, OutputStream os)
			throws IOException {
		BufferedOutputStream fos = new BufferedOutputStream(os);
		InputStream in = new BufferedInputStream(is);
		int bufLength = 8192;
		byte[] buffer = new byte[bufLength];
		int result;
		while ((result = in.read(buffer)) != -1) {
			fos.write(buffer, 0, result);
		}
		fos.flush();
		fos.close();
	}

	public static Object serialClone(Object bean) {
		Object result = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeObject(bean);
			out.close();
			ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(baos.toByteArray()));
			result = in.readObject();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return result;
	}

	public static <T> T copyBeanProperties(Object srcBean, T tgtBean) {
		return copyBeanProperties(srcBean, tgtBean, null);
	}

	public static <T> T copyBeanProperties(Object srcBean, T tgtBean,
			Class methodFilterAnnotation) {
		return copyBeanProperties(srcBean, tgtBean, methodFilterAnnotation,
				false);
	}

	public static <T> T copyBeanProperties(Object srcBean, T tgtBean,
			Class methodFilterAnnotation, boolean cloneCollections) {
		return copyBeanProperties(srcBean, tgtBean, methodFilterAnnotation,
				cloneCollections, new ArrayList<String>());
	}
	@SuppressWarnings("unchecked")
	public static <T> T copyBeanProperties(Object srcBean, T tgtBean,
			Class methodFilterAnnotation, boolean cloneCollections,
			Collection<String> ignorePropertyNames) {
		BeanInfo info = getBeanInfo(srcBean.getClass());
		BeanInfo infoTgt = getBeanInfo(tgtBean.getClass());
		for (PropertyDescriptor pd : infoTgt.getPropertyDescriptors()) {
			Method getMethod = null;
			String tgtPName = pd.getName();
			if (ignorePropertyNames.contains(tgtPName)) {
				continue;
			}
			for (PropertyDescriptor pd2 : info.getPropertyDescriptors()) {
				if (pd2.getName().equals(tgtPName)) {
					getMethod = pd2.getReadMethod();
					break;
				}
			}
			if (getMethod == null) {
				continue;
			}
			if (methodFilterAnnotation != null) {
				if (getMethod.isAnnotationPresent(methodFilterAnnotation)) {
					continue;
				}
			}
			Method setMethod = pd.getWriteMethod();
			if (setMethod != null) {
				try {
					Object obj = getMethod.invoke(srcBean, (Object[]) null);
					if (cloneCollections && obj instanceof Collection
							&& obj instanceof Cloneable) {
						Method clone = obj.getClass().getMethod("clone",
								new Class[0]);
						clone.setAccessible(true);
						obj = clone.invoke(obj, CommonUtils.EMPTY_OBJECT_ARRAY);
					}
					setMethod.invoke(tgtBean, obj);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
		return tgtBean;
	}

	public static boolean isNumericPrimitive(Class c) {
		return (c.isPrimitive() && c != char.class && c != boolean.class);
	}

	public static String objectOrPrimitiveToString(Object object) {
		if (object == null) {
			return null;
		}
		return object.toString();
	}

	public static boolean isIntegralType(Class c) {
		if (c.isPrimitive()) {
			return c != char.class && c != boolean.class && c != float.class
					&& c != double.class;
		} else {
			return c == Integer.class || c == Byte.class || c == Short.class
					|| c == Long.class;
		}
	}

	public static boolean isNullOrEmpty(String s) {
		return (s == null || s.length() == 0);
	}

	public static String readStreamToString(InputStream is) throws IOException {
		return readStreamToString(is, null);
	}

	public static String readStreamToString(InputStream is, String charsetName)
			throws IOException {
		charsetName = charsetName == null ? "UTF-8" : charsetName;
		BufferedReader in = new BufferedReader(new InputStreamReader(is,
				charsetName));
		StringWriter sw = new StringWriter();
		char[] cb = new char[4096];
		int len = -1;
		while ((len = in.read(cb, 0, 4096)) != -1) {
			sw.write(cb, 0, len);
		}
		return sw.toString();
	}

	public static void writeStringToFile(String s, File f) throws IOException {
		OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(f),
				"UTF-8");
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(s);
		bw.close();
	}

	public static InputStream writeStringToInputStream(String s)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter fw = new OutputStreamWriter(baos, "UTF-8");
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(s);
		bw.close();
		return new ByteArrayInputStream(baos.toByteArray());
	}

	public static String readFileToString(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		return readStreamToString(fis);
	}

	public static String readFileToString(File f, String charsetName)
			throws IOException {
		FileInputStream fis = new FileInputStream(f);
		return readStreamToString(fis, charsetName);
	}

	public static BufferedImage getBufferedImage(Class clazz,
			String relativePath) {
		BufferedImage img = null;
		if (img == null) {
			try {
				img = ImageIO.read(clazz.getResource(relativePath));
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return img;
	}

	public static byte[] readFileToByteArray(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		return readStreamToByteArray(fis);
	}
}
