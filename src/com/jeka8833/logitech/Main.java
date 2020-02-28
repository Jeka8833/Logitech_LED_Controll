package com.jeka8833.logitech;

import com.logitech.gaming.LogiLED;

import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Scanner;


public class Main {

    private static Robot robot;
    private static final Rectangle size = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

    public static void main(String[] args) {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        LogiLED.LogiLedInit();
        try {
            getMusic();
            int count = 0;
            long time = 0;
            while (true) {
                count++;
                final SColor color = getQuadAVG(getScreen());
                LogiLED.LogiLedSetLighting(color.r, color.g, color.b);
                if (System.currentTimeMillis() - time > 10000) {
                    System.out.println("FPS: " + (count / ((System.currentTimeMillis() - time) / 1000f)));
                    time = System.currentTimeMillis();
                    count = 0;
                }
            }
        } finally {
            LogiLED.LogiLedShutdown();
        }
    }

    private static BufferedImage getScreen() {
        return robot.createScreenCapture(size);
    }

    private static SColor getQuadAVG(final BufferedImage image) {
        final int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int r = 0;
        int g = 0;
        int b = 0;
        int n = 0;
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++) {
                final int color = pixels[n++];
                b += (color) & 0xFF;
                g += (color >> 8) & 0xFF;
                r += (color >> 16) & 0xFF;
            }
        }
        return new SColor((int) (r / n * 100 / 255f), (int) (g / n * 100 / 255f), (int) (b / n * 100 / 255f));
    }

    private static void getMusic() {
        try {
            final Mixer.Info[] infos = AudioSystem.getMixerInfo();
            for(int i = 0; i < infos.length; i++){
                System.out.println(i + " " + infos[i]);
            }
            var sc =  new Scanner(System.in);
            int a = sc.nextInt();
            Line.Info[] lines = AudioSystem.getMixer(infos[a]).getTargetLineInfo();
            for(int i = 0; i < lines.length; i++){
                System.out.println(i + " " + lines[i]);
            }
            int b = sc.nextInt();
            AudioFormat format = new AudioFormat(8000.0f, 8, 1, true, true);
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(lines[b]);
            microphone.open(format);
            microphone.start();
            int bufferSize = (int)format.getSampleRate() *
                    format.getFrameSize();
            byte[] buffer = new byte[bufferSize];

            while (true) {
                microphone.read(buffer, 0, buffer.length);

                System.out.println(buffer.length + " " );
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private static class SColor {
        final int r;
        final int g;
        final int b;

        private SColor(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}
