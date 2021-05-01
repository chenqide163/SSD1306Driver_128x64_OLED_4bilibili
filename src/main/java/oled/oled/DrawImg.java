package oled.oled;

import com.pi4j.io.i2c.I2CFactory;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Date;
import java.util.Random;

public class DrawImg {
    private static Logger LOGGER = Logger.getLogger(DrawImg.class);

    public static void main(String[] args) throws IOException {

        //读取原始位图
        Image srcImage = ImageIO.read(new File("D:\\test.jpg"));
        BufferedImage destImg = getFloydSteinbergBinImg(srcImage);
        ImageIO.write(destImg, "jpg", new File("D:\\test2.jpg"));

        destImg = getCover();
        ImageIO.write(destImg, "jpg", new File("D:\\test2.jpg"));

    }

    public static BufferedImage getCover() {
        BufferedImage image = new BufferedImage(128, 64,
                BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = image.createGraphics();
        //从树莓派的输出上，看到微软雅黑字体名为：Microsoft YaHei，指定字体为15像素
        g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
        g.setColor(new Color(0xffffffff));
        Date date = new Date();
        g.drawString("Java", 0, 15);
        g.drawString("玩转", 0, 31);

        g.drawString("ssd1306 128x64 ", 0, 47);
        g.drawString("            陈琦玩派派", 0, 63);

        g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 28));
        g.drawString("树莓派", 37, 28);

        return image;
    }

    /**
     * 输出爬虫数据
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws I2CFactory.UnsupportedBusNumberException
     */
    public static void printCrawlerData() throws IOException, InterruptedException, I2CFactory.UnsupportedBusNumberException {
        int movieNum = 342;
        int postNum = 845;
        int diskUsage = 20;
        String ip = getRaspiIP();
        for (; ; ) {
            BufferedImage image = new BufferedImage(128, 64,
                    BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D g = image.createGraphics();
            //从树莓派的输出上，看到微软雅黑字体名为：Microsoft YaHei，指定字体为15像素
            g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
            g.setColor(new Color(0xffffffff));
            Date date = new Date();
            String dayTiem = String.format("%tm-%td  %tH:%tM:%tS", date, date, date, date, date);
            g.drawString(dayTiem, 0, 15);
            g.drawString("IP: " + ip, 0, 31);

            long time = ((new Date()).getTime()) / 1000;
            if (time % 6 < 4) {
                g.drawString("已爬内容: " + (movieNum += (new Random()).nextInt(3)), 0, 47);
                g.drawString("已爬海报: " + (postNum += (new Random()).nextInt(4)), 0, 63);
            } else {
                g.drawString("CPU: " + (60 + (new Random()).nextInt(30)) + "%", 0, 47);
                g.drawString("磁盘占用: " + (diskUsage += (new Random()).nextFloat()) + "%", 0, 63);
            }


            OLEDDisplayDriver.getInstance().display(image);
            Thread.sleep(800);
        }
    }

    /**
     * 获取灰阶抖动的图片，使用Floyd–Steinberg dithering算法
     * 参考https://en.wikipedia.org/wiki/Floyd%E2%80%93Steinberg_dithering
     *
     * @param srcImage 返回的TYPE_BYTE_BINARY 二值图片
     * @return
     * @throws IOException
     */
    public static BufferedImage getFloydSteinbergBinImg(Image srcImage) {
        LOGGER.debug("start to run getFloydSteinbergBinImg");
        int width = OLEDDisplayDriver.DISPLAY_WIDTH;
        int height = OLEDDisplayDriver.DISPLAY_HEIGHT;

        //定义一个BufferedImage对象，用于保存缩小后的灰阶图片
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics graphics = bufferedImage.getGraphics();

        //将原始位图按墨水屏幕大小缩小后绘制到bufferedImage对象中
        graphics.drawImage(srcImage, 0, 0, width, height, null);

        BufferedImage destImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        int color[][] = new int[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 取出灰度图片各像素灰阶值，256级灰阶，即 & 0xff 作用
                color[x][y] = bufferedImage.getRGB(x, y) & 0xff;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int oldPixel = color[x][y];
                int newPixel = oldPixel > 125 ? 255 : 0; //应为需要转化为2级灰阶，即非255即0，在灰阶中，0黑1白

                if (0 == newPixel) {
                    destImg.setRGB(x, y, 0xffffff);
                } else {
                    destImg.setRGB(x, y, 0);
                }

                int quantError = oldPixel - newPixel;

                //右边像素
                if (x + 1 < width) {
                    int pointPixel = color[x + 1][y] + quantError * 7 / 16;
                    color[x + 1][y] = pointPixel;
                }

                //左下像素
                if (x > 0 && y + 1 < height) {
                    int pointPixel = color[x - 1][y + 1] + quantError * 3 / 16;
                    color[x - 1][y + 1] = pointPixel;
                }

                //下像素
                if (y + 1 < height) {
                    int pointPixel = color[x][y + 1] + quantError * 5 / 16;
                    color[x][y + 1] = pointPixel;
                }

                //下右像素
                if (y + 1 < height && x + 1 < width) {
                    int pointPixel = color[x][y + 1] + quantError * 1 / 16;
                    color[x][y + 1] = pointPixel;
                }
            }
        }
        return destImg;
    }

    /**
     * 获取树莓派IP
     *
     * @return
     */
    private static String getRaspiIP() {
        InputStream in = null;
        BufferedReader read = null;
        try {
            String command = "hostname -I | cut -d' ' -f1";
            Process pro = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            pro.waitFor();
            in = pro.getInputStream();
            read = new BufferedReader(new InputStreamReader(in));
            String result = "";
            String line;

            while ((line = read.readLine()) != null) {
                result = result + line + "\n";
            }
            LOGGER.debug("getRaspiIP is : " + result);
            return result;
        } catch (Exception e) {
            LOGGER.error(e);
            return "do not get the IP!";
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (read != null) {
                try {
                    read.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
