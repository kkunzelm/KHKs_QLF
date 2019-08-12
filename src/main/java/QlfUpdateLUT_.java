
/**
 * KH: 7.3.2006 
 * update lookup table of flat diff images, for example after Threshold operations etc.
 */

import java.awt.image.IndexColorModel;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class QlfUpdateLUT_ implements PlugInFilter {

	private static ImagePlus orgImg;

	public int setup(String arg, ImagePlus imp) {
		orgImg = imp;
		return DOES_32;
	}

	public void run(ImageProcessor ip) {

		if (orgImg.getType() == ImagePlus.GRAY8) {
			ip = ip.convertToFloat(); // if its 8-bit make 32-bit (methods ONLY running on float!)
		}

		ImageProcessor floatProcessor = orgImg.getProcessor();

		floatProcessor.resetMinAndMax();
		ContrastEnhancer ce = new ContrastEnhancer(); // KHK todo check again whether this is what I expect it to be
		ce.stretchHistogram(floatProcessor, 0.1);

		ImageStatistics stats;
		stats = ImageStatistics.getStatistics(ip, DOES_32, null);
		double sd = stats.stdDev;

		double minDisplay = floatProcessor.getMin() + 0.5 * sd;
		double maxDisplay = floatProcessor.getMax() - 0.5 * sd;

		System.out.println("Statistics: SD = " + sd + " , minDisplay = " + minDisplay + " , maxDisplay = " + maxDisplay
				+ "  min/max: " + floatProcessor.getMin() + "/" + floatProcessor.getMax());

		// ip.show();

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

		int lutVal;

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

		orgImg.show();

	}

	void showAbout() {
		IJ.showMessage("About QlfUpdateLUT", "This PlugIn updates the LUT in QLF Style!");
	}

}
