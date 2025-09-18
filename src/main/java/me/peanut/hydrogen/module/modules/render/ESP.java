package me.peanut.hydrogen.module.modules.render;

import com.darkmagician6.eventapi.EventTarget;
import me.peanut.hydrogen.events.EventRender3D;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.settings.Setting;
import me.peanut.hydrogen.utils.ColorUtil;
import me.peanut.hydrogen.utils.RenderUtils;
import me.peanut.hydrogen.utils.EntityUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced ESP Module with multiple rendering modes and comprehensive filtering
 * @author peanut (enhanced)
 * @version 2.0
 */
@Info(name = "ESP", description = "Enhanced entity highlighting with multiple render modes", category = Category.Render)
public class ESP extends Module {

    // Rendering Mode Settings
    private final Setting<String> mode = register(new Setting<>("Mode", "Box", Arrays.asList(
        "Box", "Outline", "Glow", "2D", "Filled", "Chams", "Shader", "Tracers"
    )));
    
    // Visual Settings
    private final Setting<Float> lineWidth = register(new Setting<>("LineWidth", 2.0f, 0.5f, 10.0f));
    private final Setting<Float> boxAlpha = register(new Setting<>("BoxAlpha", 0.3f, 0.0f, 1.0f));
    private final Setting<Float> outlineAlpha = register(new Setting<>("OutlineAlpha", 1.0f, 0.0f, 1.0f));
    private final Setting<Boolean> rainbow = register(new Setting<>("Rainbow", false));
    private final Setting<Float> rainbowSpeed = register(new Setting<>("RainbowSpeed", 1.0f, 0.1f, 5.0f));
    
    // Target Filtering
    private final Setting<Boolean> players = register(new Setting<>("Players", true));
    private final Setting<Boolean> mobs = register(new Setting<>("Mobs", true));
    private final Setting<Boolean> animals = register(new Setting<>("Animals", false));
    private final Setting<Boolean> items = register(new Setting<>("Items", false));
    private final Setting<Boolean> invisibles = register(new Setting<>("Invisibles", true));
    private final Setting<Boolean> teammates = register(new Setting<>("Teammates", false));
    private final Setting<Boolean> self = register(new Setting<>("Self", false));
    
    // Advanced Filtering
    private final Setting<Double> maxDistance = register(new Setting<>("MaxDistance", 100.0, 10.0, 500.0));
    private final Setting<Integer> maxEntities = register(new Setting<>("MaxEntities", 50, 1, 200));
    private final Setting<Boolean> throughWalls = register(new Setting<>("ThroughWalls", true));
    private final Setting<Boolean> healthBased = register(new Setting<>("HealthBased", false));
    
    // Color Settings
    private final Setting<Integer> playerRed = register(new Setting<>("PlayerRed", 255, 0, 255));
    private final Setting<Integer> playerGreen = register(new Setting<>("PlayerGreen", 100, 0, 255));
    private final Setting<Integer> playerBlue = register(new Setting<>("PlayerBlue", 100, 0, 255));
    
    private final Setting<Integer> mobRed = register(new Setting<>("MobRed", 255, 0, 255));
    private final Setting<Integer> mobGreen = register(new Setting<>("MobGreen", 50, 0, 255));
    private final Setting<Integer> mobBlue = register(new Setting<>("MobBlue", 50, 0, 255));
    
    private final Setting<Integer> animalRed = register(new Setting<>("AnimalRed", 50, 0, 255));
    private final Setting<Integer> animalGreen = register(new Setting<>("AnimalGreen", 255, 0, 255));
    private final Setting<Integer> animalBlue = register(new Setting<>("AnimalBlue", 50, 0, 255));
    
    private final Setting<Integer> itemRed = register(new Setting<>("ItemRed", 255, 0, 255));
    private final Setting<Integer> itemGreen = register(new Setting<>("ItemGreen", 255, 0, 255));
    private final Setting<Integer> itemBlue = register(new Setting<>("ItemBlue", 50, 0, 255));
    
    // Performance Settings
    private final Setting<Boolean> optimize = register(new Setting<>("Optimize", true));
    private final Setting<Integer> updateRate = register(new Setting<>("UpdateRate", 60, 1, 120));
    
    // State variables
    private final Map<Entity, EntityData> entityCache = new ConcurrentHashMap<>();
    private long lastUpdate = 0;
    private int rainbowOffset = 0;
    
    private static class EntityData {
        public final Entity entity;
        public final Color color;
        public final double distance;
        public final long cacheTime;
        
        public EntityData(Entity entity, Color color, double distance) {
            this.entity = entity;
            this.color = color;
            this.distance = distance;
            this.cacheTime = System.currentTimeMillis();
        }
        
        public boolean isExpired(long maxAge) {
            return System.currentTimeMillis() - cacheTime > maxAge;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        entityCache.clear();
        lastUpdate = 0;
        rainbowOffset = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        entityCache.clear();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        // Update entity cache periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate > (1000 / updateRate.getValue())) {
            updateEntityCache();
            lastUpdate = currentTime;
        }

