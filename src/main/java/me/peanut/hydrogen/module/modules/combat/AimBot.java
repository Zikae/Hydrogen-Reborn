package me.peanut.hydrogen.module.modules.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import me.peanut.hydrogen.events.EventRender3D;
import me.peanut.hydrogen.events.EventUpdate;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.settings.Setting;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Info(name = "AimAssist", description = "Automatically aims at enemies", category = Category.Combat, keybind = Keyboard.KEY_R)
public class AimBot extends Module {

    final HashMap<String, GetCriteriaValue> selectionCriterias = new HashMap<>();
    private EntityLivingBase target = null;
    private boolean offset = false;
    private boolean onlyPrimaryTarget = false;

    public AimBot() {
        selectionCriterias.put("Distance", (thePlayer, target) -> 
            thePlayer.getPositionEyes(0).distanceTo(target.getPositionEyes(0)));
        selectionCriterias.put("Health", (thePlayer, target) -> target.getHealth());

        ArrayList<String> selectionOrders = new ArrayList<>(selectionCriterias.keySet());

        addSetting(new Setting("Select", this, "Health", selectionOrders));
        addSetting(new Setting("Select smaller", this, true));
        addSetting(new Setting("on Click Only", this, false));
        addSetting(new Setting("Max Distance", this, 4.2, 1, 6, false));
        addSetting(new Setting("Visible Only", this, true));
        addSetting(new Setting("Alive Only", this, true));
        addSetting(new Setting("Offset", this, true));
        addSetting(new Setting("Target Only", this, false));
        addSetting(new Setting("Target Player", this, false));
        addSetting(new Setting("Target Mobs", this, false));
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        boolean targetPlayer = h2.settingsManager.getSettingByName(this, "Target Player").isEnabled();
        boolean targetMobs = h2.settingsManager.getSettingByName(this, "Target Mobs").isEnabled();
        boolean selectSmaller = h2.settingsManager.getSettingByName(this, "Select smaller").isEnabled();
        boolean visibleOnly = h2.settingsManager.getSettingByName(this, "Visible Only").isEnabled();
        boolean aliveOnly = h2.settingsManager.getSettingByName(this, "Alive Only").isEnabled();
        double maxDistance = h2.settingsManager.getSettingByName("Max Distance").getValue();
        offset = h2.settingsManager.getSettingByName(this, "Offset").isEnabled();
        onlyPrimaryTarget = h2.settingsManager.getSettingByName(this, "Target Only").isEnabled();
        
        String selectionMode = h2.settingsManager.getSettingByName("Select").getMode();
        GetCriteriaValue getCriteriaValue = selectionCriterias.get(selectionMode);

        if (getCriteriaValue == null) {
            System.err.println("Invalid selection criteria: " + selectionMode);
            return;
        }

        target = null;
        double bestValue = selectSmaller ? Double.MAX_VALUE : Double.MIN_VALUE;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!isValidEntity(entity, targetPlayer, targetMobs)) continue;
            if (!isWithinRange(entity, maxDistance)) continue;
            if (!isVisible(entity, visibleOnly)) continue;
            if (!isAlive(entity, aliveOnly)) continue;

            EntityLivingBase livingEntity = (EntityLivingBase) entity;
            double candidateValue = getCriteriaValue.op(mc.thePlayer, livingEntity);
            
            if (isBetterCandidate(candidateValue, bestValue, selectSmaller)) {
                bestValue = candidateValue;
                target = livingEntity;
                if (target.equals(TargetSelect.primaryTarget)) {
                    return;
                }
            }
        }
    }

    @EventTarget
    public void onRender(EventRender3D e) {
        if (target == null) return;
        if (onlyPrimaryTarget && !target.equals(TargetSelect.primaryTarget)) return;

        boolean onClick = h2.settingsManager.getSettingByName(this, "on Click Only").isEnabled();
        if (onClick && !mc.gameSettings.keyBindAttack.pressed) return;

        Vec3 targetPos = target.getPositionEyes(e.getPartialTicks());
        Vec3 playerPos = mc.thePlayer.getPositionEyes(e.getPartialTicks());
        
        double diffX = targetPos.xCoord - playerPos.xCoord;
        double diffY = targetPos.yCoord - playerPos.yCoord;
        double diffZ = targetPos.zCoord - playerPos.zCoord;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        mc.thePlayer.rotationYaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        mc.thePlayer.rotationPitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        if (offset) {
            double timeFactor = System.currentTimeMillis() / 200.0;
            mc.thePlayer.rotationYaw += (float) Math.sin(timeFactor) * 2f;
            mc.thePlayer.rotationPitch += (float) Math.cos(timeFactor) * 2f;
        }
    }

    private boolean isValidEntity(Entity entity, boolean targetPlayer, boolean targetMobs) {
        return (targetPlayer && entity instanceof EntityOtherPlayerMP) || 
               (targetMobs && entity instanceof EntityMob);
    }

    private boolean isWithinRange(Entity entity, double maxDistance) {
        return mc.thePlayer.getPositionEyes(0).distanceTo(entity.getPositionEyes(0)) <= maxDistance;
    }

    private boolean isVisible(Entity entity, boolean visibleOnly) {
        return !visibleOnly || !entity.isInvisible();
    }

    private boolean isAlive(Entity entity, boolean aliveOnly) {
        return !aliveOnly || (entity.isEntityAlive() && !entity.isDead);
    }

    private boolean isBetterCandidate(double candidate, double current, boolean selectSmaller) {
        return selectSmaller ? candidate < current : candidate > current;
    }

    public interface GetCriteriaValue {
        double op(EntityPlayerSP thePlayer, EntityLivingBase target);
    }
}
