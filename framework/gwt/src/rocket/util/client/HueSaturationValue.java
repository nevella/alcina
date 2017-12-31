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

/**
 * The hue saturation value class represents a way to represent a colour.
 * Instances may be used to update or create a new {@link Colour}.
 * 
 * @author Miroslav Pokorny
 */
public class HueSaturationValue {
	/**
	 * The hue component
	 */
	private float hue;

	/**
	 * The saturation component of this colour expression
	 */
	private float saturation;

	/**
	 * The value or brightness of this colour expression.
	 */
	private float value;

	public HueSaturationValue(final float hue, final float saturation,
			final float value) {
		super();
		this.setHue(hue);
		this.setSaturation(saturation);
		this.setValue(value);
	}

	/**
	 * Creates a Colour to hold the equivalent colour value.
	 * 
	 * @return The new colour
	 */
	public Colour asColour() {
		final float saturation = this.getSaturation();
		int red = 0;
		int green = 0;
		int blue = 0;
		while (true) {
			final float value = this.getValue();
			if (Tester.equals(saturation, 0.0, 0.01)) {
				red = this.toInteger(value);
				green = red;
				blue = red;
				break;
			}
			final float hue = this.getHue() * 360;
			final float hueTemp = hue / 60;
			final float i = (float) Math.floor(hueTemp);
			final float f = hueTemp - i;
			final int p = this.toInteger(value * (1 - saturation));
			final int q = this.toInteger(value * (1 - (saturation * f)));
			final int t = this.toInteger(value * (1 - (saturation * (1 - f))));
			final int v = this.toInteger(value);
			final int pick = (int) hueTemp;
			if (pick == 0) {
				red = v;
				green = t;
				blue = p;
				break;
			}
			if (pick == 1) {
				red = q;
				green = v;
				blue = p;
				break;
			}
			if (pick == 2) {
				red = p;
				green = v;
				blue = t;
				break;
			}
			if (pick == 3) {
				red = p;
				green = q;
				blue = v;
				break;
			}
			if (pick == 4) {
				red = t;
				green = p;
				blue = v;
				break;
			}
			if (pick == 5) {
				red = v;
				green = p;
				blue = q;
			}
			break;
		}
		return new Colour(red, green, blue);
	}

	public boolean equals(final HueSaturationValue otherHsv) {
		return otherHsv == null ? false
				: Tester.equals(this.getHue(), otherHsv.getHue(), 0.05f)
						&& Tester.equals(this.getSaturation(),
								otherHsv.getSaturation(), 0.05f)
						&& Tester.equals(this.getValue(), otherHsv.getValue(),
								0.05f);
	}

	public boolean equals(final Object otherObject) {
		return otherObject instanceof Colour ? this.equals((Colour) otherObject)
				: false;
	}

	public float getHue() {
		return hue;
	}

	public float getSaturation() {
		return saturation;
	}

	public float getValue() {
		return value;
	}

	public int hashCode() {
		return ((int) this.getHue() * 256 * 256 * 256)
				^ ((int) this.getSaturation() * 256 * 256)
				^ ((int) this.getValue() * 256);
	}

	public String toString() {
		return "hsv: " + this.hue + ", " + this.saturation + ", " + this.value;
	}

	protected int toInteger(final float floatValue) {
		return (int) (floatValue * Constants.COLOUR_COMPONENT_VALUE);
	}

	void setHue(final float hue) {
		Checker.between("parameter:hue", hue, 0, 1.01f);
		this.hue = hue;
	}

	void setSaturation(final float saturation) {
		Checker.between("parameter:saturation", saturation, 0, 1.01f);
		this.saturation = saturation;
	}

	void setValue(final float value) {
		Checker.between("parameter:value", value, 0, 1.01f);
		this.value = value;
	}
}
