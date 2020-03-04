package com.jeka8833.logitech;

import com.logitech.gaming.LogiLED;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.awt.ComponentFactory;

import java.awt.*;
import java.awt.peer.RobotPeer;
import java.util.Objects;

public class ScreenRGB {

    public static int fps = 30;
    public static int upBorderFullUpdate = 80;
    public static int skipPixels = 1;
    public static int timeFullUpdate = 5000;
    public static float brightness = 2f;
    public static boolean optimizeAlgorithm = true;
    public static boolean onlyDynamicScene = true;


    private static final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
    private static final Rectangle rec = new Rectangle(size);
    private static RobotPeer peer;

    private static boolean running = false;

    private static boolean autoFullUpdate = false;

    static {
        try {
            peer = ((ComponentFactory) Toolkit.getDefaultToolkit()).createRobot(new Robot(),
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void start() {
        LogiLED.LogiLedInit();
        running = true;
        new Thread(() -> {
            SumColor staticColor = new SumColor(0, 0, 0, 1);
            Rectangle dynamicVector = new Rectangle(size);

            long lastTime = System.currentTimeMillis();
            int count = 0;
            try {
                while (running) {
                    if (System.currentTimeMillis() - lastTime > timeFullUpdate) {
                        final int[] oneScreen = screenshot(rec);
                        if (!onlyDynamicScene)
                            updateLight(oneScreen);
                        final int[] twoScreen = screenshot(rec);
                        if (!onlyDynamicScene)
                            updateLight(oneScreen);
                        final Rectangle temp = getDynamicObject(oneScreen, twoScreen);
                        if (temp != null) {
                            autoFullUpdate = false;
                            dynamicVector = temp;
                            if ((dynamicVector.height * dynamicVector.width) * 100 / (size.height * size.width) > upBorderFullUpdate) {
                                System.out.println("Dynamic image very big");
                                autoFullUpdate = true;
                            }
                            if (!onlyDynamicScene)
                                staticColor = sumColor(staticSubstring(twoScreen, dynamicVector));
                        } else autoFullUpdate = true;
                        System.out.println("FPS: " + (count / ((System.currentTimeMillis() - lastTime) / 1000f)));
                        count = 0;
                        lastTime = System.currentTimeMillis();
                    }
                    if (!optimizeAlgorithm || autoFullUpdate)
                        updateLight(screenshot(rec));
                    else if (onlyDynamicScene) updateLight(screenshot(dynamicVector));
                    else updateLight(screenshot(dynamicVector), staticColor);
                    count++;
                    if (fps > 0) {
                        final int delay = (int) ((1000 * count / fps) - (System.currentTimeMillis() - lastTime));
                        if (delay - 10 > 0)
                            Thread.sleep(delay - 10);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                LogiLED.LogiLedShutdown();
            }
        }).start();
    }

    public static void stop() {
        running = false;
    }

    private static int[] screenshot(final Rectangle size) {
        return peer.getRGBPixels(size);
    }

    @Nullable
    @Contract(pure = true)
    private static Rectangle getDynamicObject(final int[] imageOne, final int[] imageTwo) {
        int minX = Integer.MAX_VALUE;
        int maxX = 0;
        int minY = Integer.MAX_VALUE;
        int maxY = 0;
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++) {
                final int index = x + y * size.width;
                if (imageOne[index] != imageTwo[index]) {
                    if (minY > y)
                        minY = y;
                    if (maxY < y)
                        maxY = y;
                    if (minX > x)
                        minX = x;
                    if (maxX < x)
                        maxX = x;
                }
            }
        }
        if (minX == Integer.MAX_VALUE || minY == Integer.MAX_VALUE)
            return null;
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    @NotNull
    @Contract(pure = true)
    private static int[] staticSubstring(@NotNull final int[] pixels, @NotNull final Rectangle substring) {
        final int len = pixels.length - (substring.width * substring.height);
        if (len == 0)
            return pixels;
        final int[] pix = new int[len];
        int n = 0;
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++) {
                if (substring.x > x || substring.x + substring.width < x || substring.y > y || substring.y + substring.height < y) {
                    pix[n++] = pixels[x + y * size.width];
                }
            }
        }
        return pix;
    }


    private static void updateLight(@NotNull final int[] pixels, @NotNull final SumColor correct) {
        final int len = pixels.length;
        int r = 0;
        int g = 0;
        int b = 0;
        for (int i = 0; i < len; i += skipPixels) {
            final int color = pixels[i];
            b += color & 0xFF;
            g += (color >> 8) & 0xFF;
            r += (color >> 16) & 0xFF;
        }
        r = (int) (((r + correct.r) / ((len / skipPixels) + correct.n)) * brightness);
        g = (int) (((g + correct.g) / ((len / skipPixels) + correct.n)) * brightness);
        b = (int) (((b + correct.b) / ((len / skipPixels) + correct.n)) * brightness);

        final float max = 100f / Math.max(255, Math.max(r, Math.max(g, b)));
        LogiLED.LogiLedSetLighting((int) (r * max), (int) (g * max), (int) (b * max));
    }

    private static void updateLight(@NotNull final int[] pixels) {
        final int len = pixels.length;
        int r = 0;
        int g = 0;
        int b = 0;
        for (int i = 0; i < len; i += skipPixels) {
            final int color = pixels[i];
            b += color & 0xFF;
            g += (color >> 8) & 0xFF;
            r += (color >> 16) & 0xFF;
        }
        r = (int) ((r / (len / skipPixels)) * brightness);
        g = (int) ((g / (len / skipPixels)) * brightness);
        b = (int) ((b / (len / skipPixels)) * brightness);

        final float max = 100f / Math.max(255, Math.max(r, Math.max(g, b)));
        LogiLED.LogiLedSetLighting((int) (r * max), (int) (g * max), (int) (b * max));
    }

    @NotNull
    @Contract("_ -> new")
    private static SumColor sumColor(@NotNull final int[] pixels) {
        final int len = pixels.length;
        int r = 0;
        int g = 0;
        int b = 0;
        for (int i = 0; i < len; i += skipPixels) {
            final int color = pixels[i];
            b += color & 0xFF;
            g += (color >> 8) & 0xFF;
            r += (color >> 16) & 0xFF;
        }
        return new SumColor(r, g, b, len / skipPixels);
    }

    private static class SumColor {
        private final int r;
        private final int g;
        private final int b;
        private final int n;

        private SumColor(int r, int g, int b, int n) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.n = n;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SumColor sumColor = (SumColor) o;
            return r == sumColor.r &&
                    g == sumColor.g &&
                    b == sumColor.b &&
                    n == sumColor.n;
        }

        @Override
        public int hashCode() {
            return Objects.hash(r, g, b, n);
        }
    }
}