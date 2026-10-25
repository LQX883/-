package edu.chenjin.library.util;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

/**
 * 图形验证码工具：生成随机字符并绘制为 PNG 图片。
 */
public final class CaptchaUtil {

    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private CaptchaUtil() {
    }

    /**
     * 生成验证码图片并写入输出流，返回验证码文本。
     */
    public static String generate(int width, int height, OutputStream out) throws IOException {
        String code = randomCode(4);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, width, height);

        for (int i = 0; i < 8; i++) {
            g.setColor(randomColor(150, 220));
            g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                    RANDOM.nextInt(width), RANDOM.nextInt(height));
        }
        for (int i = 0; i < 40; i++) {
            g.setColor(randomColor(120, 200));
            g.fillOval(RANDOM.nextInt(width), RANDOM.nextInt(height), 2, 2);
        }

        int charWidth = width / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            g.setFont(new Font("Arial", Font.BOLD, height - 14));
            g.setColor(randomColor(20, 120));
            int x = charWidth / 2 + i * charWidth;
            int y = height - 10 + RANDOM.nextInt(6) - 3;
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }
        g.dispose();
        ImageIO.write(image, "png", out);
        return code;
    }

    private static String randomCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return builder.toString();
    }

    private static Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(
                min + RANDOM.nextInt(range),
                min + RANDOM.nextInt(range),
                min + RANDOM.nextInt(range));
    }
}
