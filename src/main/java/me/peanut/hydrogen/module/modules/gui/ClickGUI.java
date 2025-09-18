package me.peanut.hydrogen.module.modules.gui;

import me.peanut.hydrogen.file.files.ClickGuiConfig;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.ui.clickgui.ClickGui;
import me.peanut.hydrogen.settings.Setting;
import me.peanut.hydrogen.utils.ColorUtil;
import com.darkmagician6.eventapi.EventTarget;
import me.peanut.hydrogen.events.EventKey;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Enhanced ClickGUI Module with comprehensive customization options
 * @author Zikae
 * @version 2.0
 */
@Info(name = "ClickGUI", description = "Customizable click-based GUI for module management", category = Category.Gui, keybind = Keyboard.KEY_RSHIFT)
public class ClickGUI extends Module {

    // Core GUI instance
    private ClickGui clickGui;
    private ClickGuiConfig config;
    private boolean firstLoad = true;

    // Visual Settings
    private final Setting<String> fontType = register(new Setting<>("FontType", "TTF", Arrays.asList("TTF", "Minecraft", "Arial", "Consolas")));
    private final Setting<Float> fontSize = register(new Setting<>("FontSize", 14.0f, 10.0f, 24.0f));
    private final Setting<Boolean> antiAliasing = register(new Setting<>("AntiAliasing", true));
    
    // Theme Settings
    private final Setting<String> theme = register(new Setting<>("Theme", "Dark", Arrays.asList("Dark", "Light", "Blue", "Purple", "Custom")));
    private final Setting<Integer> accentRed = register(new Setting<>("AccentRed", 163, 0, 255));
    private final Setting<Integer> accentGreen = register(new Setting<>("AccentGreen", 255, 0, 255));
    private final Setting<Integer> accentBlue = register(new Setting<>("AccentBlue", 223, 0, 255));
    private final Setting<Integer> accentAlpha = register(new Setting<>("AccentAlpha", 220, 0, 255));
    
    // Background Settings
    private final Setting<Integer> backgroundRed = register(new Setting<>("BackgroundRed", 30, 0, 255));
    private final Setting<Integer> backgroundGreen = register(new Setting<>("BackgroundGreen", 30, 0, 255));
    private final Setting<Integer> backgroundBlue = register(new Setting<>("BackgroundBlue", 30, 0, 255));
    private final Setting<Integer> backgroundAlpha = register(new Setting<>("BackgroundAlpha", 180, 0, 255));
    
    // Effects Settings
    private final Setting<Boolean> blur = register(new Setting<>("Blur", true));
    private final Setting<Boolean> shadow = register(new Setting<>("Shadow", true));
    private final Setting<Boolean> tooltip = register(new Setting<>("Tooltip", true));
    private final Setting<Boolean> particles = register(new Setting<>("Particles", false));
    private final Setting<Boolean> animations = register(new Setting<>("Animations", true));
    private final Setting<Float> animationSpeed = register(new Setting<>("AnimationSpeed", 0.5f, 0.1f, 2.0f));
    
    // Layout Settings
    private final Setting<Float> scale = register(new Setting<>("Scale", 1.0f, 0.5f, 2.0f));
    private final Setting<Integer> categoryWidth = register(new Setting<>("CategoryWidth", 120, 80, 200));
    private final Setting<Integer> moduleHeight = register(new Setting<>("ModuleHeight", 15, 10, 25));
    private final Setting<Boolean> compactMode = register(new Setting<>("CompactMode", false));
    
    // Behavior Settings
    private final Setting<Boolean> autoSave = register(new Setting<>("AutoSave", true));
    private final Setting<Boolean> closeOnEscape = register(new Setting<>("CloseOnEscape", true));
    private final Setting<Boolean> pauseGame = register(new Setting<>("PauseGame", false));
    private final Setting<Boolean> searchFunction = register(new Setting<>("SearchFunction", true));
    
    // Sound Settings
    private final Setting<Boolean> sounds = register(new Setting<>("Sounds", true));
    private final Setting<Float> soundVolume = register(new Setting<>("SoundVolume", 0.5f, 0.0f, 1.0f));
    
    public ClickGUI() {
        super();
        this.config = new ClickGuiConfig();
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            toggle();
            return;
        }

        initializeGui();
        loadConfiguration();
        applyTheme();
        
        mc.displayGuiScreen(this.clickGui);
        
