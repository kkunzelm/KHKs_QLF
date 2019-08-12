/**
 * Kunzelmann: 30.6.10 QLFStatistik für Stack umgeschrieben.
 *             
 * Kunzelmann: 2.3.06
 *
 * Sarakatsanis 5.6.06
 *
 * This plugin calculates the quantiles for float images and stacks.
 * What is meant by "quantile"?
 *
 * Citation from http://en.wikibooks.org/wiki/Statistics/Summary/Quantiles
 *
 * Quantiles are statistics that describe various subdivisions of a
 * frequency distribution into equal proportions.
 * The simplest division that can be envisioned is into
 * two equal halves and the quantile that does this,
 * the median value of the variate, is used also as a measure of
 * central tendency for the distribution.
 *
 * When division is into four parts the values of the variate
 * corresponding to 25%, 50% and 75% of the total distribution are called quartiles.
 *
 * In this program additional quantiles are used.
 *
 *          1, 2, 5, 10, 20, 25, 50, 75, 80, 90, 95, 98, 99
 *
 * Currently these quantiles are hard coded into this plugin. It would be more elegant to use
 * an ini-files for the quantiles. This remains a task for the future.
 *
 * Historically this evalations was used the first time in my habilitation.
 * It was applied to difference images of teeth (before and after wear).
 * The file format was developed by Wolfram Gloger.
 * 
 * In difference images unchanged areas could be zero (at least theorically), which means that the zeros
 * have to be included into the evaluation. Therefore I cannot use the background values which are zero, too, 
 * usually, as a mask for the evaluation. Before this evaluation plugin is called, we set the background zero 
 * values to NaN (Not a Number). Then we evaluate the positive, the negative and the zero values. See my other utilities (setZero2NaN, setNaN2Zero, clearNaNOutside, FillNaN) 
 * to mask background zeros as NaN
 *
 */

import ij.*;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;


public class QuantileStatistics_ implements PlugInFilter {

	ImagePlus orgImg;
        ImageStack imgSt;
        int stackSize;
	TreeMap quantilMap = new TreeMap();

	public int setup(String arg, ImagePlus imp) {
		orgImg = imp;
		return DOES_32;
	}

