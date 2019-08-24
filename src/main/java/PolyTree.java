import ij.process.ImageProcessor;

import java.awt.*;
import java.util.Stack;

class PolyTree {

    private Polygon polygon;
    private PolyTree left = null;
    private PolyTree right = null;
    private Gauss g = null;
    private final float[] colors = new float[3];

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
        Stack<Point> stack = new Stack<>();
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
            point = stack.pop();
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
                    printArray(polygon.xpoints, polygon.ypoints);
                    return -1;
                } else {
                    return sum;
                }
            } else {
                return sum;
            }
        }
    }

    void printArray(int[] x, int[] y) {
        System.out.println("Bounding polygon was:");
        System.out.print("X: ");
        for (int value : x) {
            System.out.print(value + "\t");
        }
        System.out.println();
        System.out.print("Y: ");
        for (int i = 0; i < x.length; i++) {
            System.out.print(y[i] + "\t");
        }
        System.out.println();
    }
}