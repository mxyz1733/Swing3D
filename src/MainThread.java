import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;

public class MainThread extends JFrame implements KeyListener {
    // 屏幕分辨率
    public static int screen_w = 1024;
    public static int screen_h = 682;
    public static int half_screen_w = screen_w / 2;
    public static int half_screen_h = screen_h / 2;
    public static int screenSize = screen_w * screen_h;

    // 使用 JPanel 作为画板
    public static JPanel panel;

    // 使用 int[] 存储屏幕上像素的数值
    public static int[] screen;

    // 使用 float[] 来存储屏幕的深度缓冲值
    public static float[] zBuffer;

    // 屏幕图像缓冲区, 提供了在内存中操作屏幕中图像的方法
    public static BufferedImage screenBuffer;

    // 记载目前已渲染的帧数
    public static int frameIndex;

    // 希望达到的每帧之间的间隔时间(ms)
    public static int frameInterval = 33;

    // CPU 睡眠时间, 数字越小说明运算效率越高
    public static int sleepTime, averageSleepTime;

    // 刷新率及计算刷新率所用到的一些辅助参数
    public static int framePerSecond;
    public static long lastDraw;
    public static double thisTime, lastTime;

    //总共渲染的三角形数
    public static int triangleCount;

    public MainThread() {
        // 初始化
        init();
        // 添加按键监听器
        addKeyListener(this);
    }

    // 初始化
    public void init() {
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

        // 初始化深度缓冲
        zBuffer = new float[screenSize];
    }

