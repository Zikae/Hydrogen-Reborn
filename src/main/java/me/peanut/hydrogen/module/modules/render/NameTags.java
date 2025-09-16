package me.peanut.hydrogen.module.modules.render;

import me.peanut.hydrogen.utils.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.settings.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced NameTags with better visual appearance
 */
@Info(name = "NameTags", description = "Enhances nametags with improved visuals", category = Category.Render)
public class NameTags extends Module {

    public static NameTags instance;
    private static final int BACKGROUND_COLOR = 0x80000000; // Semi-transparent black
    private static final int HEALTH_COLOR = 0xFFFF0000;     // Red for health
    private static final int NAME_COLOR = 0xFFFFFFFF;       // White for names
    private static final int ENCHANT_COLOR = 0xFFFF00FF;    // Purple for enchantments

    public NameTags() {
        instance = this;
        addSetting(new Setting("Health", this, true));
        addSetting(new Setting("State", this, false));
        addSetting(new Setting("Items", this, true));
        addSetting(new Setting("MurderMystery", this, true));
        addSetting(new Setting("Background", this, true));
        addSetting(new Setting("Shadow", this, true));
        addSetting(new Setting("Scale", this, 1.0, 0.5, 2.0, false));
    }

    public static String clearFormat(String s) {
        return s.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    public static List<String> getEnchantList(ItemStack stack) {
        List<String> eList = new ArrayList<>();
        if (stack != null && stack.getEnchantmentTagList() != null) {
            for (int j = 0; j < stack.getEnchantmentTagList().tagCount(); j++) {
                int enchantLevel = stack.getEnchantmentTagList().getCompoundTagAt(j).getInteger("lvl");
                int enchantID = stack.getEnchantmentTagList().getCompoundTagAt(j).getInteger("id");
                Enchantment enchant = Enchantment.getEnchantmentById(enchantID);

                if (enchant == null) continue;

                String name = StatCollector.translateToLocal(enchant.getName());
                name = clearFormat(name);
                
                // Create a more readable enchantment display
                String[] parts = name.split(" ");
                StringBuilder disp = new StringBuilder();
                
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        disp.append(part.substring(0, 1).toUpperCase());
                    }
                }
                
                eList.add(disp.toString() + " " + enchantLevel);
            }
        }
        return eList;
    }

    public static double[] entityRenderPos(Entity e) {
        float partialTicks = mc.timer.renderPartialTicks;
        double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
        double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
        
        return new double[]{x, y, z};
    }

    public void renderArmorESP(EntityLivingBase entity) {
        GlStateManager.depthMask(true);
        int xOff = -40;
        
        // Render held item
        renderItem(entity.getHeldItem(), xOff, -18);
        
        // Render armor
        for (int i = 0; i <= 3; i++) {
            ItemStack stack = entity.getCurrentArmor(3 - i);
            if (stack == null) continue;
            renderItem(stack, xOff + ((i + 1) * 16), -18);
        }
        
        GlStateManager.depthMask(false);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        if (!h2.settingsManager.getSettingByName("Items").isEnabled() || stack == null) return;

        // Render enchantments
        List<String> eList = getEnchantList(stack);
        if (!eList.isEmpty()) {
            GlStateManager.pushMatrix();
            float scale = 0.5f;
            GlStateManager.scale(scale, scale, scale);
            
            for (String s : eList) {
                int textWidth = mc.fontRendererObj.getStringWidth(s);
                int textX = (int) ((x + 8) * 2 - textWidth / 2);
                int textY = (int) ((y - 7) * 2 - (mc.fontRendererObj.FONT_HEIGHT * eList.indexOf(s)));
                
                if (h2.settingsManager.getSettingByName("Shadow").isEnabled()) {
                    mc.fontRendererObj.drawStringWithShadow(s, textX, textY, ENCHANT_COLOR);
                } else {
                    mc.fontRendererObj.drawString(s, textX, textY, ENCHANT_COLOR);
                }
            }
            GlStateManager.popMatrix();
        }

        // Render item
        mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, x, y - 2, "");
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, -150);
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y - 4);
        GlStateManager.popMatrix();
    }

    public void render3DPost() {
        if (!isEnabled() || mc.theWorld == null || mc.theWorld.loadedEntityList == null) return;

        RenderHelper.enableStandardItemLighting();
        for (Entity e : mc.theWorld.loadedEntityList) {
            if (e instanceof EntityLivingBase && mc.getRenderManager().getEntityRenderObject(e) instanceof RendererLivingEntity) {
                double[] p = entityRenderPos(e);
                
                // Apply scaling if enabled
                if (h2.settingsManager.getSettingByName("Scale").getValue() != 1.0) {
                    GlStateManager.pushMatrix();
                    float scale = (float) h2.settingsManager.getSettingByName("Scale").getValue();
                    GlStateManager.translate((float) p[0], (float) p[1], (float) p[2]);
                    GlStateManager.scale(scale, scale, scale);
                    GlStateManager.translate((float) -p[0], (float) -p[1], (float) -p[2]);
                }
                
                RenderUtil.passSpecialRenderNameTags((EntityLivingBase) e, p[0], p[1], p[2]);
                
                if (h2.settingsManager.getSettingByName("Scale").getValue() != 1.0) {
                    GlStateManager.popMatrix();
                }
            }
        }
        RenderHelper.disableStandardItemLighting();
    }
}
