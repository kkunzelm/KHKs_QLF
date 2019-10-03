import java.net.URL;

import ij.IJ;
import ij.ImageJ;

/**
 * Run Plugin in IDE.
 * All Plugins from plugins.dir will be listed in the Plugins menu.
 *
 * Taken from:
 * https://github.com/imagej/example-legacy-plugin/blob/master/src/main/java/com/mycompany/imagej/Process_Pixels.java
 */
public class TestPlugin {
    public static void main(String[] args) throws Exception {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        // see: https://stackoverflow.com/a/7060464/1207769
        Class<?> clazz = QlfStatistics_Stack.class;
        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        java.io.File file = new java.io.File(url.toURI());
        System.setProperty("plugins.dir", file.getAbsolutePath());

        // start ImageJ
        new ImageJ();

        // run the plugin
        IJ.runPlugIn(clazz.getName(), "");
    }
}
