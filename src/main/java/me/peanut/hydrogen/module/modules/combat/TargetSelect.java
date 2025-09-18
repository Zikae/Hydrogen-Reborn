package me.peanut.hydrogen.module.modules.combat;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.util.*;
import org.lwjgl.input.Mouse;
import me.peanut.hydrogen.command.Command;
import me.peanut.hydrogen.events.EventPrimaryTargetSelected;
import me.peanut.hydrogen.events.EventUpdate;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.settings.Setting;

import java.util.List;
import java.util.Objects;

@Info(name = "TargetSelect", description = "Press middle mouse on an entity to focus them using AimAssist", category = Category.Combat)
public class TargetSelect extends Module {

    // Settings
    private final Setting<Double> maxDistance = register(new Setting<>("MaxDistance", 6.0, 3.0, 12.0));
    private final Setting<Double> minDistance = register(new Setting<>("MinDistance", 3.0, 1.0, 6.0));
    private final Setting<Boolean> playersOnly = register(new Setting<>("PlayersOnly", false));
    private final Setting<Boolean> showMessages = register(new Setting<>("ShowMessages", true));
    
    // State variables
    public static EntityLivingBase primaryTarget = null;
    private Entity pointedEntity;
    private boolean wasMousePressed = false;
    private long lastTargetTime = 0;
    private static final long TARGET_COOLDOWN = 250; // ms

    @Override
    public void onEnable() {
        super.onEnable();
        primaryTarget = null;
        pointedEntity = null;
        wasMousePressed = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearTarget();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        boolean isMousePressed = Mouse.isButtonDown(2); // Middle mouse button
        
        // Only process on mouse press (not hold) and respect cooldown
        if (isMousePressed && !wasMousePressed && canSelectNewTarget()) {
            handleTargetSelection();
        }
        
        wasMousePressed = isMousePressed;
        
        // Clear target if it becomes invalid
        validateCurrentTarget();
    }

    private boolean canSelectNewTarget() {
        return System.currentTimeMillis() - lastTargetTime > TARGET_COOLDOWN;
    }

    private void handleTargetSelection() {
        Entity entityUnderCursor = getEntityUnderCursor();
        
        if (entityUnderCursor instanceof EntityLivingBase) {
            EntityLivingBase newTarget = (EntityLivingBase) entityUnderCursor;
            
            // Additional validation
            if (isValidTarget(newTarget)) {
                setNewTarget(newTarget);
                lastTargetTime = System.currentTimeMillis();
            }
        } else if (entityUnderCursor == null) {
            // Clear target if clicking on empty space
            clearTarget();
        }
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity == null || entity == mc.thePlayer) {
            return false;
        }
        
        if (playersOnly.getValue() && !entity.isInstanceOf(EntityPlayer.class)) {
            return false;
        }
        
        double distance = mc.thePlayer.getDistanceToEntity(entity);
        return distance <= maxDistance.getValue();
    }

    private void setNewTarget(EntityLivingBase newTarget) {
        EntityLivingBase oldTarget = primaryTarget;
        primaryTarget = newTarget;
        
        // Only fire event and show message if target actually changed
        if (!Objects.equals(oldTarget, newTarget)) {
            EventManager.call(new EventPrimaryTargetSelected(primaryTarget));
            
            if (showMessages.getValue()) {
                String targetName = newTarget.getName();
                Command.msg("§7Target selected: §f" + targetName);
            }
        }
    }

    private void clearTarget() {
        if (primaryTarget != null) {
            primaryTarget = null;
            if (showMessages.getValue()) {
                Command.msg("§7Target cleared");
            }
        }
    }

    private void validateCurrentTarget() {
        if (primaryTarget != null) {
            if (primaryTarget.isDead || 
                mc.thePlayer.getDistanceToEntity(primaryTarget) > maxDistance.getValue() * 1.5) {
                clearTarget();
            }
        }
    }

    @EventTarget
    public void onPrimaryTargetSelected(EventPrimaryTargetSelected event) {
        if (event.getTarget() != null) {
            System.out.println("Target selected: " + event.getTarget().getName() + 
                             " [" + event.getTarget().getClass().getSimpleName() + "]");
        }
    }

    /**
     * Gets the entity currently under the player's cursor using raycasting
     * @return The entity under cursor, or null if none
     */
    private Entity getEntityUnderCursor() {
        Entity viewEntity = mc.getRenderViewEntity();
        if (viewEntity == null || mc.theWorld == null) {
            return null;
        }

        double maxDist = maxDistance.getValue();
        MovingObjectPosition mouseOver = viewEntity.rayTrace(maxDist, 1.0F);
        double currentDistance = maxDist;
        Vec3 eyePosition = viewEntity.getPositionEyes(1.0F);

        // Update distance if we hit something
        if (mouseOver != null) {
            currentDistance = mouseOver.hitVec.distanceTo(eyePosition);
        }

        // Get look vector and end position
        Vec3 lookVector = viewEntity.getLook(1.0F);
        Vec3 endPosition = eyePosition.addVector(
            lookVector.xCoord * maxDist, 
            lookVector.yCoord * maxDist, 
            lookVector.zCoord * maxDist
        );

        Entity targetEntity = null;
        double closestDistance = currentDistance;
        
        // Create expanded bounding box for entity search
        AxisAlignedBB searchArea = viewEntity.getEntityBoundingBox()
            .addCoord(lookVector.xCoord * maxDist, lookVector.yCoord * maxDist, lookVector.zCoord * maxDist)
            .expand(1.0F, 1.0F, 1.0F);

        // Get entities in search area
        List<Entity> entities = mc.theWorld.getEntitiesInAABBexcluding(
            viewEntity, 
            searchArea, 
            Predicates.and(
                EntitySelectors.NOT_SPECTATING,
                entity -> entity != null && entity.canBeCollidedWith()
            )
        );

        // Find closest entity that intersects with our ray
        for (Entity entity : entities) {
            float borderSize = entity.getCollisionBorderSize();
            AxisAlignedBB entityBB = entity.getEntityBoundingBox()
                .expand(borderSize, borderSize, borderSize);
            MovingObjectPosition intercept = entityBB.calculateIntercept(eyePosition, endPosition);

            if (entityBB.isVecInside(eyePosition)) {
                // We're inside the entity's bounding box
                if (closestDistance >= 0.0D) {
                    targetEntity = entity;
                    closestDistance = 0.0D;
                }
            } else if (intercept != null) {
                double distance = eyePosition.distanceTo(intercept.hitVec);
                
                if (distance < closestDistance || closestDistance == 0.0D) {
                    // Handle riding entity edge case
                    if (entity == viewEntity.ridingEntity && !viewEntity.canRiderInteract()) {
                        if (closestDistance == 0.0D) {
                            targetEntity = entity;
                        }
                    } else {
                        targetEntity = entity;
                        closestDistance = distance;
                    }
                }
            }
        }

        // Additional distance validation
        if (targetEntity != null && closestDistance > minDistance.getValue()) {
            return targetEntity;
        }

        return null;
    }

    public static EntityLivingBase getPrimaryTarget() {
        return primaryTarget;
    }

    public static boolean hasTarget() {
        return primaryTarget != null && !primaryTarget.isDead;
    }

    public static void forceSetTarget(EntityLivingBase target) {
        primaryTarget = target;
    }

    public static void forceClearTarget() {
        primaryTarget = null;
    }
}
