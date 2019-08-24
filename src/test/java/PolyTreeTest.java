import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.*;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ij.ImagePlus;

public class PolyTreeTest {

	ImagePlus imagePlus = new ImagePlus();
	private int[] xPolyCoords = {1, 3, 1, 3};
	private int[] yPolyCoords = {1, 1, 3, 3};

	public void setupTest() {
		// create Image
		int width = 10;
		int height = 10;
		// Constructs a BufferedImage of one of the predefined image types.
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		// Create a graphics which can be used to draw into the buffered image
		Graphics2D g2d = bufferedImage.createGraphics();
		// fill all the image with red
		g2d.setColor(new Color(50, 50, 50));
		g2d.fillRect(0, 0, width, height);
		// Disposes of this graphics context and releases any system resources that it is using.
		g2d.dispose();

		imagePlus.setImage(bufferedImage);
	}

	@Test
	public void getWeightTestInPolygon() {
		setupTest();

		// Build Poly Tree
		PolyTree polyTree = new PolyTree(xPolyCoords, yPolyCoords, 4,
				imagePlus.getProcessor());

        // Test weight at coordinates inside polygon
		assertEquals(50.0, polyTree.getWeight(2, 2));
	}

	@Test
	public void getWeightTestOutsidePolygon() {
		setupTest();

        // Build Poly Tree
		PolyTree polyTree = new PolyTree(xPolyCoords, yPolyCoords, 4,
				imagePlus.getProcessor());

		// Test weight at coordinates outside polygon
		assertEquals(-1.0, polyTree.getWeight(4, 4));
	}
}
