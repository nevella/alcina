/*
 * Copyright Miroslav Pokorny
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
package rocket.util.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A helper which assists with the management of colours
 * 
 * @author Miroslav Pokorny (mP)
 */
public class Colour implements java.io.Serializable, IsSerializable {
	/**
	 * THis map is used to lookup rgb values for a colour using its name. THe
	 * key is the lowercased form of the colour name.
	 */
	private static Map namedColours;
	static {
		final Map named = new HashMap() {
			public Object get(final Object key) {
				return super.get(fixKey(key));
			}

			public Object put(final Object key, final Object value) {
				return super.put(fixKey(key), value);
			}

			protected String fixKey(final Object key) {
				return ((String) key).toLowerCase();
			}
		};
		named.put("aliceBlue", new Colour(0xf0f8ff));
		named.put("antiqueWhite", new Colour(0xfaebd7));
		named.put("aqua", new Colour(0x00ffff));
		named.put("aquaMarine", new Colour(0x7fffd4));
		named.put("azure", new Colour(0xf0ffff));
		named.put("beige", new Colour(0xf5f5dc));
		named.put("bisque", new Colour(0xffe4c4));
		named.put("black", new Colour(0x000000));
		named.put("blanchedAlmond", new Colour(0xffebcd));
		named.put("blue", new Colour(0x0000ff));
		named.put("blueViolet", new Colour(0x8a2be2));
		named.put("brown", new Colour(0xa52a2a));
		named.put("burlyWood", new Colour(0xdeb887));
		named.put("cadetBlue", new Colour(0x5f9ea0));
		named.put("chartReuse", new Colour(0x7fff00));
		named.put("chocolate", new Colour(0xd2691e));
		named.put("coral", new Colour(0xff7f50));
		named.put("cornFlowerBlue", new Colour(0x6495ed));
		named.put("cornsilk", new Colour(0xfff8dc));
		named.put("crimson", new Colour(0xdc143c));
		named.put("cyan", new Colour(0x00ffff));
		named.put("darkBlue", new Colour(0x00008b));
		named.put("darkCyan", new Colour(0x008b8b));
		named.put("darkGoldenRod", new Colour(0xb8860b));
		named.put("darkGray", new Colour(0xa9a9a9));
		named.put("darkGreen", new Colour(0x006400));
		named.put("darkKhaki", new Colour(0xbdb76b));
		named.put("darkMagenta", new Colour(0x8b008b));
		named.put("darkOliveGreen", new Colour(0x556b2f));
		named.put("darkOrange", new Colour(0xff8c00));
		named.put("darkOrchid", new Colour(0x9932cc));
		named.put("darkRed", new Colour(0x8b0000));
		named.put("darkSalmon", new Colour(0xe9967a));
		named.put("darkSeaGreen", new Colour(0x8fbc8f));
		named.put("darkSlateBlue", new Colour(0x483d8b));
		named.put("darkSlateGray", new Colour(0x2f4f4f));
		named.put("darkTurquoise", new Colour(0x00ced1));
		named.put("darkViolet", new Colour(0x9400d3));
		named.put("deepPink", new Colour(0xff1493));
		named.put("deepSkyBlue", new Colour(0x00bfff));
		named.put("dimGray", new Colour(0x696969));
		named.put("dodgerBlue", new Colour(0x1e90ff));
		named.put("feldspar", new Colour(0xd19275));
		named.put("fireBrick", new Colour(0xb22222));
		named.put("floralWhite", new Colour(0xfffaf0));
		named.put("forestGreen", new Colour(0x228b22));
		named.put("fuchsia", new Colour(0xff00ff));
		named.put("gainsboro", new Colour(0xdcdcdc));
		named.put("ghostWhite", new Colour(0xf8f8ff));
		named.put("gold", new Colour(0xffd700));
		named.put("goldenRod", new Colour(0xdaa520));
		named.put("gray", new Colour(0x808080));
		named.put("green", new Colour(0x008000));
		named.put("greenYellow", new Colour(0xadff2f));
		named.put("honeydew", new Colour(0xf0fff0));
		named.put("hotPink", new Colour(0xff69b4));
		named.put("indianRed ", new Colour(0xcd5c5c));
		named.put("indigo ", new Colour(0x4b0082));
		named.put("ivory", new Colour(0xfffff0));
		named.put("khaki", new Colour(0xf0e68c));
		named.put("lavender", new Colour(0xe6e6fa));
		named.put("lavenderBlush", new Colour(0xfff0f5));
		named.put("lawnGreen", new Colour(0x7cfc00));
		named.put("lemonChiffon", new Colour(0xfffacd));
		named.put("lightBlue", new Colour(0xadd8e6));
		named.put("lightCoral", new Colour(0xf08080));
		named.put("lightCyan", new Colour(0xe0ffff));
		named.put("lightGoldenrodYellow", new Colour(0xfafad2));
		named.put("lightGrey", new Colour(0xd3d3d3));
		named.put("lightGreen", new Colour(0x90ee90));
		named.put("lightPink", new Colour(0xffb6c1));
		named.put("lightSalmon", new Colour(0xffa07a));
		named.put("lightSeaGreen", new Colour(0x20b2aa));
		named.put("lightSkyBlue", new Colour(0x87cefa));
		named.put("lightSlateBlue", new Colour(0x8470ff));
		named.put("lightSlateGray", new Colour(0x778899));
		named.put("lightSteelBlue", new Colour(0xb0c4de));
		named.put("lightYellow", new Colour(0xffffe0));
		named.put("lime", new Colour(0x00ff00));
		named.put("limeGreen", new Colour(0x32cd32));
		named.put("linen", new Colour(0xfaf0e6));
		named.put("magenta", new Colour(0xff00ff));
		named.put("maroon", new Colour(0x800000));
		named.put("mediumAquamarine", new Colour(0x66cdaa));
		named.put("mediumBlue", new Colour(0x0000cd));
		named.put("mediumOrchid", new Colour(0xba55d3));
		named.put("mediumPurple", new Colour(0x9370d8));
		named.put("mediumSeaGreen", new Colour(0x3cb371));
		named.put("mediumSlateBlue", new Colour(0x7b68ee));
		named.put("mediumSpringGreen", new Colour(0x00fa9a));
		named.put("mediumTurquoise", new Colour(0x48d1cc));
		named.put("mediumVioletRed", new Colour(0xc71585));
		named.put("midnightBlue", new Colour(0x191970));
		named.put("mintCream", new Colour(0xf5fffa));
		named.put("mistyRose", new Colour(0xffe4e1));
		named.put("moccasin", new Colour(0xffe4b5));
		named.put("navajoWhite", new Colour(0xffdead));
		named.put("navy", new Colour(0x000080));
		named.put("oldLace", new Colour(0xfdf5e6));
		named.put("olive", new Colour(0x808000));
		named.put("oliveDrab", new Colour(0x6b8e23));
		named.put("orange", new Colour(0xffa500));
		named.put("orangeRed", new Colour(0xff4500));
		named.put("orchid", new Colour(0xda70d6));
		named.put("paleGoldenrod", new Colour(0xeee8aa));
		named.put("paleGreen", new Colour(0x98fb98));
		named.put("paleTurquoise", new Colour(0xafeeee));
		named.put("paleVioletRed", new Colour(0xd87093));
		named.put("papayaWhip", new Colour(0xffefd5));
		named.put("peachPuff", new Colour(0xffdab9));
		named.put("peru", new Colour(0xcd853f));
		named.put("pink", new Colour(0xffc0cb));
		named.put("plum", new Colour(0xdda0dd));
		named.put("powderBlue", new Colour(0xb0e0e6));
		named.put("purple", new Colour(0x800080));
		named.put("red", new Colour(0xff0000));
		named.put("rosyBrown", new Colour(0xbc8f8f));
		named.put("royalBlue", new Colour(0x4169e1));
		named.put("saddleBrown", new Colour(0x8b4513));
		named.put("salmon", new Colour(0xfa8072));
		named.put("sandyBrown", new Colour(0xf4a460));
		named.put("seaGreen", new Colour(0x2e8b57));
		named.put("seaShell", new Colour(0xfff5ee));
		named.put("sienna", new Colour(0xa0522d));
		named.put("silver", new Colour(0xc0c0c0));
		named.put("skyBlue", new Colour(0x87ceeb));
		named.put("slateBlue", new Colour(0x6a5acd));
		named.put("slateGray", new Colour(0x708090));
		named.put("snow", new Colour(0xfffafa));
		named.put("springGreen", new Colour(0x00ff7f));
		named.put("steelBlue", new Colour(0x4682b4));
		named.put("tan", new Colour(0xd2b48c));
		named.put("teal", new Colour(0x008080));
		named.put("thistle", new Colour(0xd8bfd8));
		named.put("tomato", new Colour(0xff6347));
		named.put("turquoise", new Colour(0x40e0d0));
		named.put("violet", new Colour(0xee82ee));
		named.put("violetRed", new Colour(0xd02090));
		named.put("wheat", new Colour(0xf5deb3));
		named.put("white", new Colour(0xffffff));
		named.put("whiteSmoke", new Colour(0xf5f5f5));
		named.put("yellow", new Colour(0xffff00));
		named.put("yellowGreen", new Colour(0x9acd32));
		Colour.namedColours = named;
	}

