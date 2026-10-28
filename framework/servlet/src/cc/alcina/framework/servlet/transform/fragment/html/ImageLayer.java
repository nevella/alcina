package cc.alcina.framework.servlet.transform.fragment.html;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.Image.HasImageAttributes;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.servlet.transform.fragment.api.DocumentTransformerHostApi;

/**
 * <p>
 * Maps various image tags (including word images) to tokens (and thus output
 * nodes)
 * 
 * <p>
 * There's some convoluted logic around whether to respect image size attributes
 * (if any) vs the actual source image dims - since the specified source attrs
 * might be best-effort for a particualar word doc size. But if they're small,
 * respect 'em
 */
public class ImageLayer extends Layer<StyledNode> {
	public static class ImageObserved implements ContextObservable {
		HasImageAttributes imageAttributes;

		public ImageObserved(HasImageAttributes selection) {
			this.imageAttributes = selection;
		}

		public LayoutTokens.Image.HasImageAttributes getImage() {
			return imageAttributes;
		}
	}

	public static class Peer {
		public interface Has {
			Peer getImageLayerPeer();
		}

		public IntPair resolvePreferredDimensions(IntPair sourceDims,
				IntPair specifiedAttributeDims) {
			if (specifiedAttributeDims.i1 < 400) {
				return specifiedAttributeDims;
			} else {
				return sourceDims;
			}
		}
	}

	static class ImageSelection extends StyledMeasureSelection
			implements LayoutTokens.Image.HasImageAttributes {
		String src;

		String alt;

		Integer width;

		Integer height;

		byte[] bytes;

		public byte[] getBytes() {
			return bytes;
		}

		ImageSelection(Selection parent, Measure measure, String sourceUrl,
				String alt, Integer width, Integer height, byte[] bytes) {
			super(parent, measure);
			this.src = sourceUrl;
			this.alt = alt;
			this.height = height;
			this.width = width;
			this.bytes = bytes;
		}

		public Integer getWidth() {
			return width;
		}

		public Integer getHeight() {
			return height;
		}

		public String getSrc() {
			return src;
		}

		public String getAlt() {
			return alt;
		}
	}

	public static boolean isImageLike(DomNode domNode) {
		return domNode.tagIsOneOf("img", "v:imgdata", "v:imagedata");
	}

	public static boolean containsImageData(DomNode comment) {
		return comment.toMarkup().contains("v:imagedata");
	}

	boolean isInvalidExtension(String url) {
		String extension = FileUtils.getExtension(url);
		switch (extension) {
		case "wmz":
			/*
			 * weird win image types
			 */
			return true;
		default:
			return false;
		}
	}

	@Override
	public void process(StyledNode node) throws Exception {
		DomNode domNode = node.get();
		if (isImageLike(domNode)) {
			Measure measure = Measure.fromNode(node.get(),
					LayoutTokens.Image.TYPE);
			String alt = domNode.attr("alt");
			String nonUnitWidth = domNode.attr("width")
					.replaceFirst("(\\d+)(\\D+)", "$1");
			String nonUnitHeight = domNode.attr("height")
					.replaceFirst("(\\d+)(\\D+)", "$1");
			Integer widthAttribute = Ax.notBlank(nonUnitWidth)
					? Integer.parseInt(nonUnitWidth)
					: null;
			Integer heightAttribute = Ax.notBlank(nonUnitHeight)
					? Integer.parseInt(nonUnitHeight)
					: null;
			String sourceUrl = null;
			try {
				sourceUrl = DocumentTransformerHostApi.get()
						.getImageSourceUrl(domNode);
				byte[] bytes = DocumentTransformerHostApi.get()
						.getImageBytes(domNode);
				BufferedImage img = ImageIO
						.read(new ByteArrayInputStream(bytes));
				if (Ax.isBlank(alt)) {
					alt = domNode.attr("o:title");
				}
				IntPair sourceDims = new IntPair(img.getWidth(),
						img.getHeight());
				IntPair imageDims = sourceDims;
				if (widthAttribute != null && heightAttribute != null) {
					IntPair specifiedAttributeDims = new IntPair(widthAttribute,
							heightAttribute);
					Peer peer = state.traversalContext(Peer.Has.class)
							.getImageLayerPeer();
					imageDims = peer.resolvePreferredDimensions(sourceDims,
							specifiedAttributeDims);
				}
				ImageSelection selection = new ImageSelection(node, measure,
						sourceUrl, alt, imageDims.i1, imageDims.i2, bytes);
				select(selection);
				new ImageObserved(selection).publish();
			} catch (Exception e) {
				// DEVEX - if something like a .wvz file, probably handle before
				// exception
				e.printStackTrace();
				Ax.sysLogHigh("non-fatal for selection");
				select(new ImageSelection(node, measure, sourceUrl, alt, null,
						null, null));
			}
		}
	}

	String replaceWithWordVshapeSrc(StyledNode node, String srcAttr) {
		DomNode domNode = node.get();
		DomNode ctr = domNode.ancestors().get("p", "h1", "div");
		if (ctr != null) {
			DomNode firstComment = ctr.children.nodes().stream()
					.filter(DomNode::isComment).findFirst().orElse(null);
			if (firstComment != null) {
				String hiresUrlPattern = "<v:imagedata src=\"(.+?)\".+></v>";
				Matcher m = Pattern.compile(hiresUrlPattern)
						.matcher(firstComment.toMarkup());
				if (m.find()) {
					String replacementSrcAttr = m.group(1);
					String numericRegex = "image(\\d+)\\..+";
					Pattern numericPattern = Pattern.compile(numericRegex);
					if (!isInvalidExtension(replacementSrcAttr)) {
						logger.info(
								"Word/vshape - replacing img src '{}' with '{}'",
								srcAttr, replacementSrcAttr);
						srcAttr = replacementSrcAttr;
					}
				}
			}
		}
		return srcAttr;
	}
}