        // Clean expired cache entries
        cleanCache();

        // Render entities
        renderEntities(event.getPartialTicks());
        
        // Update rainbow offset
        if (rainbow.getValue()) {
            rainbowOffset += rainbowSpeed.getValue().intValue();
            if (rainbowOffset > 360) rainbowOffset = 0;
        }
    }

    private void updateEntityCache() {
        entityCache.clear();
        
        List<Entity> validEntities = getValidEntities();
        
        // Sort by distance if optimizing
        if (optimize.getValue()) {
            validEntities.sort(Comparator.comparing(e -> 
                mc.thePlayer.getDistanceToEntity(e)));
        }
        
        // Limit entity count
        int entityCount = Math.min(validEntities.size(), maxEntities.getValue());
        
        for (int i = 0; i < entityCount; i++) {
            Entity entity = validEntities.get(i);
            Color color = getEntityColor(entity, i);
            double distance = mc.thePlayer.getDistanceToEntity(entity);
            
            entityCache.put(entity, new EntityData(entity, color, distance));
        }
    }

    private List<Entity> getValidEntities() {
        List<Entity> entities = new ArrayList<>();
        
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!isValidEntity(entity)) {
                continue;
            }
            
            double distance = mc.thePlayer.getDistanceToEntity(entity);
            if (distance > maxDistance.getValue()) {
                continue;
            }
            
            if (!throughWalls.getValue() && !canSeeEntity(entity)) {
                continue;
            }
            
