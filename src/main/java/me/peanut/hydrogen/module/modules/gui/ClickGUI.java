package me.peanut.hydrogen.module.modules.gui;

import me.peanut.hydrogen.file.files.ClickGuiConfig;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.ui.clickgui.ClickGui;
import org.lwjgl.input.Keyboard;
import me.peanut.hydrogen.settings.Setting;

import java.util.ArrayList;

/**
 * Created by peanut on 03/02/2021
 * An improved and modernized ClickGUI module.
 */

@Info(name = "ClickGUI", description = "The click gui", category = Category.Gui, keybind = Keyboard.KEY_RSHIFT)
public class ClickGUI extends Module {

    public ClickGui clickgui;

    public ClickGUI() {
        ArrayList<String> font = new ArrayList<>();
        font.add("TTF");
        font.add("Minecraft");

        addSetting(new Setting("Font Type", this, "TTF", font));
        addSetting(new Setting("Blur", this, true));
        addSetting(new Setting("Tooltip", this, true));
        addSetting(new Setting("Particles", this, false));

        // Background Color Settings
        addSetting(new Setting("Background R", this, 20, 0, 255, true));
        addSetting(new Setting("Background G", this, 20, 0, 255, true));
        addSetting(new Setting("Background B", this, 20, 0, 255, true));
        addSetting(new Setting("Background A", this, 180, 0, 255, true));

        // Accent Color Settings
        addSetting(new Setting("Accent R", this, 163, 0, 255, true));
        addSetting(new Setting("Accent G", this, 223, 0, 255, true));
        addSetting(new Setting("Accent B", this, 255, 0, 255, true));
        addSetting(new Setting("Accent A", this, 255, 0, 255, true));

        // Text Color Settings
        addSetting(new Setting("Text R", this, 255, 0, 255, true));
        addSetting(new Setting("Text G", this, 255, 0, 255, true));
        addSetting(new Setting("Text B", this, 255, 0, 255, true));
        addSetting(new Setting("Text A", this, 255, 0, 255, true));
    }

    @Override
    public void onEnable() {
        if(this.clickgui == null) {
            this.clickgui = new ClickGui();
        }
        ClickGuiConfig clickGuiConfig = new ClickGuiConfig();
        clickGuiConfig.loadConfig();
        mc.displayGuiScreen(this.clickgui);
        toggle();
        super.onEnable();
    }
}
