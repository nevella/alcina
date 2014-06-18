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

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import cc.alcina.framework.common.client.util.LooseContext;

/**
 * 
 * @author Nick Reddel
 */
public class ImgUtilities {
	public static final String CONTEXT_PDF2HTML_TOPIC_MONOCHROME_IMAGES = ImgUtilities.class
			.getName() + "." + "pdf2html-monochrome-images";

	public static final String CONTEXT_JPEG_COMPRESSION_RATIO = ImgUtilities.class
			.getName() + "." + "CONTEXT_PDF_JPEG_COMPRESSION_RATIO";

	public ImgUtilities() {
	}

	public static Icon scaleImage(File inputFile, int width, int height) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Icon icon = null;
		try {
			scaleImage(inputFile, width, height, out);
			icon = new ImageIcon(out.toByteArray());
		} catch (Exception e) {
		}
		return icon;
	}

	public static void compressJpegFile(File infile, File outfile,
			float compressionQuality) throws Exception {
		compressJpeg(ImageIO.read(infile), outfile, compressionQuality);
	}

	public static void compressJpeg(BufferedImage img, File outfile,
			float compressionQuality) throws Exception {
		// Find a jpeg writer
		ImageWriter writer = null;
		Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
		if (iter.hasNext()) {
			writer = (ImageWriter) iter.next();
		}
		// Prepare output file
		ImageOutputStream ios = ImageIO.createImageOutputStream(outfile);
		writer.setOutput(ios);
		// Set the compression quality
		ImageWriteParam iwparam = new MyImageWriteParam();
		iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwparam.setCompressionQuality(compressionQuality);
		// Write the image
		writer.write(null, new IIOImage(img, null, null), iwparam);
		// Cleanup
		ios.flush();
		writer.dispose();
		ios.close();
	}

	public static void writeJpeg(BufferedImage img, OutputStream os,
			Float compressionQuality) throws Exception {
		Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
		ImageWriter writer = (ImageWriter) iter.next();
		ImageWriteParam iwp = writer.getDefaultWriteParam();
		iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		compressionQuality = compressionQuality != null ? compressionQuality
				: LooseContext.getContext().getFloat(
						ImgUtilities.CONTEXT_JPEG_COMPRESSION_RATIO);
		iwp.setCompressionQuality(compressionQuality == null ? 0.8f
				: compressionQuality);
		ImageOutputStream ios = new MemoryCacheImageOutputStream(os);
		writer.setOutput(ios);
		IIOImage image = new IIOImage(img, null, null);
		writer.write(null, image, iwp);
		ios.flush();
		writer.dispose();
	}

	public static void toJpegThumbnail(File src, File tgt, int maxWidth)
			throws Exception {
		BufferedImage img = ImageIO.read(src);
		img = resizeToMaxWidth(img, maxWidth);
		writeJpeg(img, new FileOutputStream(tgt), 0.8f);
	}
	
	public static void toJpegThumbnail(InputStream src, OutputStream tgt, int maxWidth)
			throws Exception {
		BufferedImage img = ImageIO.read(src);
		img = resizeToMaxWidth(img, maxWidth);
		writeJpeg(img, tgt, 0.8f);
	}
	
	public static void toPngThumbnail(File src, File tgt, int maxWidth)
			throws Exception {
		BufferedImage img = ImageIO.read(src);
		img = resizeToMaxWidth(img, maxWidth);
		writePng(img, new FileOutputStream(tgt));
	}

	public static BufferedImage resizeToMaxWidth(BufferedImage src, int maxWidth) {
		if (src.getWidth() > maxWidth) {
			int scaledWidth = maxWidth;
			int scaledHeight = (int) (scaledWidth * ((double) src.getHeight() / (double) src
					.getWidth()));
			ColorModel colorModel = src.getColorModel();
			BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight,
					BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = scaled.createGraphics();
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics2D.setColor(Color.WHITE);
			graphics2D.fillRect(0, 0, scaledWidth, scaledHeight);
			boolean drawImage = graphics2D.drawImage(src, 0, 0, scaledWidth,
					scaledHeight, null);
			if (colorModel instanceof IndexColorModel) {
				IndexColorModel icm = (IndexColorModel) colorModel;
				int numComponents = icm.getNumComponents();
				int mapSize = icm.getMapSize();
				if (mapSize == 2
						|| (mapSize <= 4 && icm.getTransparentPixel() == -1)) {
					IndexColorModel blackAndWhiteColorModel = getBlackAndWhiteColorModel();// icm;//
					BufferedImage cc = new BufferedImage(scaledWidth,
							scaledHeight, src.getType(),
							blackAndWhiteColorModel);
					graphics2D = cc.createGraphics();
					graphics2D.setColor(Color.WHITE);
					graphics2D.fillRect(0, 0, scaledWidth, scaledHeight);
					graphics2D.drawImage(scaled, 0, 0, scaledWidth,
							scaledHeight, null);
					if (scaledHeight * 5 > scaledWidth) {
						checkMonochrome(cc);
					}
					return cc;
				}
			}
			return scaled;
		}
		return src;
	}

	public static void checkMonochrome(BufferedImage cc) {
		int first = cc.getRGB(0, 0);
		for (int y = 0; y < cc.getHeight(); y++) {
			for (int x = 0; x < cc.getWidth(); x++) {
				if (cc.getRGB(x, y) != first) {
					return;
				}
			}
		}
		LooseContext.getContext().publishTopic(
				ImgUtilities.CONTEXT_PDF2HTML_TOPIC_MONOCHROME_IMAGES, true);
	}

	private static IndexColorModel getBlackAndWhiteColorModel() {
		return new IndexColorModel(1, 2, new byte[] { 0, -1 }, new byte[] { 0,
				-1 }, new byte[] { 0, -1 }, 1);
	}

	public static void writePng(BufferedImage img, OutputStream os) throws IOException {
		ImageIO.write(img, "png", os);
		os.close();
	}

	// This class overrides the setCompressionQuality() method to workaround
	// a problem in compressing JPEG images using the javax.imageio package.
	public static class MyImageWriteParam extends JPEGImageWriteParam {
		public MyImageWriteParam() {
			super(Locale.getDefault());
		}

		// This method accepts quality levels between 0 (lowest) and 1 (highest)
		// and simply converts
		// it to a range between 0 and 256; this is not a correct conversion
		// algorithm.
		// However, a proper alternative is a lot more complicated.
		// This should do until the bug is fixed.
		public void setCompressionQuality(float quality) {
			if (quality < 0.0F || quality > 1.0F) {
				throw new IllegalArgumentException("Quality out-of-bounds!");
			}
			this.compressionQuality = quality * 256;
		}
	}

	public static OutputStream convertToJpeg(File inputFile, OutputStream out)
			throws InterruptedException, IOException {
		Image image = Toolkit.getDefaultToolkit().getImage(
				inputFile.getAbsolutePath());
		MediaTracker mediaTracker = new MediaTracker(new Container());
		mediaTracker.addImage(image, 0);
		mediaTracker.waitForID(0);
		if (out == null) {
			out = new ByteArrayOutputStream();
		}
		int thumbWidth = image.getWidth(null);
		int thumbHeight = image.getHeight(null);
		BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = thumbImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
		ImageIO.write(thumbImage, "jpg", out);
		return out;
	}

	public static OutputStream scaleImage(File inputFile, int width,
			int height, OutputStream out) throws InterruptedException,
			IOException {
		Image image = Toolkit.getDefaultToolkit().getImage(
				inputFile.getAbsolutePath());
		MediaTracker mediaTracker = new MediaTracker(new Container());
		mediaTracker.addImage(image, 0);
		mediaTracker.waitForID(0);
		// determine thumbnail size from WIDTH and HEIGHT
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

	public static Image scaleImage(Image image, int width, int height) {
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
		return thumbImage;
	}
}
