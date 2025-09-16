package me.peanut.hydrogen.module.modules.combat;

import com.darkmagician6.eventapi.EventTarget;
import me.peanut.hydrogen.Hydrogen;
import net.minecraft.client.Minecraft;
import me.peanut.hydrogen.events.EventUpdate;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.settings.Setting;

import java.util.ArrayList;
import java.util.Random;


@Info(name = "AutoClicker", description = "Automatically clicks for you (testing/human-like)", category = Category.Combat)
public class AutoClicker extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();

    private long leftLastSwing = 0L;
    private long rightLastSwing = 0L;

    
    private long leftNextDelay = 0L;
    private long rightNextDelay = 0L;

    public AutoClicker() {
        ArrayList<String> mode = new ArrayList<>();
        mode.add("Left Click");
        mode.add("Right Click");

        addSetting(new Setting("Type", this, "Left Click", mode));
        addSetting(new Setting("Min. CPS", this, 4, 1, 20, true));
        addSetting(new Setting("Max. CPS", this, 8, 1, 20, true));

        
        leftNextDelay = computeDelayMillis(4, 8);
        rightNextDelay = computeDelayMillis(4, 8);
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        int minCPS = (int) h2.settingsManager.getSettingByName(this, "Min. CPS").getValue();
        int maxCPS = (int) h2.settingsManager.getSettingByName(this, "Max. CPS").getValue();
        boolean leftClick = h2.settingsManager.getSettingByName(this, "Type").getMode().equalsIgnoreCase("Left Click");
        boolean rightClick = h2.settingsManager.getSettingByName(this, "Type").getMode().equalsIgnoreCase("Right Click");

        long now = System.currentTimeMillis();

        // LEFT CLICK logic
        if (leftClick && mc.gameSettings.keyBindAttack.isKeyDown()) {
            if (now - leftLastSwing >= leftNextDelay) {
                mc.clickMouse();
                leftLastSwing = now;
                leftNextDelay = computeDelayMillis(minCPS, maxCPS);
            }
        }

        // RIGHT CLICK logic 
        if (rightClick && mc.gameSettings.keyBindUseItem.isKeyDown() && !mc.thePlayer.isUsingItem()) {
            if (now - rightLastSwing >= rightNextDelay) {
                mc.rightClickMouse();
                rightLastSwing = now;
                rightNextDelay = computeDelayMillis(minCPS, maxCPS);
            }
        }
    }

    
    private long computeDelayMillis(int minCPS, int maxCPS) {
        if (minCPS <= 0) minCPS = 1;
        if (maxCPS < minCPS) maxCPS = minCPS;

        double cps = minCPS + random.nextDouble() * (maxCPS - minCPS + 0.0);
        double baseDelay = 1000.0 / cps; // ms

        double jitter = random.nextGaussian() * (baseDelay * 0.08);
        long delay = Math.max(1L, Math.round(baseDelay + jitter));
        return delay;
    }
}
