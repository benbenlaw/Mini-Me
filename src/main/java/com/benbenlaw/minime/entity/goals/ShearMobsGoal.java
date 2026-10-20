package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;

import java.util.Comparator;
import java.util.EnumSet;

public class ShearMobsGoal extends Goal {

    private static final int SCAN_INTERVAL = 20;
    private static final int MAX_TICKS = 200;
    private static final double REACH_SQR = 2.0D * 2.0D;

    private final MiniMeEntity miniMe;

    private LivingEntity target;
    private int scanCooldown;
    private int ticks;

    public ShearMobsGoal(MiniMeEntity miniMe) {
        this.miniMe = miniMe;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!MiniMeConfig.SHEARING_ENABLED.get() || !miniMe.isTaskEnabled(MiniMeTask.SHEAR)) return false;
        if (!miniMe.getMainHandItem().is(Tags.Items.TOOLS_SHEAR)) return false;
        if (miniMe.getHomePos() == null) return false;

        if (scanCooldown > 0) {
            scanCooldown--;
            return false;
        }
        scanCooldown = SCAN_INTERVAL;

        target = findTarget();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && isShearable(target)
                && ticks < MAX_TICKS && miniMe.getMainHandItem().is(Tags.Items.TOOLS_SHEAR);
    }

    @Override
    public void start() {
        ticks = 0;
        miniMe.getNavigation().moveTo(target, 1.0D);
    }

    @Override
    public void tick() {
        ticks++;
        miniMe.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (miniMe.distanceToSqr(target) > REACH_SQR) {
            if (!miniMe.getNavigation().isInProgress() || ticks % 10 == 0) {
                miniMe.getNavigation().moveTo(target, 1.0D);
            }
            return;
        }
        miniMe.getNavigation().stop();

        ItemStack shears = miniMe.getMainHandItem();
        ((Shearable) target).shear((ServerLevel) miniMe.level(), SoundSource.NEUTRAL, shears);
        miniMe.swing(InteractionHand.MAIN_HAND);

        if (MiniMeConfig.SHEARING_DAMAGE_TOOLS.get()) {
            // The stack is shared with the inventory, so broken shears empty their slot too.
            shears.hurtAndBreak(1, miniMe, EquipmentSlot.MAINHAND);
            miniMe.getInventory().setChanged();
        }

        target = null;
    }

    @Override
    public void stop() {
        target = null;
        miniMe.getNavigation().stop();
    }

    private LivingEntity findTarget() {
        BlockPosRange range = new BlockPosRange(miniMe);

        return miniMe.level().getEntitiesOfClass(LivingEntity.class, range.box(),
                        entity -> entity != miniMe && entity.isAlive() && isShearable(entity)
                                && miniMe.isWithinHomeDistance(entity.blockPosition()))
                .stream()
                .min(Comparator.comparingDouble(miniMe::distanceToSqr))
                .orElse(null);
    }

    private static boolean isShearable(LivingEntity entity) {
        return entity instanceof Shearable shearable && shearable.readyForShearing();
    }

    private record BlockPosRange(MiniMeEntity miniMe) {
        AABB box() {
            return new AABB(miniMe.getHomePos()).inflate(miniMe.getHomeRadius());
        }
    }
}
