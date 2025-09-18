package me.peanut.hydrogen.module.modules.movement;

import com.darkmagician6.eventapi.EventTarget;
import me.peanut.hydrogen.events.EventUpdate;
import me.peanut.hydrogen.module.Category;
import me.peanut.hydrogen.module.Info;
import me.peanut.hydrogen.module.Module;
import me.peanut.hydrogen.settings.Setting;
import me.peanut.hydrogen.utils.RotationUtils;
import me.peanut.hydrogen.utils.TimeUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Enhanced BlockClutch module for automatic block placement when falling
 * @author kambing (improved)
 * @version 2.0
 */
@Info(name = "BlockClutch", category = Category.Movement, description = "Automatically places blocks beneath you when falling")
public class BlockClutch extends Module {

    // Settings
    private final Setting<Double> range = register(new Setting<>("Range", 4.0, 1.0, 8.0));
    private final Setting<Boolean> autoSwitch = register(new Setting<>("AutoSwitch", true));
    private final Setting<Boolean> holdMode = register(new Setting<>("HoldMode", false));
    private final Setting<Double> fallDistance = register(new Setting<>("FallDistance", 3.0, 1.0, 10.0));
    private final Setting<Boolean> onlyWhenFalling = register(new Setting<>("OnlyWhenFalling", true));
    private final Setting<Boolean> rotations = register(new Setting<>("Rotations", true));
    private final Setting<Double> rotationSpeed = register(new Setting<>("RotationSpeed", 180.0, 50.0, 360.0));
    private final Setting<Boolean> rayCast = register(new Setting<>("RayCast", true));
    private final Setting<Boolean> swingArm = register(new Setting<>("SwingArm", true));
    private final Setting<Integer> delay = register(new Setting<>("Delay", 0, 0, 500));
    private final Setting<Boolean> onlyOnVoid = register(new Setting<>("OnlyOnVoid", false));
    private final Setting<Boolean> keepY = register(new Setting<>("KeepY", false));
    private final Setting<Integer> blocksToSave = register(new Setting<>("BlocksToSave", 32, 1, 128));
    private final Setting<Boolean> towerMode = register(new Setting<>("TowerMode", false));
    
    // State variables
    private final TimeUtils timer = new TimeUtils();
    private float[] targetRotation = null;
    private int originalSlot = -1;
    private boolean wasHoldingKey = false;
    private BlockPos lastPlacedPos = null;
    private long lastPlaceTime = 0;
    
    // Block placement queue for smoother operation
    private final List<PlacementData> placementQueue = new ArrayList<>();
    
    private static class PlacementData {
        public final BlockPos pos;
        public final EnumFacing side;
        public final Vec3 hitVec;
        public final double distance;
        
        public PlacementData(BlockPos pos, EnumFacing side, Vec3 hitVec, double distance) {
            this.pos = pos;
            this.side = side;
            this.hitVec = hitVec;
            this.distance = distance;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
        restoreOriginalSlot();
    }

    private void reset() {
        targetRotation = null;
        originalSlot = -1;
        wasHoldingKey = false;
        lastPlacedPos = null;
        lastPlaceTime = 0;
        placementQueue.clear();
        timer.reset();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!canOperate()) {
            return;
        }

        // Check if we should activate based on mode
        if (!shouldActivate()) {
            return;
        }

        // Handle rotations
        if (targetRotation != null && rotations.getValue()) {
            updateRotations();
        }

        // Check delay
        if (System.currentTimeMillis() - lastPlaceTime < delay.getValue()) {
            return;
        }

        // Try to place blocks
        attemptBlockPlacement();
    }

    private boolean canOperate() {
        return mc.thePlayer != null && 
               mc.theWorld != null && 
               !mc.thePlayer.capabilities.isFlying &&
               !mc.thePlayer.capabilities.isCreativeMode;
    }

