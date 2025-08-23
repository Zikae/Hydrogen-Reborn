package me.peanut.hydrogen.file.files;

import me.peanut.hydrogen.file.FileManager;
import me.peanut.hydrogen.ui.clickgui.ClickGui;
import me.peanut.hydrogen.ui.clickgui.component.Frame;
import me.peanut.hydrogen.utils.Logger;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by peanut on 03/02/2021
 * Modernized file handler for ClickGUI coordinates and state.
 */
@Deprecated
public class ClickGuiFile {

    private static final FileManager clickGuiCoord = new FileManager("clickgui", "Hydrogen");

    public ClickGuiFile() {
        try {
            loadClickGui();
        } catch (Exception ignored) {
        }
    }

    /**
     * Saves the current state and position of the ClickGUI frames to a file.
     */
    public static void saveClickGui() {
        try {
            clickGuiCoord.clear();
            for (Frame frame : ClickGui.frames) {
                String data = String.format("%s:%s:%s:%s",
                        frame.category.name(),
                        frame.getX(),
                        frame.getY(),
                        frame.isOpen());
                clickGuiCoord.write(data);
            }
        } catch (IOException e) {
            Logger.error("Failed to save ClickGUI file.", e);
        } catch (Exception ignored) {
        }
    }

    /**
     * Loads the ClickGUI frame positions and state from a file.
     */
    public static void loadClickGui() {
        try {
            for (String line : clickGuiCoord.read()) {
                String[] parts = line.split(":");
                if (parts.length != 4) {
                    continue; // Skip malformed lines
                }

                String panelName = parts[0];
                float panelCoordX = Float.parseFloat(parts[1]);
                float panelCoordY = Float.parseFloat(parts[2]);
                boolean extended = Boolean.parseBoolean(parts[3]);

                Optional<Frame> targetFrame = ClickGui.frames.stream()
                        .filter(frame -> frame.category.name().equalsIgnoreCase(panelName))
                        .findFirst();

                targetFrame.ifPresent(frame -> {
                    frame.setX((int) panelCoordX);
                    frame.setY((int) panelCoordY);
                    frame.setOpen(extended);
                });
            }
        } catch (Exception ignored) {
        }
    }
}

