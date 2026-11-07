package com.benbenlaw.minime.block.entity;

import com.benbenlaw.minime.block.MiniMeConstructorBlock;
import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.entity.MMEntities;
import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.UUID;

public class MiniMeConstructorBlockEntity extends BlockEntity {
    private static final float DOOR_SPEED = 0.1F;
    private static final int OPEN_BEFORE_RELEASE = 12;
    private static final int DOOR_CLOSE_DELAY = 15;
    private static final int WALK_OUT_TIMEOUT = 100;
    private static final double WALK_DISTANCE = 1.7D;
    private static final float SYNC_STEP = 0.0005F;
    private static final float SMOOTHING = 0.2F;

    private final SimpleEnergyHandler energy;
    private final EnergyHandler gate;
    private UUID owner;
    private boolean building;
    private boolean ready;
    private int releaseTimer;
    private int closeTimer;
    private UUID walkingOut;
    private int walkTimer;
    private float lastSyncedProgress = -1.0F;

    private float doorProgress;
    private float doorProgressOld;
    private float shownProgress;
    private float shownProgressOld;

    public MiniMeConstructorBlockEntity(BlockPos pos, BlockState state) {
        super(MMBlockEntities.MINI_ME_CONSTRUCTOR.get(), pos, state);
        int capacity = MiniMeConfig.CONSTRUCTOR_ENERGY.get();
        this.energy = new SimpleEnergyHandler(capacity, capacity, 0) {
            @Override
            protected void onEnergyChanged(int previousAmount) {
                setChanged();
            }
        };
        this.gate = new EnergyHandler() {
            @Override
            public long getAmountAsLong() {
                return energy.getAmountAsLong();
            }

            @Override
            public long getCapacityAsLong() {
                return energy.getCapacityAsLong();
            }

            @Override
            public int insert(int amount, TransactionContext transaction) {
                return building ? energy.insert(amount, transaction) : 0;
            }

            @Override
            public int extract(int amount, TransactionContext transaction) {
                return 0;
            }
        };
    }

    public EnergyHandler energyHandler() {
        return gate;
    }

    public boolean isBuilding() {
        return building;
    }

    public boolean isReady() {
        return ready;
    }

    public UUID getOwner() {
        return owner;
    }

    public float getProgress() {
        if (ready) return 1.0F;
        if (!building) return 0.0F;

        long capacity = energy.getCapacityAsLong();
        return capacity == 0 ? 0.0F : (float) energy.getAmountAsLong() / capacity;
    }

    public float getShownProgress(float partialTick) {
        return shownProgressOld + (shownProgress - shownProgressOld) * partialTick;
    }

    public float getDoorOpenProgress(float partialTick) {
        return doorProgressOld + (doorProgress - doorProgressOld) * partialTick;
    }

    public void interact(ServerPlayer player) {
        if (ready) {
            release(player);
            return;
        }
        if (building) {
            player.sendSystemMessage(Component.literal("Building a Mini Me: " + energy.getAmountAsLong() + " / "
                    + energy.getCapacityAsLong() + " FE"), true);
            return;
        }

        float cost = MiniMeConfig.CONSTRUCTOR_HEALTH_COST.get().floatValue();
        if (cost > 0.0F && !player.isCreative()) {
            if (player.getHealth() <= cost && MiniMeConfig.CONSTRUCTOR_WARN_INSTEAD_OF_KILLING.get()) {
                player.sendSystemMessage(Component.literal("You do not have enough health to give a sample."), true);
                return;
            }
            player.hurt(player.level().damageSources().generic(), cost);
            if (!player.isAlive()) return;
        }

        building = true;
        owner = player.getUUID();
        energy.set(0);
        lastSyncedProgress = -1.0F;
        setChanged();
        syncToClients();
        player.sendSystemMessage(Component.literal("Sample taken. Power the constructor to build the Mini Me."), true);
    }

    private BlockPos frontPos() {
        return worldPosition.relative(getBlockState().getValue(MiniMeConstructorBlock.FACING).getOpposite());
    }

    private boolean frontBlocked(ServerLevel serverLevel) {
        BlockPos front = frontPos();
        return !serverLevel.getBlockState(front).getCollisionShape(serverLevel, front).isEmpty()
                || !serverLevel.getBlockState(front.above()).getCollisionShape(serverLevel, front.above()).isEmpty();
    }

