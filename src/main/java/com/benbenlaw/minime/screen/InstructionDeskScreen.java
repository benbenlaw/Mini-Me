package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.datamaps.InstructionCosts;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import com.benbenlaw.minime.item.MMItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InstructionDeskScreen extends AbstractContainerScreen<InstructionDeskMenu> {
    private static final Identifier TEXTURE = MiniMe.identifier( "textures/gui/instruction_desk.png");
    private static final Identifier SCROLL_ICON = MiniMe.identifier( "scroll");

    private static final int LIST_X = 29;
    private static final int LIST_Y = 17;
    private static final int LIST_WIDTH = 121;
    private static final int ROW_HEIGHT = 17;
    private static final int ROW_SPACING = 17;
    private static final int VISIBLE_ROWS = 3;

    private static final int REMOVE_SIZE = 17;
    private static final int ADD_WIDTH = LIST_WIDTH - REMOVE_SIZE - 1;

    private static final int SCROLLBAR_X = 154;
    private static final int SCROLLBAR_Y = 17;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_HEIGHT = 52;
    private static final int SCROLLER_HEIGHT = 15;

    private static final int MAX_NAMES_IN_TOOLTIP = 6;

    private final Button[] addButtons = new Button[VISIBLE_ROWS];
    private final Button[] removeButtons = new Button[VISIBLE_ROWS];
    private final int[] tooltipEntry = new int[VISIBLE_ROWS];
    private List<InstructionCosts.Entry> entries = List.of();
    private int scroll = 0;
    private boolean draggingScrollbar = false;

    public InstructionDeskScreen(InstructionDeskMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private static int rowGap(int row) {
        if (row == 0) return -1;
        if (row == VISIBLE_ROWS - 1) return 1;
        return 0;
    }

    private int rowY(int row) {
        return topPos + LIST_Y + row * ROW_SPACING + rowGap(row);
    }

    private int maxScroll() {
        return Math.max(0, entries.size() - VISIBLE_ROWS);
    }

    @Override
    protected void init() {
        super.init();
        Arrays.fill(tooltipEntry, -1);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int r = row;
            addButtons[row] = this.addRenderableWidget(Button.builder(Component.empty(), button -> send(r, false))
                    .bounds(leftPos + LIST_X, rowY(row), ADD_WIDTH, ROW_HEIGHT).build());
            removeButtons[row] = this.addRenderableWidget(Button.builder(Component.literal("x"), button -> send(r, true))
                    .bounds(leftPos + LIST_X + ADD_WIDTH + 1, rowY(row), REMOVE_SIZE, ROW_HEIGHT).build());
        }
    }

    private void send(int row, boolean remove) {
        int index = scroll + row;
        if (index >= entries.size() || this.minecraft == null || this.minecraft.gameMode == null) return;

        int id = remove ? InstructionDeskMenu.BUTTON_REMOVE_BASE + index : index;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private boolean hasInstructions() {
        return this.menu.getInstructionStack().is(MMItems.INSTRUCTIONS.get());
    }

    private boolean canAdd(InstructionCosts.Entry entry) {
        if (!hasInstructions() || this.minecraft == null || this.minecraft.player == null) return false;

        List<MiniMeTask> current = MiniMeEntity.tasksOf(this.menu.getInstructionStack());
        if (current.containsAll(entry.tasks())) return false;

        return this.minecraft.player.hasInfiniteMaterials() || InstructionDeskMenu.hasItem(this.minecraft.player, entry.items());
    }

    private boolean canRemove(InstructionCosts.Entry entry) {
        if (!hasInstructions()) return false;

        List<MiniMeTask> current = MiniMeEntity.tasksOf(this.menu.getInstructionStack());
        return entry.tasks().stream().anyMatch(current::contains);
    }

    private Item shownItem(InstructionCosts.Entry entry) {
        List<Item> items = entry.items();
        if (this.minecraft != null && this.minecraft.player != null) {
            Item owned = InstructionDeskMenu.firstOwned(this.minecraft.player, items);
            if (owned != null) return owned;
        }
        return items.get((int) (System.currentTimeMillis() / 1000 % items.size()));
    }

    private static Component costTooltip(InstructionCosts.Entry entry) {
        List<Item> items = entry.items();
        if (items.size() == 1) {
            return Component.literal("Costs 1 ").append(items.getFirst().getDefaultInstance().getHoverName());
        }

        MutableComponent text = Component.literal("Costs 1 of: ");
        int shown = Math.min(items.size(), MAX_NAMES_IN_TOOLTIP);
        for (int i = 0; i < shown; i++) {
            if (i > 0) text.append(", ");
            text.append(items.get(i).getDefaultInstance().getHoverName());
        }
        if (items.size() > shown) text.append(" and " + (items.size() - shown) + " more");
        return text;
    }

    private static String label(InstructionCosts.Entry entry) {
        return entry.tasks().stream().map(MiniMeTask::label).collect(Collectors.joining(", "));
    }

    private Component fitted(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return Component.literal(text);
        while (text.length() > 1 && font.width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return Component.literal(text + "...");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        super.extractBackground(guiGraphics, mouseX, mouseY, a);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        entries = InstructionCosts.all();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        boolean hasInstructions = hasInstructions();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            boolean shown = hasInstructions && index < entries.size();
            addButtons[row].visible = shown;
            removeButtons[row].visible = shown;
            if (!shown) continue;

            InstructionCosts.Entry entry = entries.get(index);
            addButtons[row].active = canAdd(entry);
            removeButtons[row].active = canRemove(entry);

            if (tooltipEntry[row] != index) {
                tooltipEntry[row] = index;
                addButtons[row].setTooltip(Tooltip.create(costTooltip(entry)));
                removeButtons[row].setTooltip(Tooltip.create(Component.literal("Remove these jobs (the item is not refunded)")));
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        if (!hasInstructions) {
            Component hint = Component.literal("Insert instructions");
            guiGraphics.text(font, hint, leftPos + LIST_X + (LIST_WIDTH - font.width(hint)) / 2, topPos + LIST_Y + 22, 0xFF555555, false);
            return;
        }

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= entries.size()) break;

            InstructionCosts.Entry entry = entries.get(index);
            int x = leftPos + LIST_X;
            int y = rowY(row);
            int color = addButtons[row].active ? 0xFFFFFFFF : 0xFF909090;

            guiGraphics.item(new ItemStack(shownItem(entry)), x + 1, y);
            guiGraphics.text(font, fitted(label(entry), ADD_WIDTH - 26), x + 20, y + (ROW_HEIGHT - 8) / 2 + 1, color, true);
        }

        renderScrollbar(guiGraphics);
    }

    private void renderScrollbar(GuiGraphicsExtractor guiGraphics) {
        int max = maxScroll();
        if (max == 0) return;

        int thumbY = topPos + SCROLLBAR_Y + (SCROLLBAR_HEIGHT - SCROLLER_HEIGHT) * scroll / max;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLL_ICON, leftPos + SCROLLBAR_X, thumbY, SCROLLBAR_WIDTH, SCROLLER_HEIGHT);
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        return mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= topPos + SCROLLBAR_Y && mouseY < topPos + SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    }

    private void scrollTo(double mouseY) {
        int max = maxScroll();
        if (max == 0) return;

        double travel = SCROLLBAR_HEIGHT - SCROLLER_HEIGHT;
        double fraction = (mouseY - topPos - SCROLLBAR_Y - SCROLLER_HEIGHT / 2.0) / travel;
        scroll = Math.max(0, Math.min(max, (int) Math.round(fraction * max)));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && overScrollbar(event.x(), event.y()) && maxScroll() > 0) {
            draggingScrollbar = true;
            scrollTo(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos + LIST_X && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= topPos + LIST_Y && mouseY < topPos + LIST_Y + VISIBLE_ROWS * ROW_SPACING) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