	/**
	 * Returns the Colour given a web colour name.
	 * 
	 * @param namedColour
	 *            A standard colour name, such as red, blue etc. Case is not
	 *            important.
	 * @return null is returned when the name is unknown
	 */
	public static Colour getColour(final String namedColour) {
		return (Colour) Colour.namedColours.get(namedColour.toLowerCase());
	}

	/**
	 * Accepts a colour value as a string and attempts to convert it into an
	 * integer value. Currently supported formats include
	 * <ul>
	 * <li>#rgb</li>
	 * <li>#rrggbb</li>
	 * <li>rgb(r,g,b)</li>
	 * <li>named colours</li>
	 * </ul>
	 * 
	 * @param value
	 * @return
	 */
	static public Colour parse(final String value) {
		Checker.notEmpty("parameter:value", value);
		Colour colourValue = null;
		while (true) {
			// test if a #rrggbb or #rgb value...
			if (value.startsWith("#")) {
				// handles #rgb values.
				if (value.length() == 1 + 3) {
					colourValue = parseHashRgb(value);
					break;
				}
				// handles #rrggbb values.
				if (value.length() == 1 + 6) {
					colourValue = parseHashRrggbb(value);
					break;
				}
			}
			// test if rgb(rr,gg,bb)
			if (value.startsWith("rgb(") & value.endsWith(")")) {
				colourValue = parseRgbTriplet(value);
				break;
			}
			// assume that value contains the name of a colour...
			colourValue = Colour.getColour(value);
			if (null != colourValue) {
				break;
			}
			// unknown colour value/ format etc.
			throw new IllegalArgumentException(
					"Unable to read rgb value from property value\"" + value
							+ "\".");
		}
		return colourValue;
	}

