package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.entity.MiniMeFactoryBlockEntity;
import com.benbenlaw.minime.factory.FactoryStatus;
import com.benbenlaw.minime.network.packet.FactoryOpenPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FactoryOrdersScreen extends AbstractContainerScreen<FactoryOrdersMenu> {
    private static final Identifier TEXTURE = MiniMe.identifier( "textures/gui/mini_me_factory_stock.png");

    private static final float COUNT_SCALE = 0.55F;

    private static final int GRID_X = 8;
    private static final int GRID_Y = 17;
    private static final int CELL = 18;
    private static final int COLUMNS = 8;
    private static final int ROWS = 3;

    private static final int BACK_X = 154;
    private static final int BACK_Y = 3;
    private static final int BACK_SIZE = 12;

    private static final int STATUS_Y = 73;
    private static final int STATUS_WIDTH = 160;
    private static final int DRAG_THRESHOLD = 3;

    private static final int TEXT_COLOR = 0xFF404040;
    private static final int GOOD_COLOR = 0xFF55FF55;
    private static final int BAD_COLOR = 0xFFFF5555;

    private final int[] pending = new int[MiniMeFactoryBlockEntity.MAX_ORDERS];
    private int dragFrom = -1;
    private double dragStartX;
    private double dragStartY;
    private boolean dragging;

    public FactoryOrdersScreen(FactoryOrdersMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        Arrays.fill(pending, -1);
    }

    private List<MiniMeFactoryBlockEntity.Order> orders() {
        return menu.getBlockEntity().getOrders();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;

        Button back = this.addRenderableWidget(Button.builder(Component.literal("<"), button ->
                        ClientPacketDistributor.sendToServer(new FactoryOpenPacket(menu.getBlockEntity().getBlockPos(), false)))
                .bounds(leftPos + BACK_X, topPos + BACK_Y, BACK_SIZE, BACK_SIZE).build());
        back.setTooltip(Tooltip.create(Component.literal("Back to the factory")));
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private int cellAt(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - leftPos - GRID_X) / CELL);
        int row = (int) Math.floor((mouseY - topPos - GRID_Y) / CELL);
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) return -1;
        return row * COLUMNS + column;
    }

    private int cellX(int index) {
        return leftPos + GRID_X + (index % COLUMNS) * CELL;
    }

    private int cellY(int index) {
        return topPos + GRID_Y + (index / COLUMNS) * CELL;
    }

    private int amountOf(int index) {
        int stored = orders().get(index).amount();
        if (pending[index] == stored) pending[index] = -1;
        return pending[index] >= 0 ? pending[index] : stored;
    }

    private static String abbreviate(long count) {
        if (count < 1000) return String.valueOf(count);
        return String.format(java.util.Locale.ROOT, "%.1fk", count / 1000.0).replace(".0k", "k");
    }

    private void drawCount(GuiGraphicsExtractor graphics, int x, int y, String text, int color) {
        int width = this.font.width(text);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 16.5F - width * COUNT_SCALE, y + 16.0F - 8.0F * COUNT_SCALE);
        graphics.pose().scale(COUNT_SCALE, COUNT_SCALE);
        graphics.text(this.font, text, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        List<MiniMeFactoryBlockEntity.Order> orders = orders();
        for (int index = 0; index < orders.size() && index < COLUMNS * ROWS; index++) {
            MiniMeFactoryBlockEntity.Order order = orders.get(index);
            int x = cellX(index);
            int y = cellY(index);

            graphics.item(order.item(), x, y);
            int amount = amountOf(index);
            int have = menu.getBlockEntity().stockOf(index);
            drawCount(graphics, x, y, abbreviate(amount), have >= amount ? GOOD_COLOR : BAD_COLOR);
        }

        int hovered = cellAt(mouseX, mouseY);
        if (hovered >= 0 && hovered < orders.size()) {
            graphics.fill(cellX(hovered), cellY(hovered), cellX(hovered) + 16, cellY(hovered) + 16, 0x80FFFFFF);
        }

        if (dragging && dragFrom >= 0 && dragFrom < orders.size()) {
            if (hovered >= 0 && hovered < orders.size() && hovered != dragFrom) {
                graphics.fill(cellX(hovered), cellY(hovered), cellX(hovered) + 16, cellY(hovered) + 16, 0x8055FF55);
            }
            graphics.item(orders.get(dragFrom).item(), mouseX - 8, mouseY - 8);
        }
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
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(this.font, "Mini Me Factory", leftPos + 8, topPos + 6, TEXT_COLOR, false);

        List<MiniMeFactoryBlockEntity.Order> orders = orders();
        MiniMeFactoryBlockEntity factory = menu.getBlockEntity();
        FactoryStatus status = FactoryStatus.of(menu.getStatus());
        graphics.text(this.font, fitted(status.text, STATUS_WIDTH), leftPos + 8, topPos + STATUS_Y, TEXT_COLOR, false);

        if (mouseX >= leftPos + 8 && mouseX < leftPos + 8 + STATUS_WIDTH && mouseY >= topPos + STATUS_Y - 1 && mouseY < topPos + STATUS_Y + 9) {
            String active = factory.getActiveLabel();
            String tip = status.text + (active.isEmpty() ? "" : " " + active) + " Chests: " + factory.storages().size() + ".";
            graphics.setTooltipForNextFrame(this.font, Component.literal(tip), mouseX, mouseY);
            return;
        }

        int hovered = cellAt(mouseX, mouseY);
        if (!dragging && hovered >= 0 && hovered < orders.size() && menu.getCarried().isEmpty()) {
            MiniMeFactoryBlockEntity.Order order = orders.get(hovered);
            List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(order.item()));
            lines.add(Component.literal("Keeping at least " + amountOf(hovered) + " (" + factory.stockOf(hovered) + " in the chests)"));
            graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY, order.item());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int cell = cellAt(event.x(), event.y());
        if (cell >= 0) {
            List<MiniMeFactoryBlockEntity.Order> orders = orders();
            if (!menu.getCarried().isEmpty()) {
                if (event.button() == 0) sendButton(FactoryOrdersMenu.BUTTON_ADD_CARRIED);
                return true;
            }
            if (cell < orders.size()) {
                if (event.button() == 1) {
                    sendButton(FactoryOrdersMenu.BUTTON_REMOVE_BASE + cell);
                    return true;
                }
                if (event.button() == 0) {
                    dragFrom = cell;
                    dragStartX = event.x();
                    dragStartY = event.y();
                    dragging = false;
                    return true;
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragFrom >= 0) {
            if (!dragging && Math.abs(event.x() - dragStartX) + Math.abs(event.y() - dragStartY) >= DRAG_THRESHOLD) dragging = true;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragFrom >= 0) {
            int target = cellAt(event.x(), event.y());
            if (dragging && target >= 0 && target < orders().size() && target != dragFrom) {
                sendButton(FactoryOrdersMenu.moveButtonId(dragFrom, target));
                pending[dragFrom] = -1;
                pending[target] = -1;
            }
            dragFrom = -1;
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            if (cell < orders().size() && scrollY != 0) {
                int step = this.minecraft.hasControlDown() ? 100 : this.minecraft.hasShiftDown() ? 10 : 1;
                int next = Math.clamp((long) amountOf(cell) + (scrollY > 0 ? step : -step), 1, MiniMeFactoryBlockEntity.MAX_STOCK);
                if (next != amountOf(cell)) {
                    pending[cell] = next;
                    sendButton(FactoryOrdersMenu.amountButtonId(cell, next));
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
