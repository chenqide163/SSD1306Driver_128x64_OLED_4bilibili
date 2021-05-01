package oled.oled;

import com.pi4j.io.i2c.I2CFactory;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class OledMain {
    private static Logger LOGGER = Logger.getLogger(OledMain.class);
    public static void main(String[] args) throws IOException, I2CFactory.UnsupportedBusNumberException, InterruptedException {
        LOGGER.debug("begin to show~");
        OLEDDisplayDriver.getInstance().initOLEDDisplay();

        //后台显示树莓派中的可用字体
        displaySupportFonts();

        OLEDDisplayDriver.getInstance().display(DrawImg.getCover());
        Thread.sleep(2000);

        //展示jar包同级目录下的所有jpg图片
        showPicsFromSameFolder();
        //显示爬虫假数据
        DrawImg.printCrawlerData();
    }

    /**
     * 获取jar包同路径下的所有后缀为jpg的图片并展示
     */
    public static void showPicsFromSameFolder() throws IOException, InterruptedException {
        String path = OledMain.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.substring(0, path.lastIndexOf(File.separator) + 1);
        System.out.println(path);
        File file = new File(path);
        File[] files = file.listFiles();
        for (File eachFile : files) {
            if (eachFile.getName().toLowerCase().endsWith("jpg")) {
                String imgPath = eachFile.getCanonicalPath();
                System.out.println("imgPath = " + imgPath);
                BufferedImage srcImage = DrawImg.getFloydSteinbergBinImg(ImageIO.read(new File(imgPath)));
                OLEDDisplayDriver.getInstance().display(srcImage);
                Thread.sleep(2000);
            }

        }
    }

    /**
     * 展示树莓派中的可用字体
     */
    public static void displaySupportFonts() {
        //获取系统中可用的字体的名字
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontName = e.getAvailableFontFamilyNames();
        for (int i = 0; i < fontName.length; i++) {
            System.out.println(fontName[i]);
        }
    }

}
