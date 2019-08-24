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
 */

import java.awt.*;
import java.awt.image.IndexColorModel;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

public class SubtractTrianglePerc_ implements PlugInFilter {

	private static ImagePlus orgImg;

	public int setup(String arg, ImagePlus imp) {

		// convert images to Gray, 32 Bit float
		try {
			ImageConverter ic = new ImageConverter(imp);
			ic.convertToGray32();
		} catch (Exception e) {
			System.out.println(e);
		}

		orgImg = imp;
		return DOES_8G + DOES_32 + ROI_REQUIRED;
	}

	public void run(ImageProcessor ip) {

		Rectangle rectRoi = ip.getRoi();

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

					// Prozentuale Berechnung
					if (shouldBeColor > -1) { // Pt. is in Polygon
						// diff = (aktColor / shouldBeColor) * 100; // w�rde Werte von 0 bis 200 Prozent
						// ergeben (je
						// 100 Proz �ber "Nullwert"= 100 % und 100 Proz
						// unter "Nullwert"
						diff = ((aktColor / shouldBeColor) * 100) - 100; // dunkel Stellen = negativ, helle = positiv
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
		// ImagePlus byteImage = null;
		// ImagePlus ci = null;
		/**
		 * kh: Ich habe festgestellt, byte Bilder Rechen-Fehler bei der Auswertung
		 * aufweisen kh: es spricht nichts dagegen, dass das Differenzbild float bleibt
		 *
		 * if(isByte) { ImageProcessor bp = floatProcessor.convertToByte(true);
		 * byteImage = new ImagePlus("Sub Triangle of " + orgImg.getTitle(),bp); ci =
		 * byteImage; byteImage.show(); } else {
		 */
		// ci = floatImg;
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

}