        // Auto-disable after opening
        toggle();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (clickGui != null && autoSave.getValue()) {
            saveConfiguration();
        }
        super.onDisable();
    }

    @EventTarget
    public void onKey(EventKey event) {
        // Allow reopening GUI with keybind even when disabled
        if (event.getKey() == getKeybind() && mc.currentScreen == null) {
            if (!isEnabled()) {
                enable();
            }
        }
    }

    private void initializeGui() {
        if (this.clickGui == null || firstLoad) {
            this.clickGui = new ClickGui();
            firstLoad = false;
        }
        
        // Apply current settings to GUI
        updateGuiSettings();
    }

    private void updateGuiSettings() {
        if (clickGui != null) {
            // Apply visual settings
            clickGui.setFontType(fontType.getValue());
            clickGui.setFontSize(fontSize.getValue());
            clickGui.setAntiAliasing(antiAliasing.getValue());
            
            // Apply colors
            clickGui.setAccentColor(getAccentColor());
            clickGui.setBackgroundColor(getBackgroundColor());
            
            // Apply effects
            clickGui.setBlurEnabled(blur.getValue());
            clickGui.setShadowEnabled(shadow.getValue());
            clickGui.setTooltipEnabled(tooltip.getValue());
            clickGui.setParticlesEnabled(particles.getValue());
            clickGui.setAnimationsEnabled(animations.getValue());
            clickGui.setAnimationSpeed(animationSpeed.getValue());
            
            // Apply layout
            clickGui.setScale(scale.getValue());
            clickGui.setCategoryWidth(categoryWidth.getValue());
            clickGui.setModuleHeight(moduleHeight.getValue());
            clickGui.setCompactMode(compactMode.getValue());
            
            // Apply behavior
            clickGui.setCloseOnEscape(closeOnEscape.getValue());
            clickGui.setPauseGame(pauseGame.getValue());
            clickGui.setSearchEnabled(searchFunction.getValue());
            
            // Apply sound
            clickGui.setSoundsEnabled(sounds.getValue());
            clickGui.setSoundVolume(soundVolume.getValue());
        }
    }

    private void applyTheme() {
        String selectedTheme = theme.getValue();
        
        switch (selectedTheme) {
            case "Dark":
                setThemeColors(163, 255, 223, 220, 30, 30, 30, 180);
                break;
            case "Light":
                setThemeColors(50, 150, 255, 255, 240, 240, 240, 200);
                break;
            case "Blue":
                setThemeColors(100, 150, 255, 255, 20, 25, 40, 180);
                break;
            case "Purple":
                setThemeColors(150, 100, 255, 255, 40, 20, 40, 180);
                break;
            case "Custom":
                // Use user-defined colors
                break;
        }
        
        updateGuiSettings();
    }

    private void setThemeColors(int r, int g, int b, int a, int bgR, int bgG, int bgB, int bgA) {
        if (!theme.getValue().equals("Custom")) {
            accentRed.setValue(r);
            accentGreen.setValue(g);
            accentBlue.setValue(b);
            accentAlpha.setValue(a);
            backgroundRed.setValue(bgR);
            backgroundGreen.setValue(bgG);
            backgroundBlue.setValue(bgB);
            backgroundAlpha.setValue(bgA);
        }
    }

    private void loadConfiguration() {
        try {
            if (config != null) {
                config.loadConfig();
            }
        } catch (Exception e) {
            System.err.println("Failed to load ClickGUI configuration: " + e.getMessage());
        }
    }

    private void saveConfiguration() {
        try {
            if (config != null) {
                config.saveConfig();
            }
        } catch (Exception e) {
            System.err.println("Failed to save ClickGUI configuration: " + e.getMessage());
        }
    }

    // Color utility methods
    public Color getAccentColor() {
        return new Color(
            accentRed.getValue(),
            accentGreen.getValue(),
            accentBlue.getValue(),
            accentAlpha.getValue()
        );
    }

    public Color getBackgroundColor() {
        return new Color(
            backgroundRed.getValue(),
            backgroundGreen.getValue(),
            backgroundBlue.getValue(),
            backgroundAlpha.getValue()
        );
    }

    public int getAccentColorInt() {
        return ColorUtil.toHex(
            accentRed.getValue(),
            accentGreen.getValue(),
            accentBlue.getValue(),
            accentAlpha.getValue()
        );
    }

    public int getBackgroundColorInt() {
        return ColorUtil.toHex(
            backgroundRed.getValue(),
            backgroundGreen.getValue(),
            backgroundBlue.getValue(),
            backgroundAlpha.getValue()
        );
    }

    // Getter methods for external access
    public ClickGui getClickGui() {
        return clickGui;
    }

    public boolean isGuiOpen() {
        return mc.currentScreen instanceof ClickGui;
    }

    // Theme management
    public void cycleTheme() {
        String[] themes = {"Dark", "Light", "Blue", "Purple", "Custom"};
        String current = theme.getValue();
        
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].equals(current)) {
                String next = themes[(i + 1) % themes.length];
                theme.setValue(next);
                applyTheme();
                break;
            }
        }
    }

    public void resetToDefaults() {
        theme.setValue("Dark");
        fontType.setValue("TTF");
        fontSize.setValue(14.0f);
        scale.setValue(1.0f);
        blur.setValue(true);
        tooltip.setValue(true);
        particles.setValue(false);
        animations.setValue(true);
        applyTheme();
        updateGuiSettings();
    }

    // Quick access methods
    public void toggleBlur() {
        blur.setValue(!blur.getValue());
        updateGuiSettings();
    }

    public void toggleAnimations() {
        animations.setValue(!animations.getValue());
        updateGuiSettings();
    }

    public void toggleParticles() {
        particles.setValue(!particles.getValue());
        updateGuiSettings();
    }

    // Settings validation
    @Override
    public void onSettingChange(Setting setting) {
        super.onSettingChange(setting);
        
        // Validate and update GUI when settings change
        if (clickGui != null) {
            updateGuiSettings();
        }
        
        // Auto-save if enabled
        if (autoSave.getValue() && !firstLoad) {
            saveConfiguration();
        }
    }

    // Cleanup method
    public void cleanup() {
        if (autoSave.getValue()) {
            saveConfiguration();
        }
        if (clickGui != null) {
            clickGui.onGuiClosed();
        }
    }
}
