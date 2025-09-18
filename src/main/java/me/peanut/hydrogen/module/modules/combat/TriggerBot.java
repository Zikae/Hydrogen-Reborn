package me.peanut.hydrogen.module.modules.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.MovingObjectPosition;
import me.peanut.hydrogen.events.EventUpdate;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.settings.Setting;
import me.peanut.hydrogen.utils.TimeUtils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Info(name = "TriggerBot", description = "Automatically attacks entities when hovering over them", category = Category.Combat)
public class TriggerBot extends Module {

    // Settings
    private final Setting<Double> minCPS = register(new Setting<>("MinCPS", 8.0, 1.0, 20.0));
    private final Setting<Double> maxCPS = register(new Setting<>("MaxCPS", 12.0, 1.0, 20.0));
    private final Setting<Integer> randomDelay = register(new Setting<>("RandomMS", 25, 0, 100));
    private final Setting<Boolean> weaponsOnly = register(new Setting<>("WeaponsOnly", true));
    private final Setting<Boolean> targetPlayers = register(new Setting<>("Players", true));
    private final Setting<Boolean> targetMobs = register(new Setting<>("Mobs", true));
    private final Setting<Boolean> targetAnimals = register(new Setting<>("Animals", false));
    private final Setting<Boolean> ignoreTeam = register(new Setting<>("IgnoreTeam", true));
    private final Setting<Boolean> throughWalls = register(new Setting<>("ThroughWalls", false));
    private final Setting<Double> hitChance = register(new Setting<>("HitChance", 100.0, 10.0, 100.0));
    private final Setting<Boolean> autoBlock = register(new Setting<>("AutoBlock", false));

    // State variables
    private final TimeUtils timer = new TimeUtils();
    private long nextAttackTime = 0;
    private boolean wasBlocking = false;
    private EntityLivingBase lastTarget = null;

    public TriggerBot() {
        super();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        timer.reset();
        nextAttackTime = 0;
        wasBlocking = false;
        lastTarget = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (wasBlocking && mc.thePlayer != null) {
            stopBlocking();
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!canOperate()) {
            return;
        }

        EntityLivingBase target = getTargetEntity();
        
        if (target != null && shouldAttack(target)) {
            performAttack(target);
        } else if (wasBlocking && autoBlock.getValue()) {
            stopBlocking();
        }
    }

    private boolean canOperate() {
        return mc.thePlayer != null && 
               mc.theWorld != null && 
               !isGuiOpen() && 
               (!weaponsOnly.getValue() || isHoldingWeapon());
    }

    private boolean isGuiOpen() {
        return mc.currentScreen instanceof GuiContainer || 
               mc.currentScreen instanceof GuiChat || 
               (mc.currentScreen instanceof GuiScreen && !(mc.currentScreen instanceof net.minecraft.client.gui.GuiIngame));
    }

    private boolean isHoldingWeapon() {
        if (mc.thePlayer.getCurrentEquippedItem() == null) {
            return false;
        }
        
        Item item = mc.thePlayer.getCurrentEquippedItem().getItem();
        return item instanceof ItemSword || 
               item instanceof ItemAxe || 
               item instanceof ItemPickaxe ||
               item instanceof ItemBow;
    }

    private EntityLivingBase getTargetEntity() {
        if (mc.objectMouseOver == null || 
            mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY ||
            mc.objectMouseOver.entityHit == null ||
            !(mc.objectMouseOver.entityHit instanceof EntityLivingBase)) {
            return null;
        }

        EntityLivingBase entity = (EntityLivingBase) mc.objectMouseOver.entityHit;
        
        // Wall check
        if (!throughWalls.getValue() && !mc.thePlayer.canEntityBeSeen(entity)) {
            return false;
        }

        return entity;
    }

