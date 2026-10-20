package com.benbenlaw.minime.entity;

import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.entity.goals.*;
import com.benbenlaw.minime.screen.MiniMeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
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
import java.util.Set;
import java.util.UUID;

public class MiniMeEntity extends PathfinderMob implements HasCustomInventoryScreen {

    public static final int HEAD_SLOT = 0;
    public static final int CHEST_SLOT = 1;
    public static final int LEGS_SLOT = 2;
    public static final int BOOTS_SLOT = 3;
    public static final int MAIN_HAND = 4;
    public static final int OFF_HAND = 5;
    public static final int FIRST_STORAGE_SLOT = 6;
    private static final int MAX_REPLANT_SPOTS = 16;
    private static final int TASK_PRIORITY_BASE = 2;

    private static final EntityDataAccessor<ResolvableProfile> DATA_PROFILE =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.RESOLVABLE_PROFILE);

    private static final EntityDataAccessor<String> DATA_TASKS =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Integer> DATA_QUARRY_WIDTH =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> DATA_SHOW_AREA =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.BOOLEAN);

    private final SimpleContainer inventory = new SimpleContainer(21) {
        @Override
        public void setChanged() {
            super.setChanged();
            MiniMeEntity.this.syncEquipmentFromInventory();
        }
    };

    private Map<MiniMeTask, Goal> taskGoals;
    private String cachedTasksRaw;
    private List<TaskSetting> cachedTasks;

    private BlockPos homePos;
    private int blocksMinedSinceDump;
    private int homeRadius = MiniMeConfig.DEFAULT_HOME_RADIUS.get();

    private final List<ReplantSpot> replantSpots = new ArrayList<>();

    public MiniMeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));

        taskGoals = new EnumMap<>(MiniMeTask.class);
        taskGoals.put(MiniMeTask.COMBAT, new SwordAttackGoal(this, 1.0D, true));
        taskGoals.put(MiniMeTask.CHOP_TREES, new ChopTreeGoal(this));
        taskGoals.put(MiniMeTask.REPLANT, new ReplantSaplingGoal(this));
        taskGoals.put(MiniMeTask.SHEAR, new ShearMobsGoal(this));
        taskGoals.put(MiniMeTask.MINE, new MineQuarryGoal(this));
        taskGoals.put(MiniMeTask.COLLECT, new CollectLootGoal(this));
        taskGoals.put(MiniMeTask.DUMP, new DumpInventoryGoal(this));

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
                return isTaskEnabled(MiniMeTask.COMBAT) && super.canUse();
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

    public boolean isTaskApplicable(MiniMeTask task) {
        return task.isUsableWith(getMainHandItem());
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

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel && homePos != null && isShowingArea() && this.tickCount % 10 == 0) {
            showArea(serverLevel);
        }
    }

    private void showQuarry(ServerLevel level) {
        BlockPos min = getQuarryMin();
        int width = getQuarryWidth();
        double y = homePos.getY() + 0.1;

        for (int i = 0; i <= width; i++) {
            level.sendParticles(ParticleTypes.CRIT, min.getX() + i, y, min.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CRIT, min.getX() + i, y, min.getZ() + width, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CRIT, min.getX(), y, min.getZ() + i, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CRIT, min.getX() + width, y, min.getZ() + i, 1, 0, 0, 0, 0);
        }
    }

    private void showArea(ServerLevel level) {
        double cx = homePos.getX() + 0.5;
        double cz = homePos.getZ() + 0.5;
        double y = homePos.getY() + 1.0;

        int points = Math.max(16, (int) (2 * Math.PI * homeRadius));
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    cx + Math.cos(angle) * homeRadius, y, cz + Math.sin(angle) * homeRadius,
                    1, 0, 0, 0, 0);
        }

        for (int i = 0; i < 6; i++) {
            level.sendParticles(ParticleTypes.END_ROD, cx, y + i * 0.5, cz, 1, 0, 0, 0, 0);
        }

        if (isTaskEnabled(MiniMeTask.MINE) && isTaskApplicable(MiniMeTask.MINE)) {
            showQuarry(level);
        }
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
        if (remainder.getCount() == before) return; // nothing fitted

        this.playSound(SoundEvents.ITEM_PICKUP);
        if (remainder.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(remainder);
        }
    }

    public ItemStack insertIntoStorage(ItemStack stack) {
        ItemStack remaining = stack.copy();

        for (int i = FIRST_STORAGE_SLOT; i < inventory.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
                int move = Math.min(slot.getMaxStackSize() - slot.getCount(), remaining.getCount());
                if (move > 0) {
                    slot.grow(move);
                    remaining.shrink(move);
                }
            }
        }

        for (int i = FIRST_STORAGE_SLOT; i < inventory.getContainerSize() && !remaining.isEmpty(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                int move = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                inventory.setItem(i, remaining.copyWithCount(move));
                remaining.shrink(move);
            }
        }

        inventory.setChanged();
        return remaining;
    }

    public boolean hasStorageSpace() {
        for (int i = FIRST_STORAGE_SLOT; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    public void noteBlockMined() {
        blocksMinedSinceDump++;
    }

    public void resetMinedCount() {
        blocksMinedSinceDump = 0;
    }

    public boolean shouldDumpMinedItems() {
        int every = MiniMeConfig.MINING_DUMP_EVERY.get();
        if (every <= 0 || blocksMinedSinceDump < every) return false;

        return isTaskEnabled(MiniMeTask.DUMP) && MiniMeConfig.DUMP_TO_CHEST.get() && findChestNearHome() != null;
    }

    public int getQuarryWidthSetting() {
        return this.entityData.get(DATA_QUARRY_WIDTH);
    }

    public int getQuarryWidth() {
        return Math.max(1, Math.min(getQuarryWidthSetting(), 2 * homeRadius + 1));
    }

    public void changeQuarryWidth(int delta) {
        int max = Math.max(1, Math.min(MiniMeConfig.MAX_QUARRY_WIDTH.get(), 2 * homeRadius + 1));
        this.entityData.set(DATA_QUARRY_WIDTH, Math.max(1, Math.min(getQuarryWidthSetting() + delta, max)));
    }

    @Nullable
    public BlockPos getQuarryMin() {
        if (homePos == null) return null;
        int offset = (getQuarryWidth() - 1) / 2;
        return new BlockPos(homePos.getX() - offset, homePos.getY() - 1, homePos.getZ() - offset);
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canPickUpLoot() {
        return MiniMeConfig.COLLECT_ITEMS.get();
    }

    public void setHome(BlockPos pos, int radius) {
        this.homePos = pos;
        this.homeRadius = radius;
    }

    @Nullable
    public BlockPos getHomePos() {
        return homePos;
    }

    public int getHomeRadius() {
        return homeRadius;
    }

    public boolean isWithinHomeDistance(BlockPos pos) {
        return homePos != null && pos.distSqr(homePos) <= homeRadius * homeRadius;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROFILE, ResolvableProfile.createUnresolved(Util.NIL_UUID));
        builder.define(DATA_SHOW_AREA, false);
        builder.define(DATA_TASKS, defaultTasks());
        builder.define(DATA_QUARRY_WIDTH, 5);
    }

    @Override
    protected void dropAllDeathLoot(@NotNull ServerLevel level, @NotNull DamageSource source) {
        Containers.dropContents(level, this.blockPosition(), this.inventory);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_PROFILE, ResolvableProfile.createUnresolved(uuid == null ? Util.NIL_UUID : uuid));
    }

    @Nullable
    public UUID getOwnerUUID() {
        UUID id = getProfile().partialProfile().id();
        return Util.NIL_UUID.equals(id) ? null : id;
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
        if (homePos == null) return null;

        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = homePos.offset(x, y, z);
                    if (level().getBlockState(pos).is(Blocks.CHEST)) {
                        BlockEntity blockEntity = level().getBlockEntity(pos);
                        if (blockEntity instanceof ChestBlockEntity chest) {
                            return chest;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void dumpInventoryToChest() {
        BlockEntity chestEntity = findChestNearHome();
        if (!(chestEntity instanceof ChestBlockEntity chest)) return;

        boolean hasItems = false;

        for (int i = FIRST_STORAGE_SLOT; i < this.inventory.getContainerSize(); i++) {
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
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);

        ContainerHelper.saveAllItems(output, this.inventory.getItems());

        if (homePos != null) {
            output.store("HomePos", BlockPos.CODEC, homePos);
            output.putInt("HomeRadius", homeRadius);
        }

        output.store("Profile", ResolvableProfile.CODEC, getProfile());
        output.putString("Tasks", this.entityData.get(DATA_TASKS));
        output.putInt("QuarryWidth", getQuarryWidthSetting());
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);

        this.inventory.clearContent();
        ContainerHelper.loadAllItems(input, this.inventory.getItems());
        this.syncEquipmentFromInventory();

        this.homePos = input.read("HomePos", BlockPos.CODEC).orElse(null);
        this.homeRadius = input.getIntOr("HomeRadius", MiniMeConfig.DEFAULT_HOME_RADIUS.get());

        input.read("Profile", ResolvableProfile.CODEC).ifPresent(profile -> this.entityData.set(DATA_PROFILE, profile));

        input.getString("Tasks").ifPresent(tasks -> applyTaskSettings(parseTasks(tasks)));
        this.entityData.set(DATA_QUARRY_WIDTH, Math.max(1, input.getIntOr("QuarryWidth", 5)));
    }

    public MiniMeMode getCurrentMode() {
        ItemStack stack = getMainHandItem();

        if (stack.isEmpty()) return MiniMeMode.IDLE;

        if (stack.is(ItemTags.SWORDS)) return MiniMeMode.COMBAT;
        if (stack.is(ItemTags.AXES)) return MiniMeMode.TREE_FARMING;
        if (stack.is(ItemTags.PICKAXES)) return MiniMeMode.MINE;
        if (stack.is(ItemTags.HOES)) return MiniMeMode.FARM;

        return MiniMeMode.COLLECT;
    }

}
