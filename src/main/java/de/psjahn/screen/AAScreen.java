package de.psjahn.screen;

import com.mojang.logging.LogUtils;
import de.psjahn.render.MSAAFramebuffer;
import de.psjahn.render.RenderUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class AAScreen extends Screen {
    @AllArgsConstructor @Getter @Setter
    private static class Level
    {
        private double rotationSpeed;
        private double levelRotation;
        private int remainingCircles;
        private final List<Circle> attachedCircles = new ArrayList<>();
        private final List<TravellingCircle> travellingCircles = new ArrayList<>();
    }

    @AllArgsConstructor @Getter @Setter
    private static class Circle
    {
        private double rotationOffset;
        private final boolean userAttached;
        private final int index;
    }

    @AllArgsConstructor
    private static class TravellingCircle
    {
        private double position;
        private final int index;
    }

    private record Settings(float centerCircleSize, double circleSize, double lineLength, double lineWidth, double travellingCircleSpeed, float circleTextSize, float levelTextSize, float failedTextSize){}

    private final MinecraftClient minecraft;
    private final Logger LOGGER;
    private final Settings settings;

    private final List<Level> levels = new ArrayList<>();
    private int currentLevelIndex;

    private boolean failed = false;
    private double failedAnimationTicks = 0d;

    public AAScreen() {
        super(NarratorManager.EMPTY);

        minecraft=MinecraftClient.getInstance();
        LOGGER = LogUtils.getLogger();

        settings=new Settings(27, 5.5, 75, 0.3, 30,0.6f,2f, 1.8f);

        Level l1 = new Level(5, 0,6);
        levels.add(l1);

        Level l2 = new Level(5, 0,6);
        l2.getAttachedCircles().add(new Circle(0, false, 0));
        l2.getAttachedCircles().add(new Circle(72, false, 0));
        l2.getAttachedCircles().add(new Circle(144, false, 0));
        l2.getAttachedCircles().add(new Circle(216, false, 0));
        l2.getAttachedCircles().add(new Circle(288, false, 0));
        levels.add(l2);

        Level l3 = new Level(5, 0,6);
        double dividedby7 = 51.4285714286;
        l3.getAttachedCircles().add(new Circle(0, false, 0));
        l3.getAttachedCircles().add(new Circle(dividedby7, false, 0));
        l3.getAttachedCircles().add(new Circle(dividedby7*2, false, 0));
        l3.getAttachedCircles().add(new Circle(dividedby7*3, false, 0));
        l3.getAttachedCircles().add(new Circle(dividedby7*4, false, 0));
        l3.getAttachedCircles().add(new Circle(dividedby7*5, false, 0));
        l3.getAttachedCircles().add(new Circle(dividedby7*6, false, 0));
        levels.add(l3);

        Level l4 = new Level(8,0,10);
        l4.getAttachedCircles().add(new Circle(0, false, 0));
        l4.getAttachedCircles().add(new Circle(60, false, 0));
        l4.getAttachedCircles().add(new Circle(90, false, 0));
        l4.getAttachedCircles().add(new Circle(120, false, 0));
        l4.getAttachedCircles().add(new Circle(180, false, 0));
        l4.getAttachedCircles().add(new Circle(240, false, 0));
        l4.getAttachedCircles().add(new Circle(300, false, 0));
        levels.add(l4);

        currentLevelIndex=0;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(failed)
        {
            failedAnimationTicks+=5*delta;
            if(failedAnimationTicks>90) {
            close();
            }
        }

        if(levels.size()<=currentLevelIndex)
        {
            LOGGER.error("(AA) Trying to load non-existent level {" + currentLevelIndex+1 + "}, levels {" + levels.size()+"}");
            return;
        }
        Level level = levels.get(currentLevelIndex);

        //Move Travelling Circles, attach if touching
        for(TravellingCircle travellingCircle : level.getTravellingCircles())
        {
            if(failed) continue;
            travellingCircle.position-=settings.travellingCircleSpeed*delta;
            travellingCircle.position=Math.max(travellingCircle.position, settings.lineLength);
            if(travellingCircle.position<=settings.lineLength+settings.circleSize*2)
            {
                for(Circle attachedCircle : level.getAttachedCircles())
                {
                    final double rotationRad = Math.toRadians(level.getLevelRotation()+attachedCircle.getRotationOffset());
                    if(isCollision(new Point2D.Double(settings.lineLength*Math.cos(rotationRad),settings.lineLength*Math.sin(rotationRad)), new Point2D.Double(0,travellingCircle.position), settings.circleSize*2))
                    {
                        failed=true;
                        break;
                    }
                }
            }
            if(travellingCircle.position==settings.lineLength&&!failed)
            {
                level.getAttachedCircles().add(new Circle(-level.getLevelRotation() + 90, true, travellingCircle.index));
                if(level.getTravellingCircles().size()==1&&level.getRemainingCircles()==0)
                {
                    currentLevelIndex++;
                }
            }
        }
        //Remove Travelling Circle if touching
        level.getTravellingCircles().removeIf(c->c.position==settings.lineLength);

        //Draw everything using MSAA
        MatrixStack matrixStack = context.getMatrices();
        MSAAFramebuffer.use(8, ()->{
            //Center MatrixStack
            matrixStack.push();
            matrixStack.translate((float) minecraft.getWindow().getScaledWidth()/2,(float) minecraft.getWindow().getScaledHeight()/2,0);

            //Render attached Circles + Lines
            for(Circle circle : level.getAttachedCircles())
            {
                matrixStack.push();
                matrixStack.multiply(new Quaternionf().rotateZ((float) Math.toRadians(level.getLevelRotation()+circle.getRotationOffset())));
                RenderUtil.renderQuad(matrixStack, Color.BLACK, 0, -settings.lineWidth, settings.lineLength,settings.lineWidth);
                RenderUtil.renderCircle(matrixStack, Color.BLACK, settings.lineLength, 0, settings.circleSize,16);
                matrixStack.pop();


                //Render Circle Index if attached by player
                if(circle.isUserAttached())
                {
                    final double radius = settings.lineLength;
                    final double rotationRad = Math.toRadians(level.getLevelRotation()+ circle.getRotationOffset());
                    final double posX = radius*Math.cos(rotationRad);
                    final double posY = radius*Math.sin(rotationRad);

                    RenderUtil.renderNumber(matrixStack, context, minecraft.textRenderer, circle.getIndex(),posX,posY,settings.circleTextSize);
                }
            }

            //Render travelling circles
            for(TravellingCircle circle : level.getTravellingCircles())
            {
                RenderUtil.renderCircle(matrixStack, Color.BLACK, 0, circle.position, settings.circleSize, 16);
                RenderUtil.renderNumber(matrixStack, context, minecraft.textRenderer, level.getRemainingCircles(), 0, circle.position, settings.circleTextSize);
            }

            //Bottom Circle + Text
            RenderUtil.renderCircle(matrixStack, Color.BLACK, 0, settings.lineLength+settings.circleSize*20, settings.circleSize, 16);
            RenderUtil.renderNumber(matrixStack,context, minecraft.textRenderer, level.getRemainingCircles(), 0,settings.lineLength+settings.circleSize*20, settings.circleTextSize);

            //Inner Circle + Level Number
            RenderUtil.renderCircle(matrixStack, Color.BLACK, 0, 0, settings.centerCircleSize, 64);
            if(!failed) RenderUtil.renderNumber(matrixStack,context,minecraft.textRenderer,currentLevelIndex+1,0,0,settings.levelTextSize);
            if(failed) RenderUtil.renderNumber(matrixStack,context,minecraft.textRenderer,"Failed",0,0,settings.failedTextSize);

        });

        if(failed) return;
        //Update Level Rotation
        level.setLevelRotation(level.getLevelRotation()-level.getRotationSpeed()*delta);
        if(level.getLevelRotation()>=360) level.setLevelRotation(level.getLevelRotation()-360);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Level level = levels.get(currentLevelIndex);
        if(level.getRemainingCircles()<=0||failed) return super.mouseClicked(mouseX, mouseY, button);
        level.getTravellingCircles().add(new TravellingCircle(settings.lineLength+settings.circleSize*20,level.getRemainingCircles()));
        level.setRemainingCircles(level.getRemainingCircles()-1);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static boolean isCollision(Point2D.Double p1, Point2D.Double p2, double combinedRadius)
    {
        final double dx = p1.x - p2.x;
        final double dy = p1.y - p2.y;
        return combinedRadius * combinedRadius > (dx * dx + dy * dy);
    }
}
