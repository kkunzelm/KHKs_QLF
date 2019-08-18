
/**
 * KH: 14.1.2013
 * added support for stack
 *
 *
 * KH: 2.3.06
 *
 * korrigiert: Sarakatsanis 5.6.06
 *
 * Berechnen der Quantile eines Float-Bildes
 * Quantile sind im source-Code festgelegt.
 * Wäre eleganter, diese über ein Ini-File zu definieren.
 * die restliche Histogram-Statistik erhält man über den Menü-Punkt Analyze -> "Histogram"
 *
 */

import java.util.TreeMap;

import javax.swing.*;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextPanel;

public class QlfStatistics_ implements PlugInFilter {

	private static ImagePlus orgImg;
	private final TreeMap<Integer, Float> quantilMap = new TreeMap<>();
	final TextPanel textPanel = new TextPanel("Title");

	public int setup(String arg, ImagePlus imp) {
		orgImg = imp;
		return DOES_32;
	}

	public void run(ImageProcessor ip) {

		int posp = 0, negp = 0, zero = 0;
		int width = ip.getWidth();
		int height = ip.getHeight();
		int length = width * height;

		// define an array which referes to the pixels of the image

		float[] arrayOfImagePixels = (float[]) ip.getPixels();

		// I will work on a copy of the array
		// if we use this array then the image is changed after sorting

		float[] copyOfImagePixels = new float[length];

		System.arraycopy(arrayOfImagePixels, 0, copyOfImagePixels, 0, length);

		// we need a sorted array for the quantile (n-tile) calculation

		java.util.Arrays.sort(copyOfImagePixels);

		// determine the number of positive, negative and zero pixels

		for (int a = 0; a < length; a++) {
			float f = copyOfImagePixels[a];

			if (f < 0) {
				negp++;
			}
			if (f > 0) {
				posp++;
			}
			if (f == 0) {
				zero++;
			} // float.NaN wird ignoriert, weil ich nicht weiss, wie man danach fragt
		}

		// classic histogram evaluation starts here

		int nBins = 256;

		int[] histogram = new int[nBins];
		float sum = 0;
		float sum2 = 0;

		// Find image min and max - using the sorted array

		float min = copyOfImagePixels[0];
		float max = copyOfImagePixels[negp + posp + zero - 1];

		// Generate histogram

		double scale = nBins / (max - min);
		int index;
		int n = posp + negp + zero; // pixel count = number of defined pixels

		for (int z = 0; z < n; z++) {

			float v = copyOfImagePixels[z];

			sum += v;
			sum2 += v * v;
			index = (int) (scale * (v - min));
			if (index >= nBins)
				index = nBins - 1;
			histogram[index]++;

		}

		// Mean

		double mean = sum / n;

		// Standard Deviation

		double stdDev = (n * sum2 - sum * sum) / n;
		if (stdDev > 0)
			stdDev = Math.sqrt(stdDev / (n - 1));
		else
			stdDev = 0;

		/*
		 * KH: Room for improvement:
		 *
		 * While the above used method to calculate the standard deviation is correct in
		 * theory and will often work well enough, it is extremely vulnerable to the
		 * effects of roundoff error in computer floating point operations. It is
		 * possible to end up taking the square root of a negative number! The problem,
		 * together with a better solution, is described in Donald Knuth's
		 * "The Art of Computer Programming, Volume 2: Seminumerical Algorithms",
		 * section 4.2.2. The solution is to compute mean and standard deviation using a
		 * recurrence relation, like this:
		 * 
		 * M(1) = x(1), M(k) = M(k-1) + (x(k) - M(k-1) / k S(1) = 0, S(k) = S(k-1) +
		 * (x(k) - M(k-1)) * (x(k) - M(k))
		 * 
		 * for 2 <= k <= n, then
		 * 
		 * sigma = sqrt(S(n) / (n - 1))
		 * 
		 * Knuth attributes this method to B.P. Welford, Technometrics, 4,(1962),
		 * 419-420.
		 */

		// set quantil map

		int[] q = {1, 2, 5, 10, 20, 25, 50, 75, 80, 90, 95, 98, 99};

		for (int value : q) {
			quantilMap.put(value, 0f);
		}

		// determine the quantiles as defined in the quantil map

		float oldValue = 0;

		// q = Anzahl der einzelnen Quantile - siehe in Quantilmap oben

		for (int value : q) {

			// Variable i entspricht dem einzelnen Wert (Pixel) im sortierten Array des
			// Float-Bildes
			// qpr ist der Bildpunkt, der dem Prozentwert entspricht
			// Gesamtl�nge ist length, 1 % ist length geteilt durch 100
			// durch Multiplikation mit q[i] erh�lt man den jeweils gew�nschten Wert
			// ich muss aber die L�nge der definierten Pixel noch bestimmen, damit NaN nicht
			// in die Quantilen einbezogen wird.

			double qpr = (value * (negp + posp + zero) / 100.0);
			for (int i = 0; i < qpr; i++) {

				// oldValue war mit Null vorbelegt, jetzt wird die der Inhalt/Wert des
				// Quantil-Pixels eingetragen

				oldValue = copyOfImagePixels[i];
			}
			quantilMap.put(value, oldValue);
		}

		// show statistics
		textPanel.setColumnHeadings("Beschreibung	Wert");
		textPanel.appendLine("afile_name	" + orgImg.getTitle());

		textPanel.appendLine("min	" + min);
		textPanel.appendLine("max	" + max);
		textPanel.appendLine("mean	" + mean);
		textPanel.appendLine("stddev	" + stdDev);

		for (int a = 0; a < quantilMap.size(); a++) {
			textPanel.appendLine("quant(" + q[a] + " %)	" + quantilMap.get(q[a]));
		}
		textPanel.appendLine("n_defined	" + (posp + negp + zero));
		textPanel.appendLine("n_def_pos	" + posp);
		textPanel.appendLine("n_def_neg	" + negp);

		JFrame tw = new JFrame("Statistics for linear interpolation");
		JButton but = new JButton("Save as...");
		but.addActionListener(e -> textPanel.saveAs(""));
		BoxLayout box = new BoxLayout(tw.getContentPane(), BoxLayout.Y_AXIS);
		tw.getContentPane().setLayout(box);
		tw.getContentPane().add(textPanel);
		tw.getContentPane().add(but);
		tw.setBounds(200, 200, 350, 400);
		tw.setVisible(true);

	}

	void showAbout() {
		IJ.showMessage("About QlfLinearStatitics_...", "This PlugIn does QLF-Statistics !");
	}

}