    private void release(ServerPlayer player) {
        if (releaseTimer > 0) return;

        if (frontBlocked((ServerLevel) player.level())) {
            player.sendSystemMessage(Component.literal("Something is blocking the doors."), true);
            return;
        }
        releaseTimer = OPEN_BEFORE_RELEASE;
        MiniMeConstructorBlock.setOpen(getBlockState(), player.level(), worldPosition, true);
        setChanged();
    }

    private void syncToClients() {
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void serverTick(ServerLevel serverLevel) {
        if (closeTimer > 0 && --closeTimer == 0) {
            MiniMeConstructorBlock.setOpen(getBlockState(), serverLevel, worldPosition, false);
        }

        if (releaseTimer > 0 && --releaseTimer == 0) {
            spawn(serverLevel);
        }

        if (walkingOut != null) walkOut(serverLevel);

        if (building) {
            float progress = getProgress();
            if (Math.abs(progress - lastSyncedProgress) >= SYNC_STEP) {
                lastSyncedProgress = progress;
                syncToClients();
            }
            if (energy.getAmountAsLong() >= energy.getCapacityAsLong()) {
                building = false;
                ready = true;
                energy.set(0);
                lastSyncedProgress = -1.0F;
                setChanged();
                syncToClients();
                serverLevel.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
        }
    }

    private void spawn(ServerLevel serverLevel) {
        if (frontBlocked(serverLevel)) {
            MiniMeConstructorBlock.setOpen(getBlockState(), serverLevel, worldPosition, false);
            return;
        }

        Direction out = getBlockState().getValue(MiniMeConstructorBlock.FACING).getOpposite();
        BlockPos front = frontPos();
        MiniMeEntity miniMe = MMEntities.MINI_ME.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (miniMe == null) return;

        miniMe.setPos(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
        miniMe.setHome(front, MiniMeConfig.DEFAULT_HOME_RADIUS.get());
        if (owner != null) miniMe.setOwnerUUID(owner);
        float yaw = out.toYRot();
        miniMe.setYRot(yaw);
        miniMe.yBodyRot = yaw;
        miniMe.yHeadRot = yaw;
        serverLevel.addFreshEntity(miniMe);

        ready = false;
        walkingOut = miniMe.getUUID();
        walkTimer = WALK_OUT_TIMEOUT;
        setChanged();
        syncToClients();

        serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void walkOut(ServerLevel serverLevel) {
        Direction out = getBlockState().getValue(MiniMeConstructorBlock.FACING).getOpposite();
        double x = worldPosition.getX() + 0.5 + out.getStepX() * WALK_DISTANCE;
        double z = worldPosition.getZ() + 0.5 + out.getStepZ() * WALK_DISTANCE;
        if (!(serverLevel.getEntity(walkingOut) instanceof MiniMeEntity miniMe) || --walkTimer <= 0) {
            finishWalk();
            return;
        }

        miniMe.getMoveControl().setWantedPosition(x, worldPosition.getY(), z, 1.0D);
        if (miniMe.distanceToSqr(x, miniMe.getY(), z) < 0.3D) finishWalk();
    }

    private void finishWalk() {
        walkingOut = null;
        closeTimer = DOOR_CLOSE_DELAY;
    }

    public void clientTick() {
        doorProgressOld = doorProgress;
        float target = getBlockState().getValue(MiniMeConstructorBlock.OPEN) ? 1.0F : 0.0F;
        if (doorProgress < target) {
            doorProgress = Math.min(target, doorProgress + DOOR_SPEED);
        } else if (doorProgress > target) {
            doorProgress = Math.max(target, doorProgress - DOOR_SPEED);
        }

        shownProgressOld = shownProgress;
        float goal = getProgress();
        shownProgress = Math.abs(goal - shownProgress) < 0.0005F ? goal : shownProgress + (goal - shownProgress) * SMOOTHING;

        if (building && level != null) {
            RandomSource random = level.getRandom();
            double px = worldPosition.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double pz = worldPosition.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double py = worldPosition.getY() + 0.1 + shownProgress * 0.8 + random.nextDouble() * 0.1;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz,
                    (random.nextDouble() - 0.5) * 0.02, random.nextDouble() * 0.04, (random.nextDouble() - 0.5) * 0.02);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        output.putBoolean("building", building);
        output.putBoolean("ready", ready);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input.childOrEmpty("energy"));
        building = input.getBooleanOr("building", false);
        ready = input.getBooleanOr("ready", false);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }
}