    private boolean shouldActivate() {
        boolean holdKeyPressed = holdMode.getValue() && Keyboard.isKeyDown(getKeybind());
        boolean modeActive = !holdMode.getValue() || holdKeyPressed;
        
        // Update hold key state
        if (holdMode.getValue()) {
            if (holdKeyPressed && !wasHoldingKey) {
                originalSlot = mc.thePlayer.inventory.currentItem;
            } else if (!holdKeyPressed && wasHoldingKey) {
                restoreOriginalSlot();
            }
            wasHoldingKey = holdKeyPressed;
        }

        if (!modeActive) {
            return false;
        }

        // Check fall distance
        if (onlyWhenFalling.getValue() && mc.thePlayer.fallDistance < fallDistance.getValue()) {
            return false;
        }

        // Check if over void
        if (onlyOnVoid.getValue() && !isOverVoid()) {
            return false;
        }

        return true;
    }

    private boolean isOverVoid() {
        BlockPos playerPos = new BlockPos(mc.thePlayer);
        for (int y = playerPos.getY(); y >= 0; y--) {
            Block block = getBlock(new BlockPos(playerPos.getX(), y, playerPos.getZ()));
            if (!isReplaceable(block)) {
                return false;
            }
        }
        return true;
    }

    private void attemptBlockPlacement() {
        if (!hasBlocks()) {
            if (autoSwitch.getValue()) {
                switchToBlocks();
            } else {
                return;
            }
        }

        BlockPos targetPos = getTargetPosition();
        if (targetPos == null) {
            return;
        }

        // Find best placement option
        PlacementData bestPlacement = findBestPlacement(targetPos);
        if (bestPlacement == null) {
            return;
        }

        // Perform the placement
        performBlockPlacement(bestPlacement);
    }

    private BlockPos getTargetPosition() {
        BlockPos playerPos = new BlockPos(mc.thePlayer);
        
        if (towerMode.getValue() && mc.thePlayer.motionY > 0) {
            // Tower mode: place at current position when moving up
            return playerPos;
        }
        
        // Find best position beneath player
        BlockPos targetPos = playerPos.down();
        
        // If keep Y is enabled, maintain the same Y level
        if (keepY.getValue() && lastPlacedPos != null) {
            targetPos = new BlockPos(playerPos.getX(), lastPlacedPos.getY(), playerPos.getZ());
        }
        
        // Check if position needs a block
        if (!isReplaceable(getBlock(targetPos))) {
            return null;
        }
        
        return targetPos;
    }

    private PlacementData findBestPlacement(BlockPos targetPos) {
        placementQueue.clear();
        
        Vec3 eyePos = getEyePosition();
        double maxRange = range.getValue();
        
        // Check all possible sides for placement
        for (EnumFacing side : EnumFacing.values()) {
            if (side == EnumFacing.UP) continue; // Don't place on top
            
            // Skip down face unless jumping
            if (side == EnumFacing.DOWN && !isJumping()) continue;
            
            BlockPos neighborPos = targetPos.offset(side);
            Block neighborBlock = getBlock(neighborPos);
            
            // Check if neighbor can support placement
            if (!canPlaceAgainst(neighborBlock, neighborPos)) continue;
            
            EnumFacing oppositeSide = side.getOpposite();
            Vec3 hitVec = calculateHitVec(neighborPos, oppositeSide);
            
            // Check distance
            double distance = eyePos.squareDistanceTo(hitVec);
            if (distance > maxRange * maxRange) continue;
            
            // Check ray casting if enabled
            if (rayCast.getValue() && !canSeeBlock(neighborPos, hitVec)) continue;
            
            placementQueue.add(new PlacementData(neighborPos, oppositeSide, hitVec, distance));
        }
        
        if (placementQueue.isEmpty()) {
            return null;
        }
        
        // Sort by distance (closest first)
        Collections.sort(placementQueue, Comparator.comparing(p -> p.distance));
        
        return placementQueue.get(0);
    }

    private void performBlockPlacement(PlacementData placement) {
        // Set target rotation
        if (rotations.getValue()) {
            targetRotation = calculateRotations(placement.hitVec);
        }
        
        // Place the block
        boolean success = mc.playerController.onPlayerRightClick(
            mc.thePlayer, 
            mc.theWorld, 
            mc.thePlayer.getCurrentEquippedItem(), 
            placement.pos, 
            placement.side, 
            placement.hitVec
        );
        
        if (success) {
            if (swingArm.getValue()) {
                mc.thePlayer.swingItem();
            }
            
            lastPlacedPos = placement.pos.offset(placement.side.getOpposite());
            lastPlaceTime = System.currentTimeMillis();
        }
    }

