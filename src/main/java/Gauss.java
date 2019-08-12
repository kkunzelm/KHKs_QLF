
class Gauss {

	private final float[][] matrix = new float[3][4];
	private final float[] coeffs = new float[3];

	/**
	 * Contructs a new Object.
	 *
	 * @param x1
	 *            = the X-Coord of the 1. Corner
	 * @param y1
	 *            = the Y-Coord of the 1. Corner
	 * @param x2
	 *            = the X-Coord of the 2. Corner
	 * @param y2
	 *            = the Y-Coord of the 2. Corner
	 * @param x3
	 *            = the X-Coord of the 3. Corner
	 * @param y3
	 *            = the Y-Coord of the 3. Corner
	 */
	Gauss(int x1, int y1, int x2, int y2, int x3, int y3) {

		matrix[0][0] = x1; // X of. 1. Corner
		matrix[0][1] = x2; // X of. 2. Corner
		matrix[0][2] = x3; // X of. 3. Corner
		matrix[0][3] = -1.0f; // X of. Point, to set in solve(xP,yP)

		matrix[1][0] = y1; // Y of. 1. Corner
		matrix[1][1] = y2; // Y of. 2. Corner
		matrix[1][2] = y3; // Y of. 3. Corner
		matrix[1][3] = -1.0f; // Y of. Point, to set in solve(xP,yP)

		matrix[2][0] = 1.0f; // c1 + c2 + c3 = 1 ALLWAYS!!! (cause Point is in konvex hull!)
		matrix[2][1] = 1.0f;
		matrix[2][2] = 1.0f;
		matrix[2][3] = 1.0f;

		computeStairs();

	}

	/*
	 * Just for testing.
	 */
	public static void main(String[] argv) {

		Gauss g = new Gauss(1, 1, 3, 1, 1, 3);
		// Gauss g = new Gauss(12,2,14,2,12,4);
		// Gauss g = new Gauss(45,50,321,38,124,400);

		float[] c = g.solve(2, 2);
		// float[] c = g.solve(13,3);
		// float[] c = g.solve(150,150);

		System.out.println("\nmatrix in stairform:");
		g.printMatrix();

		System.out.println("\nResult of (2/2):\nC1=" + c[0] + "   C2=" + c[1] + "   C3=" + c[2]);

	}

	/*
	 * Computes the stairform of the matrix, DOESNT initialize the right part.
	 */
	private void computeStairs() {

		float aktCoeff;
		int j, k;

		for (int i = 0; i < 2; i++) { // i is columnnr

			for (j = (i + 1); j < 3; j++) { // j is rownr

				aktCoeff = matrix[j][i] / matrix[i][i];

				// init coeffs
				if (i == 0) // first rows coeffs
					coeffs[j - 1] = aktCoeff;
				else // second rows coeff
					coeffs[2] = aktCoeff;

				// compute matrixs left part
				for (k = 0; k < 3; k++) { // substract lines
					matrix[j][k] = (matrix[j][k] - (aktCoeff * matrix[i][k]));
				}

			}

		}
	}

	/*
	 * Computes c1,c2,c3 and overwrites the matrixs right part
	 *
	 * @param xP = the X-Coord of a point INSIDE the 3 cornerpoints
	 *
	 * @param yP = the Y-Coord of a point INSIDE the 3 cornerpoints
	 *
	 * @return the weight of the 3 corner vectors constructing xP/yP
	 */
	float[] solve(int xP, int yP) {

		matrix[0][3] = xP;
		matrix[1][3] = (yP - (xP * coeffs[0]));
		matrix[2][3] = (1.0f - (xP * coeffs[1]) - (matrix[1][3] * coeffs[2]));

		float[] c = new float[3];
		c[2] = matrix[2][3] / matrix[2][2];
		c[1] = ((matrix[1][3] - (c[2] * matrix[1][2])) / matrix[1][1]);
		c[0] = ((matrix[0][3] - ((c[2] * matrix[0][2]) + (c[1] * matrix[0][1]))) / matrix[0][0]);
		return c;
	}

	private void printMatrix() {
		System.out.println(matrix[0][0] + "*C1 + " + matrix[0][1] + "*C2 + " + matrix[0][2] + "*C3 = " + matrix[0][3]);
		System.out.println(matrix[1][0] + "*C1 + " + matrix[1][1] + "*C2 + " + matrix[1][2] + "*C3 = " + matrix[1][3]);
		System.out.println(matrix[2][0] + "*C1 + " + matrix[2][1] + "*C2 + " + matrix[2][2] + "*C3 = " + matrix[2][3]);

	}
}