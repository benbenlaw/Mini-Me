package com.benbenlaw.minime.integration.jei;

import com.benbenlaw.minime.factory.FactoryRecipeFill;
import com.benbenlaw.minime.factory.ViewEntry;
import com.benbenlaw.minime.network.packet.FactoryFillPacket;
import com.benbenlaw.minime.screen.MMMenus;
import com.benbenlaw.minime.screen.MiniMeFactoryMenu;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FactoryTransferHandler implements IRecipeTransferHandler<MiniMeFactoryMenu, RecipeHolder<CraftingRecipe>> {
    private static final int MAX_CANDIDATES = 48;

    private final IRecipeTransferHandlerHelper helper;

    public FactoryTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends MiniMeFactoryMenu> getContainerClass() {
        return MiniMeFactoryMenu.class;
    }

    @Override
    public Optional<MenuType<MiniMeFactoryMenu>> getMenuType() {
        return Optional.of(MMMenus.FACTORY_MENU.get());
    }

    @Override
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    private static long countOf(List<ViewEntry> view, ItemStack stack) {
        for (ViewEntry entry : view) {
            if (ItemStack.isSameItemSameComponents(entry.stack(), stack)) return entry.count();
        }
        return 0L;
    }

    @Override
    public IRecipeTransferError transferRecipe(MiniMeFactoryMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return helper.createInternalError();

        List<ViewEntry> view = menu.getView();
        Map<Integer, SlotDisplay> displays = helper.getGuiSlotIndexToIngredientMap(recipe);

        List<FactoryRecipeFill.Cell> cells = new ArrayList<>();
        for (Map.Entry<Integer, SlotDisplay> entry : displays.entrySet()) {
            int slot = Math.max(0, Math.min(8, entry.getKey()));
            List<ItemStack> candidates = new ArrayList<>();
            for (ItemStack stack : entry.getValue().resolveForStacks(SlotDisplayContext.fromLevel(minecraft.level))) {
                if (!stack.isEmpty() && countOf(view, stack) > 0 && candidates.size() < MAX_CANDIDATES) candidates.add(stack.copyWithCount(1));
            }
            cells.add(new FactoryRecipeFill.Cell(slot, candidates));
        }

        FactoryRecipeFill.Result result = FactoryRecipeFill.plan(cells, stack -> countOf(view, stack), maxTransfer);
        if (!result.missing().isEmpty()) {
            List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
            List<IRecipeSlotView> missingViews = new ArrayList<>();
            for (int slot : result.missing()) {
                if (slot >= 0 && slot < inputs.size()) missingViews.add(inputs.get(slot));
            }
            return helper.createUserErrorForMissingSlots(Component.literal("Missing Items"), missingViews);
        }

        if (doTransfer) {
            ClientPacketDistributor.sendToServer(new FactoryFillPacket(menu.containerId, cells, maxTransfer));
        }
        return null;
    }
}