	/**
	 * Handles the parsing and conversion of #rgb formatted colour values into
	 * an integer
	 * 
	 * @param value
	 * @return
	 */
	static Colour parseHashRgb(final String value) {
		Checker.notEmpty("parameter:value", value);
		int red = Character.digit(value.charAt(1), 16);
		red = red * 17;
		int green = Character.digit(value.charAt(2), 16);
		green = green * 17;
		int blue = Character.digit(value.charAt(3), 16);
		blue = blue * 17;
		return new Colour(red, green, blue);
	}

	/**
	 * Handles the parsing and conversion of #rrggbb formatted colour values
	 * into an integer
	 * 
	 * @param value
	 * @return
	 */
	static Colour parseHashRrggbb(final String value) {
		Checker.notEmpty("parameter:value", value);
		final int redGreenBlue = Integer.parseInt(value.substring(1, 1 + 6),
				16);
		return new Colour(redGreenBlue);
	}

	/**
	 * Handles the parsing and conversion of rgb(rr,gg,bbb) triplets into a
	 * single integer value.
	 * 
	 * @param value
	 * @return
	 */
	static Colour parseRgbTriplet(final String value) {
		final String[] triplets = Utilities
				.split(value.substring(4, value.length() - 1), ",", true);
		final int red = Integer.parseInt(triplets[0].trim());
		final int green = Integer.parseInt(triplets[1].trim());
		final int blue = Integer.parseInt(triplets[2].trim());
		return new Colour(red, green, blue);
	}

	private int red;

	private int green;

	private int blue;

	public Colour() {
		this(0);
	}

	public Colour(final int colour) {
		this((colour >> 16) & 0xff, (colour >> 8) & 0xff, colour & 0x0ff);
	}

	public Colour(final int red, final int green, final int blue) {
		super();
		this.setRed(red);
		this.setGreen(green);
		this.setBlue(blue);
	}

	/**
	 * Converts this colour into a HueSaturationValue.
	 * 
	 * @return
	 */
	public HueSaturationValue asHueSaturationValue() {
		final int red = this.getRed();
		final int green = this.getGreen();
		final int blue = this.getBlue();
		final int min = Math.min(red, Math.min(green, blue));
		final int value = Math.max(red, Math.max(green, blue));
		final int delta = value - min;
		int hue = 0;
		final float saturation = (value == 0) ? 0 : (float) delta / value;
		while (true) {
			if (Tester.equals(saturation, 0, 0.001)) {
				hue = 0;
				break;
			}
			while (true) {
				if (red == value) {
					hue = 60 * (green - blue) / delta;
					break;
				}
				if (green == value) {
					hue = 120 + 60 * (blue - red) / delta;
					break;
				}
				if (blue == value) {
					hue = 240 + 60 * (red - green) / delta;
					break;
				}
				break;
			}
			if (hue < 0) {
				hue = hue + 360;
			}
			break;
		}
		if (hue == 0) {
			hue = 360;
		}
		return new HueSaturationValue(hue / 360f, saturation, value / 256f);
	}

