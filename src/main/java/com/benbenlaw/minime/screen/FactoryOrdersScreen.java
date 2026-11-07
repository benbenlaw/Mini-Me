package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.entity.MiniMeFactoryBlockEntity;
import com.benbenlaw.minime.factory.FactoryStatus;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import com.benbenlaw.minime.network.packet.FactoryOpenPacket;

import java.util.Arrays;
import java.util.List;

public class FactoryOrdersScreen extends AbstractContainerScreen<FactoryOrdersMenu> {
    private static final Identifier TEXTURE = MiniMe.identifier( "textures/gui/mini_me_tasks.png");
    private static final Identifier SCROLL_ICON = MiniMe.identifier( "scroll");

    private static final int LIST_X = 29;
    private static final int LIST_Y = 17;
    private static final int LIST_WIDTH = 121;
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_SPACING = 17;
    private static final int VISIBLE_ROWS = 3;

    private static final int SCROLLBAR_X = 154;
    private static final int SCROLLBAR_Y = 17;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_HEIGHT = 52;
    private static final int SCROLLER_HEIGHT = 15;

    private static final int SIDE_BUTTON_X = 9;
    private static final int BACK_Y = 16;
    private static final int SIDE_BUTTON_SIZE = 16;

    private static final int MAIN_WIDTH = 56;
    private static final int BOX_X = 57;
    private static final int BOX_WIDTH = 30;
    private static final int UP_X = 89;
    private static final int DOWN_X = 100;
    private static final int REMOVE_X = 111;
    private static final int SMALL_WIDTH = 10;

    private static final int STATUS_Y = 73;
    private static final int STATUS_WIDTH = 160;

    private static final int TEXT_COLOR = 0xFF404040;
    private static final int GOOD_COLOR = 0xFF55FF55;
    private static final int BAD_COLOR = 0xFFFF5555;

    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_KP_ENTER = 335;

    private static class NumberBox extends EditBox {
        NumberBox(Font font, int width, int height) {
            super(font, 0, 0, width, height, Component.empty());
            setMaxLength(4);
        }

        @Override
        public void insertText(String text) {
            super.insertText(text.replaceAll("[^0-9]", ""));
        }
    }

    private int scroll;
    private boolean draggingScrollbar;
    private Button backButton;
    private final Button[] mainButtons = new Button[VISIBLE_ROWS];
    private final NumberBox[] boxes = new NumberBox[VISIBLE_ROWS];
    private final Button[] upButtons = new Button[VISIBLE_ROWS];
    private final Button[] downButtons = new Button[VISIBLE_ROWS];
    private final Button[] removeButtons = new Button[VISIBLE_ROWS];
    private final int[] rowIndex = new int[VISIBLE_ROWS];
    private final int[] lastSeen = new int[VISIBLE_ROWS];
    private final String[] tipKeys = new String[VISIBLE_ROWS];
    private int lastFocusedRow = -1;

    public FactoryOrdersScreen(FactoryOrdersMenu menu, Inventory inventory, Component title) {
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

    private List<MiniMeFactoryBlockEntity.Order> orders() {
        return menu.getBlockEntity().getOrders();
    }

    private int maxScroll() {
        return Math.max(0, orders().size() - VISIBLE_ROWS);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 1000;

        this.backButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button ->
                        ClientPacketDistributor.sendToServer(new FactoryOpenPacket(menu.getBlockEntity().getBlockPos(), false)))
                .bounds(leftPos + SIDE_BUTTON_X, topPos + BACK_Y, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE).build());
        this.backButton.setTooltip(Tooltip.create(Component.literal("Back to the factory")));

        Arrays.fill(rowIndex, -1);
        Arrays.fill(lastSeen, Integer.MIN_VALUE);
        Arrays.fill(tipKeys, null);
        lastFocusedRow = -1;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int r = row;

