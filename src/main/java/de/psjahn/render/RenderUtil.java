package de.psjahn.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class RenderUtil {
    public static void renderQuad(MatrixStack matrices, Color color, double x1, double y1, double x2, double y2) {
        double j;
        if (x1 < x2) {
            j = x1;
            x1 = x2;
            x2 = j;
        }

        if (y1 < y2) {
            j = y1;
            y1 = y2;
            y2 = j;
        }
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float[] colorFloat = getColor(color);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, (float) x1, (float) y2, 0.0F)
                .color(colorFloat[0], colorFloat[1], colorFloat[2], colorFloat[3])
                .next();
        buffer.vertex(matrix, (float) x2, (float) y2, 0.0F)
                .color(colorFloat[0], colorFloat[1], colorFloat[2], colorFloat[3])
                .next();
        buffer.vertex(matrix, (float) x2, (float) y1, 0.0F)
                .color(colorFloat[0], colorFloat[1], colorFloat[2], colorFloat[3])
                .next();
        buffer.vertex(matrix, (float) x1, (float) y1, 0.0F)
                .color(colorFloat[0], colorFloat[1], colorFloat[2], colorFloat[3])
                .next();

        setupRender();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        endRender();
    }

    public static void renderEllipse(MatrixStack matrices, Color ellipseColor, double originX, double originY, double radX, double radY, int segments) {
        segments = MathHelper.clamp(segments, 4, 360);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float[] colorFloat = getColor(ellipseColor);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < 360; i += (int) Math.min(360d / segments, 360 - i)) {
            double radians = Math.toRadians(i);
            double sin = Math.sin(radians) * radX;
            double cos = Math.cos(radians) * radY;
            buffer.vertex(matrix, (float) (originX + sin), (float) (originY + cos), 0)
                    .color(colorFloat[0], colorFloat[1], colorFloat[2], colorFloat[3])
                    .next();
        }
        setupRender();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        endRender();
    }

    public static void renderCircle(MatrixStack matrices, Color circleColor, double originX, double originY, double rad, int segments) {
        renderEllipse(matrices, circleColor, originX, originY, rad, rad, segments);
    }

    public static void renderNumber(MatrixStack matrixStack, DrawContext context, TextRenderer textRenderer, int number, double offsetX, double offsetY, float scale)
    {
        matrixStack.push();
        matrixStack.translate(offsetX,offsetY,0);
        matrixStack.scale(scale,scale,scale);
        matrixStack.translate(number>9?-5.5:-2.5,-3.5,0);
        context.drawText(textRenderer, number+"", 0,0,0xFFFFFF,false);
        matrixStack.pop();
    }

    public static void renderNumber(MatrixStack matrixStack, DrawContext context, TextRenderer textRenderer, String number, double offsetX, double offsetY, float scale)
    {
        matrixStack.push();
        matrixStack.translate(offsetX,offsetY,0);
        matrixStack.scale(scale,scale,scale);
        matrixStack.translate(-textRenderer.getWidth(number)/2d+0.5d,-3.5,0);
        context.drawText(textRenderer, number, 0,0,0xFFFFFF,false);
        matrixStack.pop();
    }

    static float[] getColor(Color c) {
        return new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f};
    }

    private static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    private static void endRender() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
