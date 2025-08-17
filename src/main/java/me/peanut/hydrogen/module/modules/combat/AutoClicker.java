package me.peanut.hydrogen.module.modules.combat;

import com.darkmagician6.eventapi.EventTarget;
import me.peanut.hydrogen.Hydrogen;
import net.minecraft.client.Minecraft;
import me.peanut.hydrogen.events.EventUpdate;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.settings.Setting;
import me.peanut.hydrogen.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Random;

/**
 * An improved AutoClicker module.
 */

@Info(name = "AutoClicker", description = "Automatically clicks for you", category = Category.Combat)
public class AutoClicker extends Module {

    private long lastClickTime = 0;
    private long nextClickDelay = 0;
    private final Random random = new Random();

    public AutoClicker() {
        ArrayList<String> mode = new ArrayList<>();
        mode.add("Left Click");
        mode.add("Right Click");
        addSetting(new Setting("Type", this, "Left Click", mode));
        addSetting(new Setting("Min. CPS", this, 8, 1, 20, true));
        addSetting(new Setting("Max. CPS", this, 12, 1, 20, true));
        addSetting(new Setting("Jitter", this, false));
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        int minCPS = (int) h2.settingsManager.getSettingByName(this, "Min. CPS").getValue();
        int maxCPS = (int) h2.settingsManager.getSettingByName(this, "Max. CPS").getValue();
        boolean leftClick = h2.settingsManager.getSettingByName(this, "Type").getMode().equalsIgnoreCase("Left Click");
        boolean rightClick = h2.settingsManager.getSettingByName(this, "Type").getMode().equalsIgnoreCase("Right Click");
        boolean jitter = h2.settingsManager.getSettingByName(this, "Jitter").getValue();

        if (System.currentTimeMillis() - lastClickTime >= nextClickDelay) {
            if (leftClick && mc.gameSettings.keyBindAttack.isKeyDown()) {
                if (mc.objectMouseOver.entityHit != null) {
                    mc.clickMouse();
                    lastClickTime = System.currentTimeMillis();
                    nextClickDelay = calculateNextDelay(minCPS, maxCPS);
                    if (jitter) {
                        doJitter();
                    }
                }
            }
            if (rightClick && mc.gameSettings.keyBindUseItem.isKeyDown() && !mc.thePlayer.isUsingItem()) {
                mc.rightClickMouse();
                lastClickTime = System.currentTimeMillis();
                nextClickDelay = calculateNextDelay(minCPS, maxCPS);
                if (jitter) {
                    doJitter();
                }
            }
        }
    }

    private long
