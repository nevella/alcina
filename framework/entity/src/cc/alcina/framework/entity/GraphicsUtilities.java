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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 *
 * @author Nick Reddel
 */
public class GraphicsUtilities implements SwingConstants {
	public static void debugLayout(Component c, int indent) {
		if (c instanceof JComponent) {
			JComponent jc = (JComponent) c;
			if (jc.isVisible() == false) {
				return;
			}
		}
		for (int i = 0; i < indent; i++) {
			System.err.print(' ');
		}
		System.err.println(String.format("-%s : min=%s, max=%s, pref=%s",
				c.getClass().getSimpleName(), shortDim(c.getMinimumSize()),
				shortDim(c.getMaximumSize()), shortDim(c.getPreferredSize())));
		if (c instanceof Container) {
			Container ct = (Container) c;
			Component[] components = ct.getComponents();
			for (Component component : components) {
				debugLayout(component, indent + 4);
			}
		}
	}

	public static void paintTriangle(Graphics g, int x, int y, int size,
			int direction, Color color) {
		Color oldColor = g.getColor();
		int mid, i, j;
		j = 0;
		size = Math.max(size, 2);
		mid = (size / 2) - 1;
		g.translate(x, y);
		g.setColor(color);
		switch (direction) {
		case NORTH:
			for (i = 0; i < size; i++) {
				g.drawLine(mid - i, i, mid + i, i);
			}
			break;
		case SOUTH:
			j = 0;
			for (i = size - 1; i >= 0; i--) {
				g.drawLine(mid - i, j, mid + i, j);
				j++;
			}
			break;
		case WEST:
			for (i = 0; i < size; i++) {
				g.drawLine(i, mid - i, i, mid + i);
			}
			break;
		case EAST:
			j = 0;
			for (i = size - 1; i >= 0; i--) {
				g.drawLine(j, mid - i, j, mid + i);
				j++;
			}
			break;
		}
		g.translate(-x, -y);
		g.setColor(oldColor);
	}

	public static String shortDim(Dimension d) {
		if (d.width > 2000 || d.height > 2000) {
			return "";
		}
		return "[" + d.width + "," + d.height + "]";
	}
}
