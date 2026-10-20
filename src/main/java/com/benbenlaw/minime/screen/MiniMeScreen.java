package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class MiniMeScreen extends AbstractContainerScreen<MiniMeMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MiniMe.MOD_ID, "textures/entity/gui/mini_me_inventory.png");

    private static final int MODEL_SIZE = 44;
    private static final float MODEL_OFFSET_Y = 0.0F;

    private static final int TASK_COUNT = MiniMeTask.values().length;
    private static final int ROW_HEIGHT = 22;
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int TEXT_COLOR = 0xFF404040;

    private final MiniMeEntity miniMeEntity;
    private float xMouse;
    private float yMouse;

    private final List<Integer> shownTasks = new ArrayList<>();

    private boolean taskMode;
    private Button tasksButton;
    private Button areaButton;
    private Button widthDownButton;
    private Button widthUpButton;
    private boolean showWidthRow;
    private int widthRowY;
    private final Button[] toggleButtons = new Button[TASK_COUNT];
    private final Button[] upButtons = new Button[TASK_COUNT];
    private final Button[] downButtons = new Button[TASK_COUNT];

    public MiniMeScreen(MiniMeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.miniMeEntity = menu.miniMeEntity;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 72;
        titleLabelY = 12;

        inventoryLabelX = 1000;

        this.tasksButton = this.addRenderableWidget(Button.builder(Component.literal("Tasks"), button -> {
            this.taskMode = !this.taskMode;
            updateMode();
        }).bounds(leftPos + 118, topPos + 5, 50, 14).build());

        for (int i = 0; i < TASK_COUNT; i++) {
            final int row = i;
            int y = topPos + 26 + i * ROW_HEIGHT;

            toggleButtons[i] = this.addRenderableWidget(Button.builder(Component.empty(),
                    button -> sendButton(MiniMeMenu.BUTTON_TOGGLE_TASK, row)).bounds(leftPos + 12, y, 32, 20).build());
            upButtons[i] = this.addRenderableWidget(Button.builder(Component.literal("^"),
                    button -> sendButton(MiniMeMenu.BUTTON_TASK_UP, row)).bounds(leftPos + 130, y, 16, 20).build());
            downButtons[i] = this.addRenderableWidget(Button.builder(Component.literal("v"),
                    button -> sendButton(MiniMeMenu.BUTTON_TASK_DOWN, row)).bounds(leftPos + 148, y, 16, 20).build());
        }

        this.widthDownButton = this.addRenderableWidget(Button.builder(Component.literal("-"),
                button -> sendButton(MiniMeMenu.BUTTON_QUARRY_WIDTH_DOWN, 0)).bounds(leftPos + 100, topPos, 18, 20).build());
        this.widthUpButton = this.addRenderableWidget(Button.builder(Component.literal("+"),
                button -> sendButton(MiniMeMenu.BUTTON_QUARRY_WIDTH_UP, 0)).bounds(leftPos + 146, topPos, 18, 20).build());

        this.areaButton = this.addRenderableWidget(Button.builder(Component.empty(),
                button -> sendButton(MiniMeMenu.BUTTON_TOGGLE_AREA, 0))
                .bounds(leftPos + 12, topPos + 26 + TASK_COUNT * ROW_HEIGHT + 4, 152, 20).build());

        updateMode();
    }

    private void sendButton(int baseId, int row) {
        int id = baseId;
        if (baseId != MiniMeMenu.BUTTON_TOGGLE_AREA && baseId < MiniMeMenu.BUTTON_QUARRY_WIDTH_DOWN) {
            if (row >= shownTasks.size()) return;
            id += shownTasks.get(row);
        }
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void updateMode() {
        this.tasksButton.setMessage(Component.literal(taskMode ? "Back" : "Tasks"));
        this.menu.setSlotsActive(!taskMode);
        updateRows();
    }

    private void updateRows() {
        shownTasks.clear();
        List<MiniMeEntity.TaskSetting> settings = miniMeEntity != null ? miniMeEntity.getTaskSettings() : List.of();
        for (int i = 0; i < settings.size(); i++) {
            if (miniMeEntity.isTaskApplicable(settings.get(i).task())) shownTasks.add(i);
        }

        for (int row = 0; row < TASK_COUNT; row++) {
            boolean shown = taskMode && row < shownTasks.size();
            toggleButtons[row].visible = shown;
            upButtons[row].visible = shown;
            downButtons[row].visible = shown;
            if (!shown) continue;

            boolean enabled = settings.get(shownTasks.get(row)).enabled();
            toggleButtons[row].setMessage(Component.literal(enabled ? "ON" : "OFF"));
            upButtons[row].active = row > 0;
            downButtons[row].active = row < shownTasks.size() - 1;
        }

        int nextY = topPos + 26 + shownTasks.size() * ROW_HEIGHT;

        this.showWidthRow = taskMode && miniMeEntity != null && miniMeEntity.isTaskApplicable(MiniMeTask.MINE);
        this.widthRowY = nextY;
        this.widthDownButton.visible = showWidthRow;
        this.widthUpButton.visible = showWidthRow;
        this.widthDownButton.setY(nextY);
        this.widthUpButton.setY(nextY);
        if (showWidthRow) nextY += ROW_HEIGHT;

        this.areaButton.visible = taskMode;
        this.areaButton.setY(nextY + 4);
        boolean showing = miniMeEntity != null && miniMeEntity.isShowingArea();
        this.areaButton.setMessage(Component.literal(showing ? "Show home area: ON" : "Show home area: OFF"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        if (taskMode) {
            graphics.fill(leftPos + 7, topPos + 7, leftPos + imageWidth - 7, topPos + imageHeight - 5, PANEL_COLOR);
        } else if (miniMeEntity != null) {
            extractMiniMe(graphics, leftPos + 26, topPos + 9, leftPos + 62, topPos + 63);
        }
    }

    private void extractMiniMe(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        float centerX = (x0 + x1) / 2.0F;
        float centerY = (y0 + y1) / 2.0F;
        float xAngle = (float) Math.atan((centerX - this.xMouse) / 40.0F);
        float yAngle = (float) Math.atan((centerY - this.yMouse) / 40.0F);

        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 20.0F * (float) (Math.PI / 180.0));
        rotation.mul(xRotation);

        EntityRenderer<? super MiniMeEntity, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(miniMeEntity);
        EntityRenderState renderState = renderer.createRenderState(miniMeEntity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        renderState.nameTag = null;

        if (renderState instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0F + xAngle * 20.0F;
            living.yRot = xAngle * 20.0F;
            living.xRot = -yAngle * 20.0F;
        }

        Vector3f translation = new Vector3f(0.0F, renderState.boundingBoxHeight / 2.0F + MODEL_OFFSET_Y, 0.0F);
        graphics.entity(renderState, MODEL_SIZE, translation, rotation, xRotation, x0, y0, x1, y1);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.xMouse = (float) mouseX;
        this.yMouse = (float) mouseY;
        updateRows();

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (taskMode && miniMeEntity != null) {
            List<MiniMeEntity.TaskSetting> settings = miniMeEntity.getTaskSettings();
            for (int row = 0; row < shownTasks.size(); row++) {
                int y = topPos + 26 + row * ROW_HEIGHT + 6;
                String name = (row + 1) + ". " + settings.get(shownTasks.get(row)).task().label();
                graphics.text(this.font, name, leftPos + 50, y, TEXT_COLOR, false);
            }

            if (showWidthRow) {
                graphics.text(this.font, "Quarry width", leftPos + 12, widthRowY + 6, TEXT_COLOR, false);
                String width = String.valueOf(miniMeEntity.getQuarryWidthSetting());
                graphics.text(this.font, width, leftPos + 132 - this.font.width(width) / 2, widthRowY + 6, TEXT_COLOR, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!taskMode) return super.mouseClicked(event, doubleClick);

        for (GuiEventListener child : this.children()) {
            if (child.mouseClicked(event, doubleClick)) return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return taskMode || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return taskMode || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!taskMode) return super.keyPressed(event);

        if (event.key() == 256) { // Escape leaves the panel instead of closing the whole screen
            this.taskMode = false;
            updateMode();
            return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(event)) {
            return super.keyPressed(event);
        }
        return true;
    }
}
