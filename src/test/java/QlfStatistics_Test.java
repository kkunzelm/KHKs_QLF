import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.*;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

class QlfStatistics_Test {
	private QlfStatistics_ qlf = new QlfStatistics_();

	@Test
	void setup() {
		assertEquals(qlf.setup("", new ImagePlus()), 8);
	}

	@Test
    void run() {
		// create Image
		int width = 10;
		int height = 10;
		// Constructs a BufferedImage of one of the predefined image types.
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		// Create a graphics which can be used to draw into the buffered image
		Graphics2D g2d = bufferedImage.createGraphics();
		// fill all the image with 100 gray values
		int color = 0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				g2d.setColor(new Color(color, color, color));
				g2d.fillRect(i, j, 1, 1);

				color++;
			}
		}
		// Disposes of this graphics context and releases any system resources that it
		// is using.
		g2d.dispose();

		// Create IJ Image with 32 Bit float Grey Depth
		ImagePlus imagePlus = new ImagePlus();
		imagePlus.setImage(bufferedImage);
		imagePlus.setTitle("Test Image");

		ImageConverter ic = new ImageConverter(imagePlus);
		ic.convertToGray32();

		ImageProcessor imageProcessor = imagePlus.getProcessor();

		// run plugin methods
		qlf.setup("", imagePlus);
		qlf.run(imageProcessor);

		// compare results to ground truth
		ResultsTable resultsTable = qlf.textPanel.getOrCreateResultsTable();
		assertEquals(resultsTable.getStringValue(1, 0), "Test Image");
		assertEquals(resultsTable.getStringValue(1, 1), "0.000");
		assertEquals(resultsTable.getStringValue(1, 2), "99.000");
		assertEquals(resultsTable.getStringValue(1, 3), "49.500");
		assertEquals(resultsTable.getStringValue(1, 4), "29.011");
		assertEquals(resultsTable.getStringValue(1, 5), "0.000");
		assertEquals(resultsTable.getStringValue(1, 6), "1.000");
		assertEquals(resultsTable.getStringValue(1, 7), "4.000");
		assertEquals(resultsTable.getStringValue(1, 8), "9.000");
		assertEquals(resultsTable.getStringValue(1, 9), "19.000");
		assertEquals(resultsTable.getStringValue(1, 10), "24.000");
		assertEquals(resultsTable.getStringValue(1, 11), "49.000");
		assertEquals(resultsTable.getStringValue(1, 12), "74.000");
		assertEquals(resultsTable.getStringValue(1, 13), "79.000");
		assertEquals(resultsTable.getStringValue(1, 14), "89.000");
		assertEquals(resultsTable.getStringValue(1, 15), "94.000");
		assertEquals(resultsTable.getStringValue(1, 16), "97.000");
		assertEquals(resultsTable.getStringValue(1, 17), "98.000");
		assertEquals(resultsTable.getStringValue(1, 18), "100.000");
		assertEquals(resultsTable.getStringValue(1, 19), "99.000");
		assertEquals(resultsTable.getStringValue(1, 20), "0.000");
	}

	@Test
	void showAbout() {
	}
}