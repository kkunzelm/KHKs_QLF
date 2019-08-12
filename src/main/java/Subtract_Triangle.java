
/**
 * KH: 23.2.2006 
 * Diese Version entspricht der Version, die ich von Hr.Sarakatsanis erhalten habe.
 * Wichtige Information: ich hatte immer das Problem, dass die PolyLine Selektion 
 * die Region of Interest falsch berechnet hat (immer links oben, und mit Fehlern in der Fl�che
 *
 * Ich glaube, den Fehler gefunden zu haben. In dieser Version von ImageJ liefert nur 
 * Polygon ein umhüllendes Rechteck,aber nicht Polyline.
 *
 * Ich habe als Lösung die Abfrage nach Polyline einfach auskommentiert.
 *
 * Wichtig: die ROI muss mit einem Polygon markiert werden. Dann klappt es.
 * Alternativ könnte man natürlich den offset selbst berechnen... aber dazu habe ich im Moment keine Zeit und es klappt ja.
 *
 *
 * Room for improvement: automatisch RGB Bilder in 8-bit umwandeln
 */

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.util.Stack;
import java.util.TreeMap;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Subtract_Triangle implements PlugInFilter {

	static ImagePlus orgImg;
	TreeMap quantilMap = new TreeMap();

	public int setup(String arg, ImagePlus imp) {
		orgImg = imp;
		return DOES_8G + DOES_32 + ROI_REQUIRED;
	}

	public void run(ImageProcessor ip) {

		Rectangle rectRoi = ip.getRoi();

		boolean isByte = false;
		if (orgImg.getType() == ImagePlus.GRAY8) {
			ip = ip.convertToFloat(); // if its 8-bit make 32-bit (methods ONLY running on float!)
			isByte = true; // for converting back if needed
		}

		// KH 23.2.2006: habe bei dieser Abfrage Polyline durch Polygon ersetzt. Jetzt
		// geht das Diff.Bild

		PolygonRoi polyRoi;

		try {
			polyRoi = (PolygonRoi) orgImg.getRoi();
		} catch (ClassCastException e) {
			IJ.error("PlugIn requires Polygon ROI!");
			return;
		}
		if (polyRoi.getType() != Roi.POLYGON) {
			IJ.error("PlugIn requires Polygon ROI!");
			return;
		}

		// Creating polyTree for faster searching (D&C)

		int[] xRoi = polyRoi.getXCoordinates();
		int[] yRoi = polyRoi.getYCoordinates();
		int length = polyRoi.getNCoordinates();

		for (int i = 0; i < length; i++) { // rel. ROI-Coords => total Coords
			xRoi[i] = xRoi[i] + rectRoi.x;
			yRoi[i] = yRoi[i] + rectRoi.y;
		}

		PolyTree tree = new PolyTree(xRoi, yRoi, length, ip);

		// polyTree created

		int offset, x, i, roiX, roiY, roiOffset, roiI;
		int width = ip.getWidth();

		// KH von mir erg�nzt um die H�he des urspr�nglichen Bildes zu bekommen
		int khheight = ip.getHeight();
		// KH ende Einschub
		int roiWidth = rectRoi.width;
		int roiHeight = rectRoi.height;
		float shouldBeColor, aktColor;
		float diff;
		float[] pixels = (float[]) ip.getPixels();

		ImagePlus floatImg = NewImage.createFloatImage("Sub Triangle of " + orgImg.getTitle(), width, ip.getHeight(), 1,
				NewImage.FILL_BLACK);
		ImageProcessor floatProcessor = floatImg.getProcessor();
		float[] floatPixels = (float[]) floatProcessor.getPixels();

		// KH von mir erg�nzt: soll den Hintergrund mit Float.NaN f�llen, so dass die
		// Statistik-Auswertung wieder stimmt
		// nachteil: ich muss die Routinen Clear, Fill f�r Float.NaN statt
		// Vordergrund/HintergrundFarbe anpassen.
		for (int kh = 0; kh < width * khheight; kh++) {
			floatPixels[kh] = Float.NaN;
		}
		// KH Ende Einschub

		roiY = 0;
		roiI = -1;

		double progress = 0;

		for (int y = rectRoi.y; y < (rectRoi.y + roiHeight); y++, roiY++) {
			offset = y * width;
			roiOffset = roiY * roiWidth;
			for (x = rectRoi.x, roiX = 0; x < (rectRoi.x + roiWidth); x++, roiX++) {
				i = offset + x;
				try { // just to be sure, not needed (try{} catch{})
					roiI = roiOffset + roiX;
					shouldBeColor = tree.getWeight(x, y);
					aktColor = pixels[i];
					// Lineare Berechnung
					if (shouldBeColor > -1) { // Pt. is in Polygon
						diff = (aktColor - shouldBeColor);
						floatPixels[i] = diff;
					}

				} catch (Exception e) {
					System.err.println("Pt. (" + x + "/" + y + ") with index " + i
							+ " is out of bounds (in ROI is ind: " + roiI + ")!");
				}
			}
			progress = ((double) (y - rectRoi.y)) / ((double) (roiHeight));
			// System.out.println("Progress is: " + progress);
			IJ.showProgress(progress);
			// IJ.showMessage("Progress is: " + progress);
		}

		floatProcessor.resetMinAndMax();
		double minDisplay = floatProcessor.getMin();
		double maxDisplay = floatProcessor.getMax();

		// floatImg.show();

		// Calculate which value in the range from 0 to 255 is equivalent to zero in
		// float image

		int zeroZ = (int) ((Math.abs(minDisplay) / (Math.abs(minDisplay) + Math.abs(maxDisplay))) * 255);

		double inclinationLutRed = 255.0 / zeroZ;

		// Position auf der X-Achse der LUT ab der positive Z Werte vorliegen.
		// posLutX=position on x-axis of the LUT of positive z-values,
		// inclinationLutGrey = inclination of the LUT curve for positive z-values

		int posLutX = 255 - zeroZ;
		double inclinationLutGrey = 255.0 / (double) posLutX;

		// define the array for LUT calculation

		byte[] reds = new byte[256];
		byte[] greens = new byte[256];
		byte[] blues = new byte[256];

		// LUT array is filled with values.
		// LUT for red only from negative numbers to zero
		// LUT for grey from zero to positive numbers

		int lutVal = -1;

		// Original von S. Klein war: for (int j=0; j<256; j++) {
		// habe es angepasst, da ich NaN als schwarz und nicht rot haben wollte

		for (int j = 1; j < 256; j++) {
			if (j < zeroZ) {
				lutVal = (int) (255.0 - (inclinationLutRed * j));
				reds[j] = (byte) lutVal;
				greens[j] = (byte) 0;
				blues[j] = (byte) 0;
			} else {
				lutVal = (int) (inclinationLutGrey * (j - zeroZ));

				reds[j] = (byte) (lutVal);
				greens[j] = (byte) (lutVal);
				blues[j] = (byte) (lutVal);
			}
		}

		IndexColorModel cm = new IndexColorModel(8, 256, reds, greens, blues);
		floatProcessor.setColorModel(cm);

		// converting back if it was a 8-bit
		ImagePlus byteImage = null;
		ImagePlus ci = null;
		/**
		 * kh: Ich habe festgestellt, byte Bilder Rechen-Fehler bei der Auswertung
		 * aufweisen kh: es spricht nichts dagegen, dass das Differenzbild float bleibt
		 *
		 * if(isByte) { ImageProcessor bp = floatProcessor.convertToByte(true);
		 * byteImage = new ImagePlus("Sub Triangle of " + orgImg.getTitle(),bp); ci =
		 * byteImage; byteImage.show(); } else {
		 */
		ci = floatImg;
		floatImg.show();
		// kh: }

		// removing roi
		for (i = 0; i < length; i++) { // rel. ROI-Coords => total Coords
			xRoi[i] = xRoi[i] - rectRoi.x;
			yRoi[i] = yRoi[i] - rectRoi.y;
		}
	}

	void showAbout() {
		IJ.showMessage("About SubstractTriangle...", "This PlugIn does linear interpolation!");
	}

	private class PolyTree {

		private Polygon polygon;
		private PolyTree left = null;
		private PolyTree right = null;
		private Gauss g = null;
		private float[] colors = new float[3];

		PolyTree(int[] x, int[] y, int length, ImageProcessor ip) {

			setHullPolygon(x, y, length);

			if (x.length > 3) { // more than 3 corners, so do D&C

				// printArray("Sawline:",x,y,length);

				int z = (length + 2) / 2;
				int[] x1 = new int[z];
				int[] y1 = new int[z];
				int[] x2 = new int[(length + 2 - z)];
				int[] y2 = new int[(length + 2 - z)];

				for (int i = 0; i < z; i++) {
					x1[i] = x[i];
					y1[i] = y[i];
				}
				int k = 0;
				for (int i = (z - 2); i < length; i++) {
					x2[k] = x[i];
					y2[k] = y[i];
					k++;
				}
				left = new PolyTree(x1, y1, x1.length, ip);
				right = new PolyTree(x2, y2, x2.length, ip);

			} else if (x.length == 3) { // Polygon has become Triangle, so initialize Gauss
				g = new Gauss(x[0], y[0], x[1], y[1], x[2], y[2]);

				// colors[0] = ip.getPixel(x[0],y[0]);
				// colors[1] = ip.getPixel(x[1],y[1]);
				// colors[2] = ip.getPixel(x[2],y[2]);

				colors[0] = ip.getPixelValue(x[0], y[0]);
				colors[1] = ip.getPixelValue(x[1], y[1]);
				colors[2] = ip.getPixelValue(x[2], y[2]);

				// System.out.println("Triangle reached:");
				// System.out.println("X: " + x[0] + "\t" + x[1] + "\t" + x[2]);
				// System.out.println("Y: " + y[0] + "\t" + y[1] + "\t" + y[2]);
				// System.out.println("C: " +
				// ip.getPixel(x[0],y[0])+"\t"+ip.getPixel(x[1],y[1])+"\t"+ip.getPixel(x[2],y[2]));

			} else if (x.length < 3) {
				System.out.println("Polygon with less than 3 corners computed => ERROR!");
			}
		}

		void setHullPolygon(int[] x, int[] y, int length) {

			int[] xCoords = new int[length];
			int[] yCoords = new int[length];

			int r = 0;
			Stack stack = new Stack();
			for (int i = 0; i < length; i++) {

				if ((i % 2) == 0) { // equal nr
					xCoords[r] = x[i];
					yCoords[r] = y[i];
					r++;
				} else { // odd nr
					stack.push(new Point(x[i], y[i]));
				}
				// System.out.println("X: " + xCoords[i] + "\tY:" + yCoords[i]);
			}
			Point point;
			while (!stack.empty()) {
				point = (Point) stack.pop();
				xCoords[r] = point.x;
				yCoords[r] = point.y;
				r++;
			}
			polygon = new Polygon(xCoords, yCoords, length);

		}

		/**
		 * Gets the weight of a point if it's in a triangle return null if point isn't
		 * in polygon
		 */
		float getWeight(int x, int y) {
			if (!polygon.contains(x, y)) {
				return -1;
			}
			float sum;
			if (g != null) { // is triangle => solving
				float[] weight = g.solve(x, y);
				sum = (weight[0] * colors[0] + weight[1] * colors[1] + weight[2] * colors[2]);
				// System.out.println("Pt 0 has color " + colors[0] + " and has weight " +
				// weight[0]);
				// System.out.println("Pt 1 has color " + colors[1] + " and has weight " +
				// weight[1]);
				// System.out.println("Pt 2 has color " + colors[2] + " and has weight " +
				// weight[2]);
				// System.out.println("Sum is: " + sum);
				return sum;
			} else { // must be a node
				sum = left.getWeight(x, y);
				if (sum == -1) { // was not in left
					sum = right.getWeight(x, y);
					if (sum == -1) {
						System.err.println("ERROR: Point (" + x + "/" + y + ") was not in Triangle!");
						printArray("Bounding polygon was:", polygon.xpoints, polygon.ypoints, polygon.npoints);
						return -1;
					} else {
						return sum;
					}
				} else {
					return sum;
				}
			}
		}

		void printArray(String label, int[] x, int[] y, int length) {
			System.out.println(label);
			System.out.print("X: ");
			for (int i = 0; i < x.length; i++) {
				System.out.print(x[i] + "\t");
			}
			System.out.println();
			System.out.print("Y: ");
			for (int i = 0; i < x.length; i++) {
				System.out.print(y[i] + "\t");
			}
			System.out.println();
		}
	}
}