    private boolean hasBlocks() {
        ItemStack currentItem = mc.thePlayer.getCurrentEquippedItem();
        return currentItem != null && 
               currentItem.getItem() instanceof ItemBlock && 
               currentItem.stackSize > 0 &&
               (blocksToSave.getValue() == 0 || currentItem.stackSize > blocksToSave.getValue());
    }

    private void switchToBlocks() {
        if (originalSlot == -1 && !holdMode.getValue()) {
            originalSlot = mc.thePlayer.inventory.currentItem;
        }
        
        int blockSlot = findBlockSlot();
        if (blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = blockSlot;
        }
    }

    private int findBlockSlot() {
        int bestSlot = -1;
        int maxStack = 0;
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                // Prefer slots with more blocks, but respect blocks to save setting
                int availableBlocks = stack.stackSize - blocksToSave.getValue();
                if (availableBlocks > 0 && availableBlocks > maxStack) {
                    maxStack = availableBlocks;
                    bestSlot = i;
                }
            }
        }
        
        return bestSlot;
    }

    private void restoreOriginalSlot() {
        if (originalSlot != -1 && originalSlot != mc.thePlayer.inventory.currentItem) {
            mc.thePlayer.inventory.currentItem = originalSlot;
            originalSlot = -1;
        }
    }

    private void updateRotations() {
        if (targetRotation == null) return;
        
        float[] currentRotation = {mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
        float speed = rotationSpeed.getValue().floatValue();
        
        float[] smoothRotation = RotationUtils.smoothRotation(currentRotation, targetRotation, speed);
        
        mc.thePlayer.rotationYaw = smoothRotation[0];
        mc.thePlayer.rotationPitch = smoothRotation[1];
        
        // Clear rotation when close enough
        if (Math.abs(smoothRotation[0] - targetRotation[0]) < 2.0f && 
            Math.abs(smoothRotation[1] - targetRotation[1]) < 2.0f) {
            targetRotation = null;
        }
    }

    private Vec3 getEyePosition() {
        return new Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
            mc.thePlayer.posZ
        );
    }

    private Vec3 calculateHitVec(BlockPos pos, EnumFacing side) {
        Vec3 base = new Vec3(pos).addVector(0.5, 0.5, 0.5);
        Vec3 sideVec = new Vec3(side.getDirectionVec()).scale(0.5);
        return base.add(sideVec);
    }

    private float[] calculateRotations(Vec3 target) {
        Vec3 eyePos = getEyePosition();
        double deltaX = target.xCoord - eyePos.xCoord;
        double deltaY = target.yCoord - eyePos.yCoord;
        double deltaZ = target.zCoord - eyePos.zCoord;
        
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(deltaY, horizontalDistance) * 180.0 / Math.PI);
        
        return new float[]{yaw, pitch};
    }

    private boolean canPlaceAgainst(Block block, BlockPos pos) {
        return block != null && 
               !isReplaceable(block) && 
               block.canCollideCheck(mc.theWorld.getBlockState(pos), false);
    }

    private boolean isReplaceable(Block block) {
        if (block == null) return true;
        
        Material material = block.getMaterial();
        if (material.isReplaceable()) return true;
        
        // Special case for snow
        if (block instanceof BlockSnow) {
            return block.getBlockBoundsMaxY() <= 0.125;
        }
        
        return false;
    }

    private boolean canSeeBlock(BlockPos pos, Vec3 hitVec) {
        Vec3 eyePos = getEyePosition();
        MovingObjectPosition raycast = mc.theWorld.rayTraceBlocks(eyePos, hitVec, false, true, false);
        
        return raycast == null || 
               (raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && 
                raycast.getBlockPos().equals(pos));
    }

    private boolean isJumping() {
        return Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
    }

    private static Block getBlock(BlockPos pos) {
        return Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock();
    }

    // Utility methods for external access
    public boolean isPlacing() {
        return System.currentTimeMillis() - lastPlaceTime < 100;
    }

    public BlockPos getLastPlacedPos() {
        return lastPlacedPos;
    }

    public int getBlocksInInventory() {
        int totalBlocks = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                totalBlocks += Math.max(0, stack.stackSize - blocksToSave.getValue());
            }
        }
        return totalBlocks;
    }
}