	public void run(ImageProcessor ip) {

		int posp = 0, negp = 0, zero = 0;
		int width = ip.getWidth();
		int height = ip.getHeight();
                stackSize = orgImg.getStackSize();
                int length = width * height;
		int sumOfPixelsInStack = width * height*stackSize;

                imgSt = orgImg.getStack();

                // I will work on a copy of the array
                // if we would use this array then the image would be changed after sorting
                // but I want to avoid to change the image.
               float [] copyOfImagePixels = new float[sumOfPixelsInStack];


                for (int n=1; n<=stackSize; n++){

                        // the the slice n
                        ip = imgSt.getProcessor(n);

                        // define an array which referes to the pixels of the image
                        float[] arrayOfImagePixels = (float[])ip.getPixels();

                        // usage of arraycopy:
                        // static void arraycopy( Object src, int srcPos, Object dest, int destPos, int length )
                        // Kopiert length viele Einträge des Arrays src ab der Position srcPos in ein Array dest ab der Stelle destPos. Der Typ des Feldes ist egal, es muss nur in beiden Fällen der gleiche Typ sein. Die Methode ist zumindest für große Felder schneller als eine eigene Kopierschleife.

                        System.arraycopy(arrayOfImagePixels, 0, copyOfImagePixels, ((n-1)*length), length);
                }

		// we need a sorted array for the quantile (n-tile) calculation

		java.util.Arrays.sort(copyOfImagePixels);

                // Arrays.sort puts the "NaN" which might be present in an image during sorting
                // at the end of the stack -> the higher index numbers are the NaNs



		// determine the number of positive, negative and zero pixels - because I need later the number of defined pixels as some pixels could have been excluded as NaN

		for (int a=0; a < copyOfImagePixels.length; a++) {
				float f = copyOfImagePixels[a];

					if (f < 0) {negp++;}
					if (f > 0) {posp++;}
					if (f == 0) {zero++;}  // float.NaN wird ignoriert, weil ich nicht weiss, wie man danach fragt
			}


		// classic histogram evaluation starts here

		int nBins = 256;

		int [] histogram = new int[nBins];
		double sum = 0;
		double sum_2 = 0;
                double sum2 = 0;
                double sumc = 0;

		// Find image min and max - using the sorted array

		float min = copyOfImagePixels[0];
		float max = copyOfImagePixels[negp+posp+zero-1];
                
		// Generate histogram
		
		double scale = nBins/(max-min);
		int index;

                // wäre vielleicht eleganter, n in der nächsten Schleife durch if (pixel not NaN) then n = n + 1 zu bestimmen

                int n = posp+negp+zero; //pixel count = number of defined pixels = all which are not NaN


			for (int z=0; z<n; z++) {

                            float v = copyOfImagePixels[z];
                            sum += v;
                            sum_2 += v*v;

                            index = (int)(scale*(v-min));
                            if (index>=nBins)
                               index = nBins-1;
                            histogram[index]++;

			}

		// Mean

		double mean = sum/n;


		// Standard Deviation
                for (int z=0; z<n; z++) {
                    double x = copyOfImagePixels[z];
                    sum2 = sum2 +  Math.pow ((x - mean) , 2);   // (x-mean)^2 or (x-mean)**2
                    sumc = sumc + (x - mean);
                 }
                double variance = (sum2 - (sumc*sumc)/n)/(n - 1);
                double stdDevComp = Math.sqrt(variance);                  // stdDevComp = compensated variance form Wikipedia

		double stdDev = (n*sum_2-sum*sum)/n;
            if (stdDev>0)
                stdDev = Math.sqrt(stdDev/(n-1));
            else
                stdDev = 0;


		/* Kunzelmann: Room for improvement:
		 *
		 * While the above used method to calculate the standard deviation
		 * is correct in theory and will often work well enough, it is
		 * extremely vulnerable to the effects of roundoff error in computer floating point operations.
		 * It is possible to end up taking the square root of a negative number!
		 * The problem, together with a better solution, is described in
		 * Donald Knuth's "The Art of Computer Programming, Volume 2: Seminumerical Algorithms",
		 * section 4.2.2. The solution is to compute mean and standard deviation using a recurrence relation,
		 * like this:

			M(1) = x(1), M(k) = M(k-1) + (x(k) - M(k-1) / k
			S(1) = 0, S(k) = S(k-1) + (x(k) - M(k-1)) * (x(k) - M(k))

			for 2 <= k <= n, then

			sigma = sqrt(S(n) / (n - 1))

			Knuth attributes this method to B.P. Welford, Technometrics, 4,(1962), 419-420.

                 * An even better description can be found here:
                 *       http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
                 *       def compensated_variance(data):
                            n = 0
                            sum1 = 0
                            for x in data:
                                n = n + 1
                                sum1 = sum1 + x
                            mean = sum1/n

                            sum_2 = 0
                            sumc = 0
                            for x in data:
                                sum_2 = sum_2 + (x - mean)**2
                                sumc = sumc + (x - mean)
                            variance = (sum_2 - sumc**2/n)/(n - 1)
                            return variance

                 */




		//set quantil map

		int q[] = { 1, 2, 5, 10, 20, 25, 50, 75, 80, 90, 95, 98, 99};

		for (int j=0; j < q.length; j++) {
			quantilMap.put(new Integer(q[j]), new Integer(0));
		}

		// determine the quantiles as defined in the quantil map

		float oldValue = 0;

		// q = Anzahl der einzelnen Quantile - siehe in Quantilmap oben

		for (int j=0; j < q.length; j++) {

			// Variable i entspricht dem einzelnen Wert (Pixel) im sortierten Array des Float-Bildes
			// qpr ist der Bildpunkt, der dem Prozentwert entspricht
			// Gesamtlänge ist length, 1 % ist length geteilt durch 100
			// durch Multiplikation mit q[i] erhält man den jeweils gewünschten Wert
			// ich muss aber die Länge der definierten Pixel noch bestimmen, damit NaN nicht in die Quantilen
			// einbezogen wird.

			double qpr = (q[j] * (negp+posp+zero) / 100);
				for (int i=0; i < qpr; i++) {

					// oldValue war mit Null vorbelegt, jetzt wird die der Inhalt/Wert des Quantil-Pixels eingetragen

					oldValue = copyOfImagePixels[i];
				}
				quantilMap.put(new Integer(q[j]), new Float(oldValue));
			}




	  // show statistics
    final TextPanel  textPanel = new TextPanel("Title");
    textPanel.setColumnHeadings("Beschreibung	Wert");
    textPanel.appendLine("afile_name	" + orgImg.getTitle());

    textPanel.appendLine("min	" + min);
    textPanel.appendLine("max	" + max);
    textPanel.appendLine("mean	" + mean);
    textPanel.appendLine("stddev	" + stdDev);
    textPanel.appendLine("stddevComp	" + stdDevComp);

    for (int a = 0 ; a < quantilMap.size(); a++) {
    	textPanel.appendLine("quant(" + q[a] + " %)	" + quantilMap.get(new Integer(q[a])));
    }
    textPanel.appendLine("n_defined	" + (posp+negp+zero));
    textPanel.appendLine("n_def_pos	" + posp);
    textPanel.appendLine("n_def_neg	" + negp);
    
    JFrame tw = new JFrame("Quantile Statistics for Difference Images");
    JButton but = new JButton("Save as...");
    but.addActionListener(new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		textPanel.saveAs("");
    	}
    });
    BoxLayout box = new BoxLayout(tw.getContentPane(), BoxLayout.Y_AXIS);
    tw.getContentPane().setLayout(box);
    tw.getContentPane().add(textPanel);
    tw.getContentPane().add(but);
    tw.setBounds(200,200,350,400);
    tw.setVisible(true);

	}


	void showAbout() {
		IJ.showMessage("About QlfLinearStatitics_...","This PlugIn does QLF-Statistics !");
	}


}

