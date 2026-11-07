package com.benbenlaw.minime.entity;

import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.item.MMDataComponents;
import com.benbenlaw.minime.item.MMItems;
import com.benbenlaw.minime.entity.goals.*;
import com.benbenlaw.minime.screen.MiniMeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MiniMeEntity extends PathfinderMob implements HasCustomInventoryScreen {
    public static final int HEAD_SLOT = 0;
    public static final int CHEST_SLOT = 1;
    public static final int LEGS_SLOT = 2;
    public static final int BOOTS_SLOT = 3;
    public static final int MAIN_HAND = 4;
    public static final int OFF_HAND = 5;
    public static final int STORAGE_SLOT_COUNT = 15;
    public static final int END_STORAGE_SLOT = 6 + STORAGE_SLOT_COUNT;
    public static final int INSTRUCTION_SLOT = END_STORAGE_SLOT;
    public static final int CONTAINER_SIZE = INSTRUCTION_SLOT + 1;
    private static final double CARRY_RANGE = 48.0D;
    private static final int CARRY_SLOTS = 4;
    private static final double STACK_HEIGHT = 0.5D;
    private static final double SHOULDER_SIDE = 0.35D;
    private static final double SHOULDER_HEIGHT = 1.1D;
    public static final int FIRST_STORAGE_SLOT = 6;
    private static final int MAX_REPLANT_SPOTS = 16;
    private static final int TASK_PRIORITY_BASE = 2;
    private static final int CHEST_SCAN_INTERVAL = 40;
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_SKIN_NAME_LENGTH = 16;

    private static final EntityDataAccessor<ResolvableProfile> DATA_PROFILE =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.RESOLVABLE_PROFILE);

    private static final EntityDataAccessor<String> DATA_TASKS =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.STRING);

    private static final Map<MiniMeSetting, EntityDataAccessor<Integer>> SETTING_DATA = new EnumMap<>(MiniMeSetting.class);

    static {
        for (MiniMeSetting setting : MiniMeSetting.values()) {
            SETTING_DATA.put(setting, SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.INT));
        }
    }

    private static final EntityDataAccessor<Optional<BlockPos>> DATA_HOME_POS =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    private static final EntityDataAccessor<Integer> DATA_HOME_RADIUS =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<ItemStack> DATA_INSTRUCTION =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final EntityDataAccessor<Integer> DATA_CARRY_SLOT =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<String> DATA_CARRIER =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> DATA_SITTING =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_SHOW_AREA =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.BOOLEAN);

    private final SimpleContainer inventory = new SimpleContainer(CONTAINER_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            MiniMeEntity.this.syncEquipmentFromInventory();
        }
    };

    private Map<MiniMeTask, Goal> taskGoals;
    private String cachedTasksRaw;
    private List<TaskSetting> cachedTasks;

    private UUID ownerUuid;
    private UUID pendingCarry;
    private int itemsMinedSinceDump;
    private BlockPos cachedChestPos;
    private long chestScanTime = -1000;

    private final List<ReplantSpot> replantSpots = new ArrayList<>();

    private ItemStack factoryCarry = ItemStack.EMPTY;

    public MiniMeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true);
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new CarryGoal(this));
        goalSelector.addGoal(0, new SitGoal(this));
        goalSelector.addGoal(1, new FloatGoal(this));

        taskGoals = new EnumMap<>(MiniMeTask.class);
        taskGoals.put(MiniMeTask.COMBAT, new SwordAttackGoal(this, 1.0D, true));
        taskGoals.put(MiniMeTask.CHOP_TREES, new ChopTreeGoal(this));
        taskGoals.put(MiniMeTask.REPLANT, new ReplantSaplingGoal(this));
        taskGoals.put(MiniMeTask.SHEAR, new ShearMobsGoal(this));
        taskGoals.put(MiniMeTask.MINE, new MineQuarryGoal(this));
        taskGoals.put(MiniMeTask.COLLECT, new CollectLootGoal(this));
        taskGoals.put(MiniMeTask.DUMP, new DumpInventoryGoal(this));
        taskGoals.put(MiniMeTask.FACTORY, new FactoryGoal(this));

        List<TaskSetting> settings = getTaskSettings();
        for (int i = 0; i < settings.size(); i++) {
            goalSelector.addGoal(TASK_PRIORITY_BASE + i, taskGoals.get(settings.get(i).task()));
        }

        int after = TASK_PRIORITY_BASE + MiniMeTask.values().length;
        goalSelector.addGoal(after, new ReturnHomeGoal(this));
        goalSelector.addGoal(after + 1, new RandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(after + 2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(after + 3, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true) {
            @Override
            public boolean canUse() {
                return isTaskActive(MiniMeTask.COMBAT) && super.canUse();
            }
        });
    }

    private static String defaultTasks() {
        List<TaskSetting> settings = new ArrayList<>();
        for (MiniMeTask task : MiniMeTask.values()) settings.add(new TaskSetting(task, true));
        return encodeTasks(settings);
    }

    public record TaskSetting(MiniMeTask task, boolean enabled) {
    }

    public List<TaskSetting> getTaskSettings() {
        String raw = this.entityData.get(DATA_TASKS);
        if (!raw.equals(cachedTasksRaw)) {
            cachedTasks = parseTasks(raw);
            cachedTasksRaw = raw;
        }
        return cachedTasks;
    }

    public boolean isTaskEnabled(MiniMeTask task) {
        for (TaskSetting setting : getTaskSettings()) {
            if (setting.task() == task) return setting.enabled();
        }
        return false;
    }

    public void toggleTask(int index) {
        List<TaskSetting> settings = new ArrayList<>(getTaskSettings());
        if (index < 0 || index >= settings.size()) return;

        TaskSetting old = settings.get(index);
        settings.set(index, new TaskSetting(old.task(), !old.enabled()));
        applyTaskSettings(settings);
    }

    public static List<MiniMeTask> tasksOf(ItemStack stack) {
        if (!stack.is(MMItems.INSTRUCTIONS.get())) return List.of();

        return stack.getOrDefault(MMDataComponents.INSTRUCTION_TASKS.get(), List.of());
    }

    public boolean isTaskApplicable(MiniMeTask task) {
        return tasksOf(getInstructionItem()).contains(task);
    }

    public boolean isSitting() {
        return this.entityData.get(DATA_SITTING);
    }

    public void setSitting(boolean sitting) {
        this.entityData.set(DATA_SITTING, sitting);
    }

    public boolean isCarried() {
        return !this.entityData.get(DATA_CARRIER).isEmpty();
    }

    public boolean isCarriedBy(Player player) {
        return player != null && player.getUUID().toString().equals(this.entityData.get(DATA_CARRIER));
    }

    @Nullable
    public Player getCarrier() {
        String stored = this.entityData.get(DATA_CARRIER);
        if (stored.isEmpty()) return null;

        try {
            return this.level().getPlayerByUUID(UUID.fromString(stored));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public Player getPendingCarryPlayer() {
        return pendingCarry == null ? null : this.level().getPlayerByUUID(pendingCarry);
    }

    public void cancelCarry() {
        pendingCarry = null;
    }

    public int getCarrySlot() {
        return this.entityData.get(DATA_CARRY_SLOT);
    }

    private int freeCarrySlot(Player player) {
        boolean[] used = new boolean[CARRY_SLOTS];
        for (MiniMeEntity other : this.level().getEntitiesOfClass(MiniMeEntity.class, player.getBoundingBox().inflate(8.0D),
                entity -> entity != this && entity.isCarriedBy(player))) {
            int slot = other.getCarrySlot();
            if (slot >= 0 && slot < CARRY_SLOTS) used[slot] = true;
        }
        for (int i = 0; i < CARRY_SLOTS; i++) {
            if (!used[i]) return i;
        }
        return -1;
    }

    public void completeCarry(Player player) {
        pendingCarry = null;
        int slot = freeCarrySlot(player);
        if (slot < 0) return;

        this.entityData.set(DATA_CARRY_SLOT, slot);
        this.entityData.set(DATA_CARRIER, player.getUUID().toString());
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();
    }

    private void putDown(@Nullable Player carrier) {
        this.entityData.set(DATA_CARRIER, "");
        this.noPhysics = false;
        this.setNoGravity(false);
        if (carrier != null) {
            double yaw = Math.toRadians(carrier.yBodyRot);
            this.setPos(carrier.getX() - Math.cos(yaw) * 0.8, carrier.getY(), carrier.getZ() - Math.sin(yaw) * 0.8);
        }
        this.setDeltaMovement(Vec3.ZERO);
    }

    public void toggleSit(ServerPlayer player) {
        if (isCarried()) {
            player.sendSystemMessage(Component.literal("Put the Mini Me down before telling it to sit."), true);
            return;
        }
        setSitting(!isSitting());
        player.sendSystemMessage(Component.literal(isSitting() ? "The Mini Me sits down." : "The Mini Me stands up."), true);
    }

    public void toggleCarry(ServerPlayer player) {
        if (isCarriedBy(player)) {
            putDown(player);
            player.sendSystemMessage(Component.literal("You put the Mini Me down."), true);
            return;
        }
        if (isCarried()) return;

        if (pendingCarry != null) {
            pendingCarry = null;
            player.sendSystemMessage(Component.literal("The Mini Me stops coming over."), true);
            return;
        }
        if (distanceTo(player) > CARRY_RANGE) {
            player.sendSystemMessage(Component.literal("The Mini Me is too far away to come to you."), true);
            return;
        }

        if (freeCarrySlot(player) < 0) {
            player.sendSystemMessage(Component.literal("Your shoulders are full. You can carry four Mini Mes."), true);
            return;
        }

        setSitting(false);
        pendingCarry = player.getUUID();
        player.sendSystemMessage(Component.literal("The Mini Me runs over to you."), true);
    }

    public Vec3 shoulderPosition(Player carrier, float partialTick) {
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, carrier.yBodyRotO, carrier.yBodyRot));
        int slot = getCarrySlot();
        double side = slot % 2 == 0 ? SHOULDER_SIDE : -SHOULDER_SIDE;
        double height = SHOULDER_HEIGHT + (slot / 2) * STACK_HEIGHT - (carrier.isCrouching() ? 0.25D : 0.0D);
        return new Vec3(Mth.lerp(partialTick, carrier.xo, carrier.getX()) - Math.cos(yaw) * side,
                Mth.lerp(partialTick, carrier.yo, carrier.getY()) + height,
                Mth.lerp(partialTick, carrier.zo, carrier.getZ()) - Math.sin(yaw) * side);
    }

    @Override
    public boolean isPushable() {
        return !isCarried() && super.isPushable();
    }

    @Override
    public void push(Entity entity) {
        if (!isCarried()) super.push(entity);
    }

    @Override
    protected void doPush(Entity entity) {
        if (!isCarried()) super.doPush(entity);
    }

    @Override
    public void tick() {
        super.tick();

        if (!isCarried()) {
            this.noPhysics = false;
            return;
        }

        this.noPhysics = true;
        Player carrier = getCarrier();
        if (carrier == null || !carrier.isAlive()) {
            if (!this.level().isClientSide()) putDown(null);
            return;
        }

        Vec3 spot = shoulderPosition(carrier, 1.0F);
        this.setPos(spot.x, spot.y, spot.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0D;
        this.yBodyRotO = this.yBodyRot;
        this.yBodyRot = carrier.yBodyRot;
        this.setYRot(carrier.getYRot());
        this.yHeadRotO = this.yHeadRot;
        this.yHeadRot = carrier.yHeadRot;
    }

    public ItemStack getFactoryCarry() {
        return factoryCarry;
    }

    public void setFactoryCarry(ItemStack stack) {
        this.factoryCarry = stack;
    }

    public ItemStack getInstructionItem() {
        return this.entityData.get(DATA_INSTRUCTION);
    }

    public boolean isTaskActive(MiniMeTask task) {
        return isTaskApplicable(task) && isTaskEnabled(task);
    }

    public void moveTask(int index, int direction) {
        List<TaskSetting> settings = new ArrayList<>(getTaskSettings());
        if (index < 0 || index >= settings.size() || direction == 0) return;

        int step = direction > 0 ? 1 : -1;
        int target = index + step;
        while (target >= 0 && target < settings.size() && !isTaskApplicable(settings.get(target).task())) {
            target += step;
        }
        if (target < 0 || target >= settings.size()) return;

        TaskSetting moved = settings.remove(index);
        settings.add(target, moved);
        applyTaskSettings(settings);
    }

    private void applyTaskSettings(List<TaskSetting> settings) {
        this.entityData.set(DATA_TASKS, encodeTasks(settings));
        rebuildTaskGoals();
    }

    private void rebuildTaskGoals() {
        if (taskGoals == null) return;

        List<TaskSetting> settings = getTaskSettings();
        for (int i = 0; i < settings.size(); i++) {
            Goal goal = taskGoals.get(settings.get(i).task());
            goalSelector.removeGoal(goal);
            goalSelector.addGoal(TASK_PRIORITY_BASE + i, goal);
        }
    }

    private static String encodeTasks(List<TaskSetting> settings) {
        StringBuilder builder = new StringBuilder();
        for (TaskSetting setting : settings) {
            if (!builder.isEmpty()) builder.append(',');
            builder.append(setting.task().name()).append(':').append(setting.enabled() ? '1' : '0');
        }
        return builder.toString();
    }

    private static List<TaskSetting> parseTasks(String raw) {
        List<TaskSetting> settings = new ArrayList<>();
        Set<MiniMeTask> seen = EnumSet.noneOf(MiniMeTask.class);

        for (String part : raw.split(",")) {
            String[] pieces = part.split(":");
            if (pieces.length != 2) continue;
            try {
                MiniMeTask task = MiniMeTask.valueOf(pieces[0]);
                if (seen.add(task)) settings.add(new TaskSetting(task, pieces[1].equals("1")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        for (MiniMeTask task : MiniMeTask.values()) {
            if (seen.add(task)) settings.add(new TaskSetting(task, true));
        }
        return settings;
    }

    public boolean isShowingArea() {
        return this.entityData.get(DATA_SHOW_AREA);
    }

    public void setShowingArea(boolean showing) {
        this.entityData.set(DATA_SHOW_AREA, showing);
    }

    public record ReplantSpot(BlockPos pos, @Nullable Item preferred) {
    }

    public List<ReplantSpot> getReplantSpots() {
        return replantSpots;
    }

    public void queueReplant(BlockPos pos, @Nullable Item preferred) {
        if (replantSpots.size() >= MAX_REPLANT_SPOTS) return;
        for (ReplantSpot spot : replantSpots) {
            if (spot.pos().equals(pos)) return;
        }
        replantSpots.add(new ReplantSpot(pos.immutable(), preferred));
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        int before = stack.getCount();

        ItemStack remainder = insertIntoStorage(stack);
        if (remainder.getCount() == before) return;

        this.playSound(SoundEvents.ITEM_PICKUP);
        if (remainder.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(remainder);
        }
    }

    public ItemStack insertIntoStorage(ItemStack stack) {
        ItemStack remaining = stack.copy();

        for (int i = FIRST_STORAGE_SLOT; i < END_STORAGE_SLOT && !remaining.isEmpty(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
                int move = Math.min(slot.getMaxStackSize() - slot.getCount(), remaining.getCount());
                if (move > 0) {
                    slot.grow(move);
                    remaining.shrink(move);
                }
            }
        }

        for (int i = FIRST_STORAGE_SLOT; i < END_STORAGE_SLOT && !remaining.isEmpty(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                int move = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                inventory.setItem(i, remaining.copyWithCount(move));
                remaining.shrink(move);
            }
        }

        inventory.setChanged();
        return remaining;
    }

    public boolean hasStoredItems() {
        for (int i = FIRST_STORAGE_SLOT; i < END_STORAGE_SLOT; i++) {
            if (!inventory.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    public boolean hasStorageSpace() {
        for (int i = FIRST_STORAGE_SLOT; i < END_STORAGE_SLOT; i++) {
            if (inventory.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    public void noteItemsMined(int count) {
        itemsMinedSinceDump += count;
    }

    public void resetMinedCount() {
        itemsMinedSinceDump = 0;
    }

    public boolean shouldDumpMinedItems() {
        int after = getSetting(MiniMeSetting.DUMP_AFTER);
        if (after <= 0 || itemsMinedSinceDump < after) return false;

        if (!hasStoredItems()) {
            itemsMinedSinceDump = 0;
            return false;
        }

        return isTaskActive(MiniMeTask.DUMP) && MiniMeConfig.DUMP_TO_CHEST.get() && findChestNearHome() != null;
    }

    public int getSettingMin(MiniMeSetting setting) {
        BlockPos home = getHomePos();
        if (home != null && (setting == MiniMeSetting.MIN_DEPTH || setting == MiniMeSetting.MAX_DEPTH)) {
            return home.getY() - level().getMaxY();
        }
        return setting.min();
    }

    public int getSettingMax(MiniMeSetting setting) {
        BlockPos home = getHomePos();
        if (home != null && (setting == MiniMeSetting.MIN_DEPTH || setting == MiniMeSetting.MAX_DEPTH)) {
            return home.getY() - level().getMinY();
        }
        return setting.max();
    }

    public int clampSetting(MiniMeSetting setting, int value) {
        return Math.max(getSettingMin(setting), Math.min(value, getSettingMax(setting)));
    }

    public int getSetting(MiniMeSetting setting) {
        return this.entityData.get(SETTING_DATA.get(setting));
    }

    private void setSetting(MiniMeSetting setting, int value) {
        this.entityData.set(SETTING_DATA.get(setting), clampSetting(setting, value));
    }

    public void setSettingValue(MiniMeSetting setting, int requested) {
        int value = clampSetting(setting, requested);
        setSetting(setting, value);

        if (setting == MiniMeSetting.MIN_DEPTH && value > getSetting(MiniMeSetting.MAX_DEPTH)) {
            setSetting(MiniMeSetting.MAX_DEPTH, value);
        }
        if (setting == MiniMeSetting.MAX_DEPTH && value < getSetting(MiniMeSetting.MIN_DEPTH)) {
            setSetting(MiniMeSetting.MIN_DEPTH, value);
        }
    }

    public record Footprint(int minX, int minZ, int sizeX, int sizeZ) {
        public int maxXExclusive() {
            return minX + sizeX;
        }

        public int maxZExclusive() {
            return minZ + sizeZ;
        }

        public int centerX() {
            return minX + sizeX / 2;
        }

        public int centerZ() {
            return minZ + sizeZ / 2;
        }

        public boolean contains(int x, int z) {
            return x >= minX && x < maxXExclusive() && z >= minZ && z < maxZExclusive();
        }
    }

    @Nullable
    public Footprint getQuarryFootprint() {
        return footprint(MiniMeSetting.QUARRY_SIZE_X, MiniMeSetting.QUARRY_SIZE_Z,
                MiniMeSetting.QUARRY_OFFSET_X, MiniMeSetting.QUARRY_OFFSET_Z);
    }

    @Nullable
    public Footprint getChestFootprint() {
        return footprint(MiniMeSetting.CHEST_SIZE_X, MiniMeSetting.CHEST_SIZE_Z,
                MiniMeSetting.CHEST_OFFSET_X, MiniMeSetting.CHEST_OFFSET_Z);
    }

    @Nullable
    private Footprint footprint(MiniMeSetting sizeX, MiniMeSetting sizeZ, MiniMeSetting offsetX, MiniMeSetting offsetZ) {
        if (getHomePos() == null) return null;

        int width = getSetting(sizeX);
        int length = getSetting(sizeZ);
        return new Footprint(
                getHomePos().getX() + getSetting(offsetX) - (width - 1) / 2,
                getHomePos().getZ() + getSetting(offsetZ) - (length - 1) / 2,
                width, length);
    }

    public int getQuarryTopY() {
        return getHomePos().getY() - getSetting(MiniMeSetting.MIN_DEPTH);
    }

    public int getQuarryBottomY() {
        return getHomePos().getY() - getSetting(MiniMeSetting.MAX_DEPTH);
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canPickUpLoot() {
        return MiniMeConfig.COLLECT_ITEMS.get();
    }

    public void setHome(@Nullable BlockPos pos, int radius) {
        this.entityData.set(DATA_HOME_POS, Optional.ofNullable(pos));
        this.entityData.set(DATA_HOME_RADIUS, radius);
    }

    @Nullable
    public BlockPos getHomePos() {
        return this.entityData.get(DATA_HOME_POS).orElse(null);
    }

    public int getHomeRadius() {
        return this.entityData.get(DATA_HOME_RADIUS);
    }

    public boolean isWithinHomeDistance(BlockPos pos) {
        return getHomePos() != null && pos.distSqr(getHomePos()) <= getHomeRadius() * getHomeRadius();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROFILE, ResolvableProfile.createUnresolved(Util.NIL_UUID));
        builder.define(DATA_SITTING, false);
        builder.define(DATA_CARRIER, "");
        builder.define(DATA_CARRY_SLOT, 0);
        builder.define(DATA_SHOW_AREA, false);
        builder.define(DATA_INSTRUCTION, ItemStack.EMPTY);
        builder.define(DATA_HOME_POS, Optional.empty());
        builder.define(DATA_HOME_RADIUS, MiniMeConfig.DEFAULT_HOME_RADIUS.get());
        builder.define(DATA_TASKS, defaultTasks());
        for (MiniMeSetting setting : MiniMeSetting.values()) {
            builder.define(SETTING_DATA.get(setting), setting.defaultValue());
        }
    }

    @Override
    protected void dropAllDeathLoot(@NotNull ServerLevel level, @NotNull DamageSource source) {
        Containers.dropContents(level, this.blockPosition(), this.inventory);
        Containers.dropItemStack(level, getX(), getY(), getZ(), this.factoryCarry);
        this.factoryCarry = ItemStack.EMPTY;
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUuid = uuid;
        setSkin("");
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUuid;
    }

    public void setSkin(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            this.entityData.set(DATA_PROFILE, ResolvableProfile.createUnresolved(ownerUuid == null ? Util.NIL_UUID : ownerUuid));
        } else {
            this.entityData.set(DATA_PROFILE, ResolvableProfile.createUnresolved(trimmed));
        }
    }

    public String getSkinName() {
        return getProfile().name().orElse("");
    }

    public void rename(String name) {
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) trimmed = trimmed.substring(0, MAX_NAME_LENGTH);
        this.setCustomName(trimmed.isEmpty() ? null : Component.literal(trimmed));
    }

    public ResolvableProfile getProfile() {
        return this.entityData.get(DATA_PROFILE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!this.level().isClientSide()) {
            if (player.isShiftKeyDown()) {
                this.setHome(this.blockPosition(), MiniMeConfig.DEFAULT_HOME_RADIUS.get());
                player.sendOverlayMessage(Component.literal("MiniMe home set!"));
                this.playSound(SoundEvents.LEVER_CLICK, 1.0F, 1.0F);
                return InteractionResult.CONSUME;
            }

            openCustomInventoryScreen(player);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void openCustomInventoryScreen(@NotNull Player player) {
        if (!this.level().isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            if (player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }

            serverPlayer.openMenu(new SimpleMenuProvider((containerId, playerInventory, p) ->
                    new MiniMeMenu(containerId, playerInventory, this.inventory, this), this.getDisplayName()), buf -> {
                buf.writeUUID(getUUID());
            });
        }
    }

    @Nullable
    public BlockEntity findChestNearHome() {
        if (getHomePos() == null) return null;

        long now = level().getGameTime();
        boolean stale = now - chestScanTime >= CHEST_SCAN_INTERVAL
                || (cachedChestPos != null && !(level().getBlockState(cachedChestPos).getBlock() instanceof ChestBlock));
        if (stale) {
            chestScanTime = now;
            cachedChestPos = scanForChest();
        }
        return cachedChestPos == null ? null : level().getBlockEntity(cachedChestPos);
    }

    @Nullable
    private BlockPos scanForChest() {
        Footprint area = getChestFootprint();
        boolean quarrying = isTaskActive(MiniMeTask.MINE);
        Footprint quarry = quarrying ? getQuarryFootprint() : null;

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = area.minX(); x < area.maxXExclusive(); x++) {
            for (int z = area.minZ(); z < area.maxZExclusive(); z++) {
                if (quarry != null && quarry.contains(x, z)) continue;

                for (int y = getHomePos().getY() - 4; y <= getHomePos().getY() + 4; y++) {
                    pos.set(x, y, z);
                    if (!(level().getBlockState(pos).getBlock() instanceof ChestBlock)) continue;

                    double distance = distanceToSqr(x + 0.5, y, z + 0.5);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    public void dumpInventoryToChest() {
        BlockEntity chestEntity = findChestNearHome();
        if (!(chestEntity instanceof ChestBlockEntity chest)) return;

        boolean hasItems = false;

        for (int i = FIRST_STORAGE_SLOT; i < END_STORAGE_SLOT; i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                hasItems = true;
                for (int j = 0; j < chest.getContainerSize(); j++) {
                    ItemStack chestStack = chest.getItem(j);

                    if (chestStack.isEmpty()) {
                        chest.setItem(j, stack.copy());
                        this.inventory.setItem(i, ItemStack.EMPTY);
                        break;
                    } else if (ItemStack.isSameItemSameComponents(chestStack, stack)) {
                        int availableSpace = chestStack.getMaxStackSize() - chestStack.getCount();
                        if (availableSpace > 0) {
                            int toMove = Math.min(availableSpace, stack.getCount());
                            chestStack.grow(toMove);
                            stack.shrink(toMove);

                            if (stack.isEmpty()) {
                                this.inventory.setItem(i, ItemStack.EMPTY);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (hasItems) {
            chest.setChanged();
            this.inventory.setChanged();
        }
    }

    public boolean setChestOpen(BlockPos pos, boolean open) {
        BlockState state = level().getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return false;

        level().blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            level().blockEvent(pos.relative(ChestBlock.getConnectedDirection(state)), state.getBlock(), 1, open ? 1 : 0);
        }

        level().playSound(null, pos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS,
                0.5F, level().getRandom().nextFloat() * 0.1F + 0.9F);
        return true;
    }

    private void syncEquipmentFromInventory() {
        if (this.level().isClientSide()) return;

        this.setItemSlot(EquipmentSlot.HEAD, inventory.getItem(HEAD_SLOT));
        this.setItemSlot(EquipmentSlot.CHEST, inventory.getItem(CHEST_SLOT));
        this.setItemSlot(EquipmentSlot.LEGS, inventory.getItem(LEGS_SLOT));
        this.setItemSlot(EquipmentSlot.FEET, inventory.getItem(BOOTS_SLOT));
        this.setItemSlot(EquipmentSlot.MAINHAND, inventory.getItem(MAIN_HAND));
        this.setItemSlot(EquipmentSlot.OFFHAND, inventory.getItem(OFF_HAND));
        this.entityData.set(DATA_INSTRUCTION, inventory.getItem(INSTRUCTION_SLOT).copy());
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);

        ContainerHelper.saveAllItems(output, this.inventory.getItems());

        if (getHomePos() != null) {
            output.store("HomePos", BlockPos.CODEC, getHomePos());
            output.putInt("HomeRadius", getHomeRadius());
        }

        output.store("Profile", ResolvableProfile.CODEC, getProfile());
        if (ownerUuid != null) output.store("Owner", UUIDUtil.CODEC, ownerUuid);
        output.store("FactoryCarry", ItemStack.OPTIONAL_CODEC, factoryCarry);
        output.putBoolean("Sitting", isSitting());

        output.putString("Tasks", this.entityData.get(DATA_TASKS));
        for (MiniMeSetting setting : MiniMeSetting.values()) {
            output.putInt(setting.name(), getSetting(setting));
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);

        this.inventory.clearContent();
        ContainerHelper.loadAllItems(input, this.inventory.getItems());
        this.syncEquipmentFromInventory();

        setHome(input.read("HomePos", BlockPos.CODEC).orElse(null),
                input.getIntOr("HomeRadius", MiniMeConfig.DEFAULT_HOME_RADIUS.get()));

        input.read("Profile", ResolvableProfile.CODEC).ifPresent(profile -> this.entityData.set(DATA_PROFILE, profile));
        this.ownerUuid = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.factoryCarry = input.read("FactoryCarry", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        setSitting(input.getBooleanOr("Sitting", false));
        this.setNoGravity(false);
        this.noPhysics = false;


        input.getString("Tasks").ifPresent(tasks -> applyTaskSettings(parseTasks(tasks)));
        for (MiniMeSetting setting : MiniMeSetting.values()) {
            setSetting(setting, input.getIntOr(setting.name(), setting.defaultValue()));
        }
    }
}