    private boolean shouldAttack(EntityLivingBase target) {
        if (target == null || target.isDead || target == mc.thePlayer) {
            return false;
        }

        // Hit chance check
        if (ThreadLocalRandom.current().nextDouble(0, 100) > hitChance.getValue()) {
            return false;
        }

        // Timing check
        if (System.currentTimeMillis() < nextAttackTime) {
            return false;
        }

        // Target type checks
        if (!isValidTargetType(target)) {
            return false;
        }

        // Team check
        if (ignoreTeam.getValue() && target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            if (isTeammate(player)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidTargetType(EntityLivingBase target) {
        if (target instanceof EntityPlayer) {
            return targetPlayers.getValue();
        } else if (target instanceof EntityMob) {
            return targetMobs.getValue();
        } else if (target instanceof EntityAnimal) {
            return targetAnimals.getValue();
        }
        
        // Default to mobs setting for other living entities
        return targetMobs.getValue();
    }

    private boolean isTeammate(EntityPlayer player) {
        if (mc.thePlayer.getTeam() != null && player.getTeam() != null) {
            return mc.thePlayer.getTeam().isSameTeam(player.getTeam());
        }
        
        // Simple name color check as fallback
        String playerName = player.getDisplayName().getFormattedText();
        String myName = mc.thePlayer.getDisplayName().getFormattedText();
        
        // Extract color codes (§ followed by color code)
        String playerColor = extractColorCode(playerName);
        String myColor = extractColorCode(myName);
        
        return playerColor != null && playerColor.equals(myColor);
    }

    private String extractColorCode(String text) {
        if (text.length() >= 2 && text.charAt(0) == '§') {
            return text.substring(0, 2);
        }
        return null;
    }

    private void performAttack(EntityLivingBase target) {
        try {
            // Auto-block logic
            if (autoBlock.getValue() && isHoldingSword() && !wasBlocking) {
                startBlocking();
            }

            // Stop blocking temporarily if we were blocking
            boolean shouldResumeBlocking = wasBlocking;
            if (wasBlocking) {
                stopBlocking();
            }

            // Perform the attack
            mc.thePlayer.swingItem();
            mc.playerController.attackEntity(mc.thePlayer, target);

            // Resume blocking if needed
            if (shouldResumeBlocking && autoBlock.getValue()) {
                startBlocking();
            }

            // Calculate next attack time with randomization
            double cps = calculateRandomCPS();
            long baseDelay = Math.round(1000.0 / cps);
            int randomMs = ThreadLocalRandom.current().nextInt(0, randomDelay.getValue() + 1);
            
            nextAttackTime = System.currentTimeMillis() + baseDelay + randomMs;
            lastTarget = target;
            
        } catch (Exception e) {
            System.err.println("TriggerBot attack error: " + e.getMessage());
        }
    }

    private double calculateRandomCPS() {
        double min = Math.min(minCPS.getValue(), maxCPS.getValue());
        double max = Math.max(minCPS.getValue(), maxCPS.getValue());
        
        if (min == max) {
            return min;
        }
        
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    private boolean isHoldingSword() {
        return mc.thePlayer.getCurrentEquippedItem() != null && 
               mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword;
    }

    private void startBlocking() {
        if (mc.thePlayer.getCurrentEquippedItem() != null && 
            mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword) {
            mc.gameSettings.keyBindUseItem.pressed = true;
            wasBlocking = true;
        }
    }

    private void stopBlocking() {
        mc.gameSettings.keyBindUseItem.pressed = false;
        wasBlocking = false;
    }

    // Utility methods for external access
    public boolean isAttacking() {
        return System.currentTimeMillis() < nextAttackTime + 100; // 100ms grace period
    }

    public EntityLivingBase getLastTarget() {
        return lastTarget;
    }

    public double getCurrentCPS() {
        if (nextAttackTime == 0) return 0;
        long timeSinceAttack = System.currentTimeMillis() - (nextAttackTime - Math.round(1000.0 / calculateRandomCPS()));
        return timeSinceAttack > 0 ? 1000.0 / timeSinceAttack : 0;
    }
}
