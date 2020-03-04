package com.jeka8833.logitech;

import com.logitech.gaming.LogiLED;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.awt.ComponentFactory;

import java.awt.*;
import java.awt.peer.RobotPeer;

public class ScreenRGB {

    public static int fps = 30;
    public static int upBorderFullUpdate = 80;
    public static int skipPixels = 1;
    public static int timeFullUpdate = 5000;
    public static float brightness = 2f;
    public static boolean optimizeAlgorithm = true;
    public static boolean onlyDynamicScene = false;

    private static final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
    private static final Rectangle rec = new Rectangle(size);
    private static RobotPeer peer;
    private static boolean running = false;

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
            SumColor hash = new SumColor(0, 0, 0, 0);
            Rectangle hashVector = new Rectangle(0, 0, 10, 2);
            Rectangle dynamicVector = new Rectangle(size);
            boolean forceUpdate = false;
            boolean autoFullUpdate = false;
            long lastTime = System.currentTimeMillis();
            int count = 0;
            try {
                while (running) {
                    if (optimizeAlgorithm && (forceUpdate || System.currentTimeMillis() - lastTime > timeFullUpdate)) {
                        final int[] oneScreen = screenshot(rec);
                        SumColor color = null;
                        if (!onlyDynamicScene)
                            updateLight(oneScreen);
                        final int[] twoScreen = screenshot(rec);
                        if (!onlyDynamicScene)
                            color = updateLight(oneScreen);
                        final Rectangle temp = getDynamicObject(oneScreen, twoScreen);
                        if (temp != null) {
                            autoFullUpdate = false;
                            dynamicVector = temp;
                            if ((dynamicVector.height * dynamicVector.width) * 100 / (size.height * size.width) > upBorderFullUpdate)
                                autoFullUpdate = true;
                            if (!onlyDynamicScene) {
                                final int[] substringPixels = staticSubstring(twoScreen, dynamicVector);
                                if (substringPixels == null)
                                    staticColor = color;
                                else
                                    staticColor = sumColor(substringPixels);
                            }
                        } else autoFullUpdate = true;
                        if (forceUpdate) {
                            final int wid = size.width / 100 * (100 - upBorderFullUpdate) / 2;
                            hash = sumColor(screenshot(hashVector = new Rectangle(dynamicVector.x > size.width -
                                    (dynamicVector.x + dynamicVector.width) ? (dynamicVector.x - wid) / 2 :
                                    (dynamicVector.x + dynamicVector.width + size.width - wid) / 2, (size.height - 2)
                                    / 2, wid, 2)));
                            forceUpdate = false;
                        }
                        System.out.println("FPS: " + (count / ((System.currentTimeMillis() - lastTime) / 1000f)));
                        count = 0;
                        lastTime = System.currentTimeMillis();
                    }
                    if (optimizeAlgorithm && !hash.equals(sumColor(screenshot(hashVector)))) {
                        forceUpdate = true;
                        System.out.println("Force update");
                    }
                    if (!optimizeAlgorithm || autoFullUpdate || forceUpdate)
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

    @Nullable
    @Contract(pure = true)
    private static int[] staticSubstring(@NotNull final int[] pixels, @NotNull final Rectangle substring) {
        final int len = pixels.length - (substring.width * substring.height);
        if (len == 0)
            return null;
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
        int r = correct.r;
        int g = correct.g;
        int b = correct.b;
        for (int i = 0; i < len; i += skipPixels) {
            final int color = pixels[i];
            b += color & 0xFF;
            g += (color >> 8) & 0xFF;
            r += (color >> 16) & 0xFF;
        }
        final float state = (len / skipPixels + correct.n) / brightness;
        final float max = 100f / Math.max(255, Math.max(r /= state, Math.max(g /= state, b /= state)));
        LogiLED.LogiLedSetLighting((int) (r * max), (int) (g * max), (int) (b * max));
    }

    @NotNull
    private static SumColor updateLight(@NotNull final int[] pixels) {
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
        final float state = len / skipPixels / brightness;
        final float max = 100f / Math.max(255, Math.max(r /= state, Math.max(g /= state, b /= state)));
        final SumColor color = new SumColor((int) (r * max), (int) (g * max), (int) (b * max), len);
        LogiLED.LogiLedSetLighting(color.r, color.g, color.b);
        return color;
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
    }
}