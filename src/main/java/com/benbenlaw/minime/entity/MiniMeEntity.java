package com.benbenlaw.minime.entity;

import com.benbenlaw.minime.entity.goals.CollectLootGoal;
import com.benbenlaw.minime.entity.goals.DumpInventoryGoal;
import com.benbenlaw.minime.entity.goals.ReturnHomeGoal;
import com.benbenlaw.minime.entity.goals.SwordAttackGoal;
import com.benbenlaw.minime.screen.MiniMeMenu;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.*;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class MiniMeEntity extends PathfinderMob implements ContainerListener, HasCustomInventoryScreen {

    private SimpleContainer inventory = new SimpleContainer(21);
    private int actionCooldown = 0;
    private BlockPos homePos;
    private int homeRadius = 8;

    public static int HEAD_SLOT = 0;
    public static int CHEST_SLOT = 1;
    public static int LEGS_SLOT = 2;
    public static int BOOTS_SLOT = 3;
    public static int MAIN_HAND = 4;
    public static int OFF_HAND = 5;

    private static final EntityDataAccessor<ItemStack> DATA_HEAD = SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_CHEST = SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_LEGS = SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_BOOTS = SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final EntityDataAccessor<ItemStack> DATA_MAIN_HAND = SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_OFF_HAND = SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.ITEM_STACK);



    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public MiniMeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.createInventory();
        this.setCanPickUpLoot(true);
    }

    @Override
    protected void registerGoals() {

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SwordAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new CollectLootGoal(this));
        this.goalSelector.addGoal(4, new ReturnHomeGoal(this));
        this.goalSelector.addGoal(5, new DumpInventoryGoal(this));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));

    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack stackToPickup = itemEntity.getItem().copy();

        for (int i = 6; i < this.inventory.getContainerSize(); i++) {
            ItemStack slot = this.inventory.getItem(i);

            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stackToPickup)) {
                int availableSpace = slot.getMaxStackSize() - slot.getCount();
                if (availableSpace > 0) {
                    int toAdd = Math.min(availableSpace, stackToPickup.getCount());
                    slot.grow(toAdd);
                    stackToPickup.shrink(toAdd);
                    this.inventory.setChanged();

                    if (stackToPickup.isEmpty()) {
                        itemEntity.discard();
                        this.makeSound(SoundEvents.ITEM_PICKUP);
                        return;
                    }
                }
            }
        }

        for (int i = 6; i < this.inventory.getContainerSize(); i++) {
            ItemStack slot = this.inventory.getItem(i);

            if (slot.isEmpty()) {
                int toAdd = Math.min(stackToPickup.getCount(), stackToPickup.getMaxStackSize());
                ItemStack newStack = stackToPickup.copy();
                newStack.setCount(toAdd);
                this.inventory.setItem(i, newStack);
                stackToPickup.shrink(toAdd);
                this.inventory.setChanged();

                if (stackToPickup.isEmpty()) {
                    this.makeSound(SoundEvents.ITEM_PICKUP);
                    itemEntity.discard();
                    return;
                }
            }
        }

        itemEntity.setItem(stackToPickup);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return; // Only run AI server-side

        if (actionCooldown > 0) {
            actionCooldown--;
            return;
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
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
        builder.define(DATA_HEAD, ItemStack.EMPTY);
        builder.define(DATA_CHEST, ItemStack.EMPTY);
        builder.define(DATA_LEGS, ItemStack.EMPTY);
        builder.define(DATA_BOOTS, ItemStack.EMPTY);
        builder.define(DATA_MAIN_HAND, ItemStack.EMPTY);
        builder.define(DATA_OFF_HAND, ItemStack.EMPTY);
        builder.define(DATA_OWNER_UUID, Optional.empty());
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource sourse) {
        Containers.dropContents(level, this.blockPosition(), this.inventory);
    }

    @Override
    public @NotNull ItemStack getMainHandItem() {
        return this.entityData.get(DATA_MAIN_HAND);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public boolean hasInventoryChanged(Container inventory) {
        return this.inventory != inventory;
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!this.level().isClientSide()) {

            if (player.isShiftKeyDown()) {
                this.setHome(this.blockPosition(), 8);
                player.displayClientMessage(Component.literal("MiniMe home set!"), true);
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

            serverPlayer.openMenu(new SimpleMenuProvider((ix, playerInventory, playerEntityx) ->
                    new MiniMeMenu(ix, playerInventory, this.inventory, this), this.getDisplayName()), buf -> {
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

        for (int i = 3; i < this.inventory.getContainerSize(); i++) {
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
            this.playSound(SoundEvents.CHEST_CLOSE, 1.0F, 1.0F);
        }

    }

    @Override
    public void containerChanged(Container inventory) {
        this.setItemSlot(EquipmentSlot.MAINHAND, inventory.getItem(MAIN_HAND));
        this.setItemSlot(EquipmentSlot.OFFHAND, inventory.getItem(OFF_HAND));
        this.setItemSlot(EquipmentSlot.HEAD, inventory.getItem(HEAD_SLOT));
        this.setItemSlot(EquipmentSlot.CHEST, inventory.getItem(CHEST_SLOT));
        this.setItemSlot(EquipmentSlot.LEGS, inventory.getItem(LEGS_SLOT));
        this.setItemSlot(EquipmentSlot.FEET, inventory.getItem(BOOTS_SLOT));
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> getInventory().getItem(MAIN_HAND);
            case OFFHAND -> getInventory().getItem(OFF_HAND);
            case HEAD -> getInventory().getItem(HEAD_SLOT);
            case CHEST -> getInventory().getItem(CHEST_SLOT);
            case LEGS -> getInventory().getItem(LEGS_SLOT);
            case FEET -> getInventory().getItem(BOOTS_SLOT);
            default -> super.getItemBySlot(slot);
        };
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        ListTag listtag = new ListTag();
        for (int x = 0; x < this.inventory.getContainerSize(); x++) {
            ItemStack itemstack = this.inventory.getItem(x);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte)(x));
                listtag.add(itemstack.save(this.registryAccess(), compoundtag));
            }
        }

        if (homePos != null) {
            NbtUtils.writeBlockPos(homePos);
        }

        UUID uuid = getOwnerUUID();
        if (uuid != null) {
            pCompound.putUUID("OwnerUUID", uuid);
        }

        pCompound.put("Items", listtag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        this.createInventory();
        ListTag listtag = pCompound.getList("Items", 10);

        for (int x = 0; x < listtag.size(); x++) {
            CompoundTag compoundtag = listtag.getCompound(x);
            int j = compoundtag.getByte("Slot") & 255;
            if (j < this.inventory.getContainerSize()) {
                ItemStack stack = ItemStack.parse(this.registryAccess(), compoundtag).orElse(ItemStack.EMPTY);
                this.inventory.setItem(j, stack);

            }
        }

        if (pCompound.contains("HomePos")) {
            this.homePos = NbtUtils.readBlockPos(pCompound, "HomePos").orElse(null);
        }

        if (pCompound.hasUUID("OwnerUUID")) {
            setOwnerUUID(pCompound.getUUID("OwnerUUID"));
        }
    }

    public void createInventory() {
        SimpleContainer old = this.inventory;
        this.inventory = new SimpleContainer(21);

        if (old != null) {
            old.removeListener(this);
            int size = Math.min(old.getContainerSize(), this.inventory.getContainerSize());

            for (int i = 0; i < size; i++) {
                ItemStack stack = old.getItem(i);
                if (!stack.isEmpty()) {
                    this.inventory.setItem(i, stack.copy());
                }
            }
        }

        this.inventory.addListener(this);

    }

}
