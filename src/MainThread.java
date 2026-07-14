import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;

public class MainThread extends JFrame {
    // 屏幕分辨率
    public static int screen_w = 1024;
    public static int screen_h = 682;
    public static int half_screen_w = screen_w / 2;
    public static int half_screen_h = screen_h / 2;

    // 使用 JPanel 作为画板
    public static JPanel panel;

    // 使用 int[] 存储屏幕上像素的数值
    public static int[] screen;

    // 屏幕图像缓冲区, 提供了在内存中操作屏幕中图像的方法
    public static BufferedImage screenBuffer;

    // 记载目前已渲染的帧数
    public static int frameIndex;

    // 希望达到的每帧之间的间隔时间(ms)
    public static int frameInterval = 33;

    // CPU 睡眠时间, 数字越小说明运算效率越高
    public static int sleepTime;

    // 刷新率及计算刷新率所用到的一些辅助参数
    public static int framePerSecond;
    public static long lastDraw;
    public static double thisTime, lastTime;

    public MainThread() {
        // 弹出一个 JPanel 窗口并将其放置于屏幕中间
        setTitle("Swing 3D");
        panel = (JPanel) this.getContentPane();
        panel.setPreferredSize(new Dimension(screen_w, screen_h));
        panel.setMinimumSize(new Dimension(screen_w, screen_h));
        panel.setLayout(null);

        setResizable(false);
        pack();
        setVisible(true);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((dim.width - getSize().width) / 2, (dim.height - getSize().height) / 2);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 用 TYPE_INT_RGB 来创建 BufferedImage, 然后将屏幕的像素数组指向 BufferedImage 中的 DataBuffer
        // 从而通过改变屏幕的像素数组中的数据即可在屏幕中渲染出图像
        screenBuffer = new BufferedImage(screen_w, screen_h, BufferedImage.TYPE_INT_RGB);
        DataBuffer dest = screenBuffer.getRaster().getDataBuffer();
        screen = ((DataBufferInt) dest).getData();

        // 渲染主循环
        while (true) {
            int r_skyblue = 163, g_skyblue = 216, b_skyblue = 239;  // 天蓝色
            int r_orange = 255, g_orange = 128, b_orange = 0;       // 橙色

            // 将屏幕渲染为从天蓝色过渡到橙色的动态画面
            for (int i = 0; i < screen_w; ++i) {
                int p = (i + frameIndex * 8) % screen_w;
                for (int j = 0; j <screen_h; ++j) {
                    float t1 = Math.abs((float) (half_screen_w - p) / half_screen_w);
                    float t2 = 1.0f - t1;
                    int r = (int) (r_skyblue * t1 + r_orange * t2);
                    int g = (int) (g_skyblue * t1 + g_orange * t2);
                    int b = (int) (b_skyblue * t1 + b_orange * t2);
                    screen[i + j * screen_w] = (r << 16) | (g << 8) | b;
                }
            }

            // 更新帧数
            frameIndex++;

            // 计算当前帧率并尽量让刷新率保持稳定
            if (frameIndex % 30 == 0) {
                double thisTime = System.currentTimeMillis();
                framePerSecond = (int) (1000 / ((thisTime - lastTime) / 30.0f));
                lastTime = thisTime;
            }
            sleepTime = 0;
            while (System.currentTimeMillis() - lastDraw < frameInterval) {
                try {
                    Thread.sleep(1L);
                    sleepTime++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            lastDraw = System.currentTimeMillis();

            // 显示当前刷新率
            Graphics2D g2 = (Graphics2D) screenBuffer.getGraphics();
            g2.setColor(Color.BLACK);
            g2.drawString(String.format("FPS: %d    Thread sleep: %d ms", framePerSecond, sleepTime), 5, 15);

            // 将图像绘制到显存中, 这是唯一用到显卡的地方
            panel.getGraphics().drawImage(screenBuffer, 0, 0, this);
        }
    }

    // 主程序入口
    public static void main(String[] args) {
        new MainThread();
    }
}
