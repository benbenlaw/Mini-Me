package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.factory.ViewEntry;
import com.benbenlaw.minime.network.packet.FactoryClickPacket;
import com.benbenlaw.minime.network.packet.FactoryOpenPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.SpriteIconButton;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MiniMeFactoryScreen extends AbstractContainerScreen<MiniMeFactoryMenu> {
    private static final Identifier TEXTURE = MiniMe.identifier( "textures/gui/mini_me_factory.png");
    private static final Identifier WRENCH_ICON = MiniMe.identifier( "wrench");

    private static final Identifier SCROLL_ICON = MiniMe.identifier( "scroll");

    private static final int SCROLLBAR_X = 154;
    private static final int SCROLLBAR_Y = 22;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_HEIGHT = 70;
    private static final int SCROLLER_HEIGHT = 15;

    private static final float COUNT_SCALE = 0.55F;

    private static final int VIEW_X = 8;
    private static final int VIEW_Y = 22;
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final int CELL = 18;

    private static final int SEARCH_X = 8;
    private static final int SEARCH_Y = 4;
    private static final int SEARCH_WIDTH = 124;
    private static final int SEARCH_HEIGHT = 16;

    private static final int AREA_X = 134;
    private static final int CONFIG_X = 152;
    private static final int CONFIG_Y = 4;
    private static final int CONFIG_SIZE = 16;

    private static final int KEY_ESCAPE = 256;

    private record Shown(ViewEntry entry, String name, String mod) {
    }

    private EditBox searchBox;
    private Button areaButton;
    private Boolean areaShown;
    private List<ViewEntry> sourceView = List.of();
    private List<Shown> shown = List.of();
    private String filter = "";
    private int scroll;
    private boolean draggingScrollbar;

    public MiniMeFactoryScreen(MiniMeFactoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 245);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 1000;
        this.inventoryLabelY = 152;

        this.searchBox = new EditBox(this.font, leftPos + SEARCH_X, topPos + SEARCH_Y, SEARCH_WIDTH, SEARCH_HEIGHT, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Search"));
        this.searchBox.setMaxLength(32);
        this.searchBox.setValue(filter);
        this.searchBox.setResponder(text -> {
            filter = text.toLowerCase(Locale.ROOT).trim();
            scroll = 0;
            rebuild();
        });
        this.addRenderableWidget(this.searchBox);

        SpriteIconButton config = SpriteIconButton.builder(Component.literal("Orders"), button ->
                        ClientPacketDistributor.sendToServer(new FactoryOpenPacket(menu.getBlockEntity().getBlockPos(), true)), true)
                .size(CONFIG_SIZE, CONFIG_SIZE)
                .sprite(WRENCH_ICON, 12, 12)
                .withTootip()
                .build();
        config.setPosition(leftPos + CONFIG_X, topPos + CONFIG_Y);
        config.setTooltip(Tooltip.create(Component.literal("Orders: what the Mini Me workers keep stocked.\nFirst slot is highest priority. Drag to reorder.\nScroll to change the amount (Shift 10, Ctrl 100).\nRight-click to remove. Click with an item to add.")));
        this.addRenderableWidget(config);

        this.areaButton = this.addRenderableWidget(Button.builder(Component.literal("[]"), button -> sendRaw(MiniMeFactoryMenu.BUTTON_TOGGLE_AREA))
                .bounds(leftPos + AREA_X, topPos + CONFIG_Y, CONFIG_SIZE, CONFIG_SIZE).build());
        this.areaShown = null;

        rebuild();
    }

    private void sendRaw(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void rebuild() {
        sourceView = menu.getView();
        List<Shown> built = new ArrayList<>();
        for (ViewEntry entry : sourceView) {
            String name = entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT);
            String mod = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).getNamespace();

            boolean matches;
            if (filter.isEmpty()) {
                matches = true;
            } else if (filter.startsWith("@")) {
                matches = mod.startsWith(filter.substring(1));
            } else {
                matches = name.contains(filter);
            }
            if (matches) built.add(new Shown(entry, name, mod));
        }
        shown = built;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int maxScroll() {
        return Math.max(0, (shown.size() + COLUMNS - 1) / COLUMNS - ROWS);
    }

    private static String abbreviate(long count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1_000_000) return String.format(Locale.ROOT, "%.1fk", count / 1000.0).replace(".0k", "k");
        return String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0).replace(".0M", "M");
    }

    private void drawCount(GuiGraphicsExtractor graphics, int x, int y, String text) {
        int width = this.font.width(text);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 16.5F - width * COUNT_SCALE, y + 16.0F - 8.0F * COUNT_SCALE);
        graphics.pose().scale(COUNT_SCALE, COUNT_SCALE);
        graphics.text(this.font, text, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
    }

    private Shown entryAt(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - leftPos - VIEW_X) / CELL);
        int row = (int) Math.floor((mouseY - topPos - VIEW_Y) / CELL);
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) return null;

        int index = (scroll + row) * COLUMNS + column;
        return index < shown.size() ? shown.get(index) : null;
    }

    private boolean overView(double mouseX, double mouseY) {
        return mouseX >= leftPos + VIEW_X - 1 && mouseX < leftPos + VIEW_X + COLUMNS * CELL - 1
                && mouseY >= topPos + VIEW_Y - 1 && mouseY < topPos + VIEW_Y + ROWS * CELL - 1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (sourceView != menu.getView()) rebuild();
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int index = (scroll + row) * COLUMNS + column;
                if (index >= shown.size()) break;

                ViewEntry entry = shown.get(index).entry();
                int x = leftPos + VIEW_X + column * CELL;
                int y = topPos + VIEW_Y + row * CELL;
                graphics.item(entry.stack(), x, y);
                graphics.itemDecorations(this.font, entry.stack(), x, y, "");
                drawCount(graphics, x, y, abbreviate(entry.count()));
            }
        }

        Shown hovered = overView(mouseX, mouseY) ? entryAt(mouseX, mouseY) : null;
        if (hovered != null) {
            int column = (int) Math.floor((mouseX - leftPos - VIEW_X) / CELL);
            int row = (int) Math.floor((mouseY - topPos - VIEW_Y) / CELL);
            int x = leftPos + VIEW_X + column * CELL;
            int y = topPos + VIEW_Y + row * CELL;
            graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean showing = menu.getBlockEntity().isShowingArea();
        if (areaShown == null || areaShown != showing) {
            areaShown = showing;
            areaButton.setTooltip(Tooltip.create(Component.literal("Show factory area: " + (showing ? "ON" : "OFF"))));
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Shown hovered = overView(mouseX, mouseY) ? entryAt(mouseX, mouseY) : null;
        if (hovered != null) {
            List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(hovered.entry().stack()));
            lines.add(Component.literal("In the factory: " + hovered.entry().count()));
            graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY, hovered.entry().stack());
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
        if (overView(mouseX, mouseY)) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (searchBox.isFocused() && !searchBox.isMouseOver(event.x(), event.y())) {
            searchBox.setFocused(false);
            this.setFocused(null);
        }

        if (event.button() == 0 && overScrollbar(event.x(), event.y()) && maxScroll() > 0) {
            draggingScrollbar = true;
            scrollTo(event.y());
            return true;
        }

        if ((event.button() == 0 || event.button() == 1) && overView(event.x(), event.y())) {
            Shown clicked = entryAt(event.x(), event.y());
            ItemStack key = clicked == null ? ItemStack.EMPTY : clicked.entry().stack();
            ClientPacketDistributor.sendToServer(new FactoryClickPacket(menu.containerId, key, event.button(), event.hasShiftDown()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchBox.isFocused()) {
            if (event.key() == KEY_ESCAPE) {
                searchBox.setFocused(false);
                this.setFocused(null);
                return true;
            }
            if (searchBox.keyPressed(event) || searchBox.canConsumeInput()) return true;
        }
        return super.keyPressed(event);
    }
}