            mainButtons[row] = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            }).bounds(leftPos + LIST_X, topPos, MAIN_WIDTH, ROW_HEIGHT).build());

            NumberBox box = new NumberBox(this.font, BOX_WIDTH, ROW_HEIGHT);
            box.setTooltip(Tooltip.create(Component.literal("Keep at least this many in the factory's chests. Press Enter to apply.")));
            boxes[row] = this.addRenderableWidget(box);

            upButtons[row] = this.addRenderableWidget(Button.builder(Component.literal("^"), button -> move(r, FactoryOrdersMenu.BUTTON_UP_BASE))
                    .bounds(leftPos + LIST_X + UP_X, topPos, SMALL_WIDTH, ROW_HEIGHT).build());
            upButtons[row].setTooltip(Tooltip.create(Component.literal("Higher priority")));

            downButtons[row] = this.addRenderableWidget(Button.builder(Component.literal("v"), button -> move(r, FactoryOrdersMenu.BUTTON_DOWN_BASE))
                    .bounds(leftPos + LIST_X + DOWN_X, topPos, SMALL_WIDTH, ROW_HEIGHT).build());
            downButtons[row].setTooltip(Tooltip.create(Component.literal("Lower priority")));

            removeButtons[row] = this.addRenderableWidget(Button.builder(Component.literal("x"), button -> move(r, FactoryOrdersMenu.BUTTON_REMOVE_BASE))
                    .bounds(leftPos + LIST_X + REMOVE_X, topPos, SMALL_WIDTH, ROW_HEIGHT).build());
            removeButtons[row].setTooltip(Tooltip.create(Component.literal("Remove this order")));
        }
        refresh();
    }

    private void sendButton(int id) {
        if (this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void move(int row, int base) {
        if (rowIndex[row] >= 0) sendButton(base + rowIndex[row]);
    }

    private void commit(int row) {
        int index = rowIndex[row];
        if (index < 0) return;

        try {
            int value = Math.clamp(Integer.parseInt(boxes[row].getValue().trim()), 1, MiniMeFactoryBlockEntity.MAX_STOCK);
            boxes[row].setValue(String.valueOf(value));
            lastSeen[row] = value;
            sendButton(FactoryOrdersMenu.amountButtonId(index, value));
        } catch (NumberFormatException e) {
            lastSeen[row] = Integer.MIN_VALUE;
        }
    }

    private int focusedRow() {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            if (boxes[row].isFocused()) return row;
        }
        return -1;
    }

    private void refresh() {
        int focused = focusedRow();
        if (lastFocusedRow >= 0 && lastFocusedRow != focused) commit(lastFocusedRow);
        lastFocusedRow = focused;

        List<MiniMeFactoryBlockEntity.Order> orders = orders();
        scroll = Math.clamp(scroll, 0, maxScroll());

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            boolean shown = index < orders.size();

            if (rowIndex[row] != (shown ? index : -1)) {
                if (boxes[row].isFocused()) {
                    boxes[row].setFocused(false);
                    this.setFocused(null);
                }
                lastSeen[row] = Integer.MIN_VALUE;
            }
            rowIndex[row] = shown ? index : -1;

            mainButtons[row].visible = shown;
            boxes[row].visible = shown;
            upButtons[row].visible = shown;
            downButtons[row].visible = shown;
            removeButtons[row].visible = shown;
            if (!shown) {
                tipKeys[row] = null;
                continue;
            }

            int y = rowY(row);
            mainButtons[row].setX(leftPos + LIST_X);
            mainButtons[row].setY(y);
            boxes[row].setX(leftPos + LIST_X + BOX_X);
            boxes[row].setY(y);
            upButtons[row].setY(y);
            upButtons[row].active = index > 0;
            downButtons[row].setY(y);
            downButtons[row].active = index < orders.size() - 1;
            removeButtons[row].setY(y);

            MiniMeFactoryBlockEntity.Order order = orders.get(index);
            int amount = order.amount();
            if (!boxes[row].isFocused() && amount != lastSeen[row]) {
                boxes[row].setValue(String.valueOf(amount));
                lastSeen[row] = amount;
            }

            int have = menu.getBlockEntity().stockOf(index);
            String key = index + ":" + have + ":" + amount + ":" + order.item().getHoverName().getString();
            if (!key.equals(tipKeys[row])) {
                tipKeys[row] = key;
                mainButtons[row].setTooltip(Tooltip.create(Component.literal(order.item().getHoverName().getString()
                        + ": " + have + " in the chests, keeping " + amount + ".")));
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        int x = leftPos + FactoryOrdersMenu.PATTERN_SLOT_X - 1;
        int y = topPos + FactoryOrdersMenu.PATTERN_SLOT_Y - 1;
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    private String fitted(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (text.length() > 1 && font.width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        refresh();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(this.font, "Mini Me Factory", leftPos + 8, topPos + 6, TEXT_COLOR, false);

        List<MiniMeFactoryBlockEntity.Order> orders = orders();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = rowIndex[row];
            if (index < 0) continue;

            MiniMeFactoryBlockEntity.Order order = orders.get(index);
            int x = leftPos + LIST_X;
            int y = rowY(row);

            ItemStack stack = order.item();
            graphics.item(stack, x + 2, y + 1);

            int have = menu.getBlockEntity().stockOf(index);
            String haveText = String.valueOf(have);
            graphics.text(this.font, haveText, x + MAIN_WIDTH - 4 - font.width(haveText), y + (ROW_HEIGHT - 8) / 2 + 1,
                    have >= order.amount() ? GOOD_COLOR : BAD_COLOR, true);
        }

        if (orders.isEmpty()) {
            graphics.textWithWordWrap(this.font, Component.literal("No orders yet. Drop an item on the slot at the left to add one."),
                    leftPos + LIST_X + 2, rowY(0) + 2, LIST_WIDTH - 4, TEXT_COLOR);
        }

        MiniMeFactoryBlockEntity factory = menu.getBlockEntity();
        FactoryStatus status = FactoryStatus.of(menu.getStatus());
        String statusText = fitted(status.text, STATUS_WIDTH);
        graphics.text(this.font, statusText, leftPos + 8, topPos + STATUS_Y, TEXT_COLOR, false);

        if (mouseX >= leftPos + 8 && mouseX < leftPos + 8 + STATUS_WIDTH && mouseY >= topPos + STATUS_Y - 1 && mouseY < topPos + STATUS_Y + 9) {
            String active = factory.getActiveLabel();
            String tip = status.text + (active.isEmpty() ? "" : " " + active)
                    + " Chests: " + factory.storages().size() + ", machines: " + factory.machines().size() + ".";
            graphics.setTooltipForNextFrame(this.font, Component.literal(tip), mouseX, mouseY);
        }

        FactoryOrdersMenu menuRef = menu;
        if (this.hoveredSlot != null && this.hoveredSlot.index == FactoryOrdersMenu.PATTERN_SLOT_INDEX && menuRef.getCarried().isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, Component.literal("Drop an item here to add an order."), mouseX, mouseY);
        }

        if (maxScroll() > 0) {
            int thumbY = topPos + SCROLLBAR_Y + (SCROLLBAR_HEIGHT - SCROLLER_HEIGHT) * scroll / maxScroll();
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLL_ICON, leftPos + SCROLLBAR_X, thumbY, SCROLLBAR_WIDTH, SCROLLER_HEIGHT);
        }
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
        int focused = focusedRow();
        if (focused >= 0 && !boxes[focused].isMouseOver(event.x(), event.y())) {
            boxes[focused].setFocused(false);
            this.setFocused(null);
        }

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

    @Override
    public boolean keyPressed(KeyEvent event) {
        int focused = focusedRow();
        if (focused >= 0) {
            int key = event.key();
            if (key == KEY_ESCAPE || key == KEY_ENTER || key == KEY_KP_ENTER) {
                commit(focused);
                boxes[focused].setFocused(false);
                this.setFocused(null);
                lastFocusedRow = -1;
                return true;
            }
            if (boxes[focused].keyPressed(event) || boxes[focused].canConsumeInput()) return true;
        }
        return super.keyPressed(event);
    }
}