            entities.add(entity);
        }
        
        return entities;
    }

    private boolean isValidEntity(Entity entity) {
        if (entity == null || entity.isDead) {
            return false;
        }
        
        if (entity == mc.thePlayer && !self.getValue()) {
            return false;
        }
        
        if (entity.isInvisible() && !invisibles.getValue()) {
            return false;
        }
        
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (!players.getValue()) return false;
            if (!teammates.getValue() && isTeammate(player)) return false;
            return true;
        }
        
        if (entity instanceof EntityMob) {
            return mobs.getValue();
        }
        
        if (entity instanceof EntityAnimal) {
            return animals.getValue();
        }
        
        if (entity instanceof EntityItem) {
            return items.getValue();
        }
        
        // Other living entities
        return entity instanceof EntityLivingBase && mobs.getValue();
    }

    private boolean canSeeEntity(Entity entity) {
        Vec3 playerEyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 entityPos = new Vec3(entity.posX, entity.posY + entity.height / 2, entity.posZ);
        
        return mc.theWorld.rayTraceBlocks(playerEyes, entityPos, false, true, false) == null;
    }

    private boolean isTeammate(EntityPlayer player) {
        if (mc.thePlayer.getTeam() != null && player.getTeam() != null) {
            return mc.thePlayer.getTeam().isSameTeam(player.getTeam());
        }
        
        // Fallback: check display name colors
        String playerColor = getPlayerNameColor(player.getDisplayName().getFormattedText());
        String myColor = getPlayerNameColor(mc.thePlayer.getDisplayName().getFormattedText());
        
        return playerColor != null && playerColor.equals(myColor);
    }

    private String getPlayerNameColor(String displayName) {
        if (displayName.length() >= 2 && displayName.charAt(0) == '§') {
            return displayName.substring(0, 2);
        }
        return null;
    }

    private Color getEntityColor(Entity entity, int index) {
        if (rainbow.getValue()) {
            return ColorUtil.rainbow((rainbowOffset + index * 20) % 360, 1.0f, 1.0f);
        }
        
        if (healthBased.getValue() && entity instanceof EntityLivingBase) {
            return getHealthColor((EntityLivingBase) entity);
        }
        
        if (entity instanceof EntityPlayer) {
            return new Color(playerRed.getValue(), playerGreen.getValue(), playerBlue.getValue());
        } else if (entity instanceof EntityMob) {
            return new Color(mobRed.getValue(), mobGreen.getValue(), mobBlue.getValue());
        } else if (entity instanceof EntityAnimal) {
            return new Color(animalRed.getValue(), animalGreen.getValue(), animalBlue.getValue());
        } else if (entity instanceof EntityItem) {
            return new Color(itemRed.getValue(), itemGreen.getValue(), itemBlue.getValue());
        }
        
        return Color.WHITE;
    }

    private Color getHealthColor(EntityLivingBase entity) {
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float ratio = Math.max(0, Math.min(1, health / maxHealth));
        
        // Red to green based on health
        int red = (int) (255 * (1 - ratio));
        int green = (int) (255 * ratio);
        
        return new Color(red, green, 0);
    }

    private void renderEntities(float partialTicks) {
        GlStateManager.pushMatrix();
        setupRenderState();
        
        String renderMode = mode.getValue();
        
        for (EntityData data : entityCache.values()) {
            Entity entity = data.entity;
            Color color = data.color;
            
            if (entity.isDead) continue;
            
            switch (renderMode) {
                case "Box":
                    renderBox(entity, color, partialTicks);
                    break;
                case "Outline":
                    renderOutline(entity, color, partialTicks);
                    break;
                case "Glow":
                    renderGlow(entity, color, partialTicks);
                    break;
                case "2D":
                    render2D(entity, color, partialTicks);
                    break;
                case "Filled":
                    renderFilled(entity, color, partialTicks);
                    break;
                case "Tracers":
                    renderTracers(entity, color, partialTicks);
                    break;
                case "Chams":
                    renderChams(entity, color, partialTicks);
                    break;
                case "Shader":
                    renderShader(entity, color, partialTicks);
                    break;
            }
        }
        
        restoreRenderState();
        GlStateManager.popMatrix();
    }

    private void setupRenderState() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(lineWidth.getValue());
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    }

    private void restoreRenderState() {
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderBox(Entity entity, Color color, float partialTicks) {
        AxisAlignedBB boundingBox = getRenderBoundingBox(entity, partialTicks);
        
        // Render filled box
        RenderUtils.drawFilledBoundingBox(boundingBox, 
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 
                      (int) (boxAlpha.getValue() * 255)));
        
        // Render outline
        RenderUtils.drawBoundingBox(boundingBox, 
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 
                      (int) (outlineAlpha.getValue() * 255)));
    }

    private void renderOutline(Entity entity, Color color, float partialTicks) {
        AxisAlignedBB boundingBox = getRenderBoundingBox(entity, partialTicks);
        RenderUtils.drawBoundingBox(boundingBox, 
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 
                      (int) (outlineAlpha.getValue() * 255)));
    }

    private void renderGlow(Entity entity, Color color, float partialTicks) {
        AxisAlignedBB boundingBox = getRenderBoundingBox(entity, partialTicks);
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        
        // Render multiple expanding boxes for glow effect
        for (int i = 0; i < 3; i++) {
            float expansion = 0.1f * (i + 1);
            float alpha = (outlineAlpha.getValue() * 255) / (i + 2);
            
            AxisAlignedBB glowBox = boundingBox.expand(expansion, expansion, expansion);
            RenderUtils.drawBoundingBox(glowBox, 
                new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) alpha));
        }
        
        GlStateManager.popMatrix();
    }

    private void render2D(Entity entity, Color color, float partialTicks) {
        // 2D box rendering - simplified for brevity
        // Would require screen coordinate conversion
        renderBox(entity, color, partialTicks);
    }

    private void renderFilled(Entity entity, Color color, float partialTicks) {
        AxisAlignedBB boundingBox = getRenderBoundingBox(entity, partialTicks);
        RenderUtils.drawFilledBoundingBox(boundingBox, 
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 
                      (int) (boxAlpha.getValue() * 255)));
    }

    private void renderTracers(Entity entity, Color color, float partialTicks) {
        Vec3 playerPos = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 entityPos = RenderUtils.getInterpolatedPos(entity, partialTicks);
        
        RenderUtils.drawLine(playerPos, entityPos, 
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 
                      (int) (outlineAlpha.getValue() * 255)));
    }

    private void renderChams(Entity entity, Color color, float partialTicks) {
        // Chams rendering would require more complex shader work
        // Simplified to outlined box for now
        renderBox(entity, color, partialTicks);
    }

    private void renderShader(Entity entity, Color color, float partialTicks) {
        // Shader-based rendering would require OpenGL shaders
        // Simplified to glow effect for now
        renderGlow(entity, color, partialTicks);
    }

    private AxisAlignedBB getRenderBoundingBox(Entity entity, float partialTicks) {
        double x = RenderUtils.interpolate(entity.lastTickPosX, entity.posX, partialTicks) - RenderManager.renderPosX;
        double y = RenderUtils.interpolate(entity.lastTickPosY, entity.posY, partialTicks) - RenderManager.renderPosY;
        double z = RenderUtils.interpolate(entity.lastTickPosZ, entity.posZ, partialTicks) - RenderManager.renderPosZ;
        
        return new AxisAlignedBB(
            x - entity.width / 2, y, z - entity.width / 2,
            x + entity.width / 2, y + entity.height, z + entity.width / 2
        );
    }

    private void cleanCache() {
        long maxAge = 1000; // 1 second
        entityCache.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(maxAge) || entry.getKey().isDead);
    }

    // Utility methods for external access
    public boolean isESPEnabled() {
        return isEnabled();
    }

    public int getCachedEntityCount() {
        return entityCache.size();
    }

    public String getCurrentMode() {
        return mode.getValue();
    }

    public void cycleMode() {
        String[] modes = {"Box", "Outline", "Glow", "2D", "Filled", "Chams", "Shader", "Tracers"};
        String current = mode.getValue();
        
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(current)) {
                String next = modes[(i + 1) % modes.length];
                mode.setValue(next);
                break;
            }
        }
    }
}
