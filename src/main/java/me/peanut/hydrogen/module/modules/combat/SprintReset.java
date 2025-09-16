package me.peanut.hydrogen.module.modules.combat;

import com.darkmagician6.eventapi.EventTarget;
import me.peanut.hydrogen.Hydrogen;
import me.peanut.hydrogen.events.EventTick;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.settings.Setting;
import me.peanut.hydrogen.utils.TimeUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;

import java.util.ArrayList;
import java.util.List;

/**
 * Improved SprintReset module with better targeting, efficiency, and reliability
 */
@Info(name = "SprintReset", category = Category.Combat, description = "Stops holding movement keys when hitting enemies to improve combos")
public class SprintReset extends Module {

    private long lastPressTime = 0;
    private boolean isKeyPressed = false;
    private KeyBinding targetKey = null;
    private Entity lastTarget = null;
    
    // Cache settings for performance
    private boolean swordOnly = true;
    private int pressDelay = 500;
    private int holdTime = 100;
    private String keyType = "W Key";

    public SprintReset() {
        ArrayList<String> mode = new ArrayList<>();
        mode.add("W Key");
        mode.add("S Key");
        mode.add("A Key");
        mode.add("D Key");
        
        addSetting(new Setting("Sword Only", this, true));
        addSetting(new Setting("Type", this, "W Key", mode));
        addSetting(new Setting("Delay", this, 500, 100, 2000, true));
        addSetting(new Setting("Hold Time", this, 100, 50, 250, true));
        addSetting(new Setting("Check Line of Sight", this, true));
        addSetting(new Setting("Max Distance", this, 4.0, 2.0, 6.0, false));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastPressTime = 0;
        isKeyPressed = false;
        lastTarget = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Ensure we release any pressed keys when disabled
        if (isKeyPressed && targetKey != null) {
            targetKey.pressed = false;
            isKeyPressed = false;
        }
    }

    @EventTarget
    public void onUpdate(EventTick event) {
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null || 
            !mc.thePlayer.isEntityAlive() || mc.currentScreen != null) {
            return;
        }
        
        // Update cached settings
        updateSettings();
        
        // Check if we should activate based on current conditions
        if (shouldActivate()) {
            activateSprintReset();
        }
        
        // Check if we need to release the key after hold time
        if (isKeyPressed && System.currentTimeMillis() - lastPressTime >= holdTime) {
            releaseKey();
        }
    }
    
    private void updateSettings() {
        swordOnly = Hydrogen.getClient().settingsManager.getSettingByName(this, "Sword Only").isEnabled();
        pressDelay = (int) Hydrogen.getClient().settingsManager.getSettingByName("Delay").getValue();
        holdTime = (int) Hydrogen.getClient().settingsManager.getSettingByName("Hold Time").getValue();
        keyType = Hydrogen.getClient().settingsManager.getSettingByName(this, "Type").getMode();
        targetKey = getKeyBinding();
    }
    
    private boolean shouldActivate() {
        // Check if player is swinging and has a valid target
        if (!mc.thePlayer.isSwingInProgress) return false;
        
        // Check if holding a sword when swordOnly is enabled
        if (swordOnly && !(mc.thePlayer.getHeldItem() != null && 
            mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
            return false;
        }
        
        // Check cooldown
        if (System.currentTimeMillis() - lastPressTime < pressDelay) return false;
        
        // Get current target
        Entity target = getCurrentTarget();
        if (target == null) return false;
        
        // Check if target is valid
        if (!isValidTarget(target)) return false;
        
        lastTarget = target;
        return true;
    }
    
    private Entity getCurrentTarget() {
        // First check if we're looking at an entity
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return mc.objectMouseOver.entityHit;
        }
        
        // Fallback to checking entities in range
        double maxDistance = Hydrogen.getClient().settingsManager.getSettingByName("Max Distance").getValue();
        List<Entity> entities = mc.theWorld.getLoadedEntityList();
        
        for (Entity entity : entities) {
            if (entity != mc.thePlayer && 
                mc.thePlayer.getDistanceToEntity(entity) <= maxDistance && 
                isValidTarget(entity)) {
                return entity;
            }
        }
        
        return null;
    }
    
    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof EntityLivingBase) || !entity.isEntityAlive()) {
            return false;
        }
        
        // Check line of sight if enabled
        boolean checkLOS = Hydrogen.getClient().settingsManager.getSettingByName(this, "Check Line of Sight").isEnabled();
        if (checkLOS && !mc.thePlayer.canEntityBeSeen(entity)) {
            return false;
        }
        
        // Check distance
        double maxDistance = Hydrogen.getClient().settingsManager.getSettingByName("Max Distance").getValue();
        if (mc.thePlayer.getDistanceToEntity(entity) > maxDistance) {
            return false;
        }
        
        // Only target players if it's a player
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            // Add additional checks for players if needed
            return true;
        }
        
        return true;
    }
    
    private void activateSprintReset() {
        if (targetKey == null) return;
        
        // Press the key
        targetKey.pressed = true;
        isKeyPressed = true;
        lastPressTime = System.currentTimeMillis();
    }
    
    private void releaseKey() {
        if (targetKey != null) {
            targetKey.pressed = false;
        }
        isKeyPressed = false;
    }
    
    private KeyBinding getKeyBinding() {
        switch (keyType) {
            case "W Key": return mc.gameSettings.keyBindForward;
            case "S Key": return mc.gameSettings.keyBindBack;
            case "A Key": return mc.gameSettings.keyBindLeft;
            case "D Key": return mc.gameSettings.keyBindRight;
            default: return mc.gameSettings.keyBindForward;
        }
    }
}