    // 渲染主循环
    public void mainLoop() {
        //做一个甜甜圈的场景
        float R = 1.0f; // radius of the torus
        float r = 0.3f; // radius of the tube

        int numSides = 256; // number of sides around the tube
        int numRings = 256; // number of rings around the torus

        Vector3D[] vertices = new Vector3D[numSides * numRings];
        int[] indices = new int[numSides * numRings * 6];

        int index = 0;

        for (int i = 0; i < numRings; i++) {
            float u = (float)i / numRings * 2.0f * (float)Math.PI;
            for (int j = 0; j < numSides; j++) {
                float v = (float)j / numSides * 2.0f * (float)Math.PI;
                float x = (R + r * (float)Math.cos(v)) * (float)Math.cos(u);
                float y = (R + r * (float)Math.cos(v)) * (float)Math.sin(u);
                float z = r * (float)Math.sin(v);
                vertices[index++] = new Vector3D(x, y, z);
            }
        }

        index = 0;

        for (int i = 0; i < numRings; i++) {
            for (int j = 0; j < numSides; j++) {
                int nexti = (i + 1) % numRings;
                int nextj = (j + 1) % numSides;
                int a = i * numSides + j;
                int b = i * numSides + nextj;
                int c = nexti * numSides + nextj;
                int d = nexti * numSides + j;
                indices[index++] = a;
                indices[index++] = b;
                indices[index++] = c;
                indices[index++] = c;
                indices[index++] = d;
                indices[index++] = a;
            }
        }

        VertexBufferObject torus = new VertexBufferObject(vertices, indices);

        while (true) {
            // 三角形数归零
            triangleCount = 0;

            // 把深度缓冲归零
            zBuffer[0] = 0;
            for (int i = 1; i < screenSize; i+=i)
                System.arraycopy(zBuffer, 0, zBuffer, i, Math.min(screenSize - i, i));

            // 更新视角
            Camera.update();

            // 渲染背景
            screen[0] = (163 << 16) | (216 << 8) | 239; // 天蓝色
            for(int i = 1; i < screenSize; i += i)
                System.arraycopy(screen, 0, screen, i, Math.min(screenSize - i, i));

            Rasterizer.VBO = torus;

            // 画甜甜圈1
            Rasterizer.triangleColor = 0xCD5C5C;
            Rasterizer.localTranslation.set(0, 0, 4f);
            Rasterizer.renderType = 0;
            Rasterizer.localRotationY = (frameIndex*2)%360;
            Rasterizer.localRotationX = (frameIndex*2)%360;
            Rasterizer.localRotationZ = (frameIndex*2)%360;
            Rasterizer.rasterize();


            // 画甜甜圈2
            Rasterizer.triangleColor = 0x008B8B;
            Rasterizer.localRotationY = 180;
            Rasterizer.localRotationX = 0;
            Rasterizer.localRotationZ = 0;
            Rasterizer.localTranslation.set(0.7f, 0.3f, 4.2f);
            Rasterizer.rasterize();

            // 更新帧数
            frameIndex++;

            // 计算当前帧率并尽量让刷新率保持稳定
            int totalSpeedTime = 0;
            while (System.currentTimeMillis() - lastDraw < frameInterval) {
                try {
                    Thread.sleep(1);
                    totalSpeedTime++;
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            sleepTime += totalSpeedTime;
            lastDraw = System.currentTimeMillis();
            // 计算当前的刷新率，并尽量让刷新率保持恒定。
            if (frameIndex % 30 == 0) {
                double thisTime = System.currentTimeMillis();
                framePerSecond = (int) (1000 / ((thisTime - lastTime) / 30));
                lastTime = thisTime;
                averageSleepTime = sleepTime / 30;
                sleepTime = 0;
            }

            // 显示当前刷新率
            Graphics2D g2 = (Graphics2D) screenBuffer.getGraphics();
            g2.setColor(Color.BLACK);
            g2.drawString(String.format("FPS: %d    Thread sleep: %d ms", framePerSecond, sleepTime), 5, 15);

            // 将图像绘制到显存中, 这是唯一用到显卡的地方
            panel.getGraphics().drawImage(screenBuffer, 0, 0, this);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyChar() == 'w' || e.getKeyChar() == 'W')
            Camera.MOVE_FORWARD = true;
        else if(e.getKeyChar() == 's' || e.getKeyChar() == 'S')
            Camera.MOVE_BACKWARD = true;
        else if(e.getKeyChar() == 'a' || e.getKeyChar() == 'A')
            Camera.SLIDE_LEFT = true;
        else if(e.getKeyChar() == 'd' || e.getKeyChar() == 'D')
            Camera.SLIDE_RIGHT = true;


        if(e.getKeyCode() == KeyEvent.VK_UP)
            Camera.LOOK_UP= true;
        else if(e.getKeyCode() == KeyEvent.VK_DOWN)
            Camera.LOOK_DOWN = true;
        else if(e.getKeyCode() == KeyEvent.VK_LEFT)
            Camera.LOOK_LEFT = true;
        else if(e.getKeyCode() == KeyEvent.VK_RIGHT)
            Camera.LOOK_RIGHT = true;

    }

    @Override
    public void keyReleased(KeyEvent e) {
        if(e.getKeyChar() == 'w' || e.getKeyChar() == 'W')
            Camera.MOVE_FORWARD = false;
        else if(e.getKeyChar() == 's' || e.getKeyChar() == 'S')
            Camera.MOVE_BACKWARD = false;
        else if(e.getKeyChar() == 'a' || e.getKeyChar() == 'A')
            Camera.SLIDE_LEFT = false;
        else if(e.getKeyChar() == 'd' || e.getKeyChar() == 'D')
            Camera.SLIDE_RIGHT = false;

        if(e.getKeyCode() == KeyEvent.VK_UP)
            Camera.LOOK_UP= false;
        else if(e.getKeyCode() == KeyEvent.VK_DOWN)
            Camera.LOOK_DOWN = false;
        else if(e.getKeyCode() == KeyEvent.VK_LEFT)
            Camera.LOOK_LEFT = false;
        else if(e.getKeyCode() == KeyEvent.VK_RIGHT)
            Camera.LOOK_RIGHT = false;

    }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    // 初始化各模块
    public static void initModules() {
        // 初始化查找表
        LookupTables.init();
        // 初始化软光栅渲染器
        Rasterizer.init();
        // 初始化相机
        Camera.init(0, 0, 0);
    }

    // 主程序入口
    public static void main(String[] args) {
        MainThread thread = new MainThread();
        initModules();
        thread.mainLoop();
    }
}