	public boolean equals(final Colour otherColour) {
		return otherColour == null ? false
				: this.getRed() == otherColour.getRed()
						&& this.getGreen() == otherColour.getGreen()
						&& this.getBlue() == otherColour.getBlue();
	}

	public boolean equals(final Object otherObject) {
		return otherObject instanceof Colour ? this.equals((Colour) otherObject)
				: false;
	}

	public int getBlue() {
		return this.blue;
	}

	public int getGreen() {
		return this.green;
	}

	public int getRed() {
		return this.red;
	}

	public int getRgb() {
		return this.getRed() << 16 | this.getGreen() << 8 | this.getBlue();
	}

	public int hashCode() {
		return this.getRed() << 16 | this.getGreen() << 8 | this.getBlue();
	}

	public Colour makeDarker(final float darkness) {
		final int red = getRed();
		final int green = getGreen();
		final int blue = getBlue();
		final int darkerRed0 = (int) (red * darkness);
		final int darkerGreen0 = (int) (green * darkness);
		final int darkerBlue0 = (int) (blue * darkness);
		final int darkerRed1 = Math.max(red - darkerRed0, 0x0);
		final int darkerGreen1 = Math.max(green - darkerGreen0, 0x0);
		final int darkerBlue1 = Math.max(blue - darkerBlue0, 0x0);
		return new Colour(darkerRed1, darkerGreen1, darkerBlue1);
	}

	public Colour makeLighter(final float whiteness) {
		final int red = getRed();
		final int green = getGreen();
		final int blue = getBlue();
		final int lighterRed0 = (int) ((0xff - red) * whiteness);
		final int lighterGreen0 = (int) ((0xff - green) * whiteness);
		final int lighterBlue0 = (int) ((0xff - blue) * whiteness);
		final int lighterRed1 = Math.min(red + lighterRed0, 0xff);
		final int lighterGreen1 = Math.min(green + lighterGreen0, 0xff);
		final int lighterBlue1 = Math.min(blue + lighterBlue0, 0xff);
		return new Colour(lighterRed1, lighterGreen1, lighterBlue1);
	}

	public Colour mix(final Colour otherColour, final float mixRatio) {
		Checker.notNull("parameter:otherColour", otherColour);
		final int red = getRed();
		final int green = getGreen();
		final int blue = getBlue();
		final int otherRed = otherColour.getRed();
		final int otherGreen = otherColour.getGreen();
		final int otherBlue = otherColour.getBlue();
		final float otherRatio = 1.0f - mixRatio;
		final int mixedRed = (int) (red * mixRatio + otherRed * otherRatio);
		final int mixedGreen = (int) (green * mixRatio
				+ otherGreen * otherRatio);
		final int mixedBlue = (int) (blue * mixRatio + otherBlue * otherRatio);
		return new Colour(mixedRed, mixedGreen, mixedBlue);
	}

	public String toCssColour() {
		final int rgb = this.getRed() * 0x10000 + this.getGreen() * 0x100
				+ this.getBlue();
		final String rgbHexString = Integer.toHexString(rgb);
		final String string = '#' + Utilities.padLeft(rgbHexString, 6, '0');
		return string;
	}

	public String toString() {
		return // super.toString() + ", colour:
		"0x" + Utilities.padLeft(Integer.toHexString(red), 2, '0')
				+ Utilities.padLeft(Integer.toHexString(green), 2, '0')
				+ Utilities.padLeft(Integer.toHexString(blue), 2, '0');
	}

	void setBlue(final int blue) {
		Checker.between("parameter:blue", blue, 0,
				Constants.COLOUR_COMPONENT_VALUE + 1);
		this.blue = blue;
	}

	void setGreen(final int green) {
		Checker.between("parameter:green", green, 0,
				Constants.COLOUR_COMPONENT_VALUE + 1);
		this.green = green;
	}

	void setRed(final int red) {
		Checker.between("parameter:red", red, 0,
				Constants.COLOUR_COMPONENT_VALUE + 1);
		this.red = red;
	}
}
