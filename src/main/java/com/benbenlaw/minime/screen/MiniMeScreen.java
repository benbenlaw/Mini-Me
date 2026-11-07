package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.network.packet.CustomizePacket;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeSetting;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MiniMeScreen extends AbstractContainerScreen<MiniMeMenu> {
    private static final Identifier TEXTURE = MiniMe.identifier( "textures/entity/gui/mini_me_inventory.png");
    private static final Identifier TASK_TEXTURE = MiniMe.identifier( "textures/gui/mini_me_tasks.png");
    private static final Identifier WRENCH_ICON = MiniMe.identifier( "wrench");
    private static final Identifier SCROLL_ICON = MiniMe.identifier( "scroll");
    private static final Identifier CUSTOMIZE_ICON = MiniMe.identifier( "customize");

    private static final int MODEL_SIZE = 44;
    private static final float MODEL_OFFSET_Y = 0.0F;

    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_KP_ENTER = 335;

    private static final int CONFIG_X = 133;
    private static final int CONFIG_Y = 8;
    private static final int CONFIG_SIZE = 16;
    private static final int CUSTOMIZE_X = CONFIG_X - CONFIG_SIZE - 1;

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

    private static final int SIDE_BUTTON_X = 8;
    private static final int BACK_X = 9;
    private static final int BACK_Y = 16;
    private static final int AREA_Y = 33;
    private static final int SIDE_BUTTON_SIZE = 16;

    private static final int TOGGLE_WIDTH = 82;
    private static final int WRENCH_X = 83;
    private static final int WRENCH_SIZE = 16;
    private static final int UP_X = 100;
    private static final int DOWN_X = 111;
    private static final int ARROW_WIDTH = 10;

    private static final int[] SLOT_X = {42, 87};
    private static final int BOX_WIDTH = 34;
    private static final int TEXT_BOX_X = 34;
    private static final int TEXT_BOX_WIDTH = LIST_WIDTH - TEXT_BOX_X;
    private static final long TEXT_HOLD_MILLIS = 500;
    private static final int TITLE_MAX_WIDTH = 41;

    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ON_COLOR = 0xFF55FF55;
    private static final int OFF_COLOR = 0xFFFF5555;

    private record Control(MiniMeSetting setting, String tooltip) {
    }

    private record Action(String text, MiniMeSetting setting, boolean toMax, String tooltip) {
    }

    private record SettingRow(String label, String separator, List<Control> controls, List<Action> actions) {
        static SettingRow of(String label, Control control) {
            return new SettingRow(label, "", List.of(control), List.of());
        }

        static SettingRow pair(String label, String separator, Control first, Control second) {
            return new SettingRow(label, separator, List.of(first, second), List.of());
        }

        static SettingRow actions(String label, Action first, Action second) {
            return new SettingRow(label, "", List.of(), List.of(first, second));
        }

        int controlAt(int slot) {
            if (controls.isEmpty()) return -1;
            if (controls.size() == 1) return slot == 1 ? 0 : -1;
            return slot;
        }

        int actionAt(int slot) {
            if (actions.isEmpty()) return -1;
            if (actions.size() == 1) return slot == 1 ? 0 : -1;
            return slot;
        }
    }

    private sealed interface Line permits TaskLine, SettingLine, CustomizeLine {
    }

    private record TaskLine(int listIndex, MiniMeTask task, boolean enabled, boolean canMoveUp, boolean canMoveDown) implements Line {
    }

    private record SettingLine(SettingRow row) implements Line {
    }

    private enum CustomizeKind {
        NAME_TAG, NAME, SKIN
    }

    private record CustomizeLine(CustomizeKind kind, boolean value) implements Line {
    }

    private static class NumberBox extends EditBox {
        NumberBox(Font font, int width, int height) {
            super(font, 0, 0, width, height, Component.empty());
            setMaxLength(4);
        }

        @Override
        public void insertText(String text) {
            super.insertText(text.replaceAll("[^0-9-]", ""));
        }
    }

    private static class TextBox extends EditBox {
        private String disallowed = "";

        TextBox(Font font, int width, int height) {
            super(font, 0, 0, width, height, Component.empty());
        }

        void setRules(int maxLength, String disallowed) {
            this.disallowed = disallowed;
            setMaxLength(maxLength);
        }

        @Override
        public void insertText(String text) {
            super.insertText(disallowed.isEmpty() ? text : text.replaceAll(disallowed, ""));
        }
    }

    private final MiniMeEntity miniMeEntity;
    private float xMouse;
    private float yMouse;

    private boolean taskMode;
    private boolean customizeMode;
    private MiniMeTask settingsTask;
    private int scroll;
    private boolean draggingScrollbar;

    private List<Line> lines = List.of();
    private final Line[] rowLines = new Line[VISIBLE_ROWS];
    private final Object[] tooltipKeys = new Object[VISIBLE_ROWS];

    private SpriteIconButton configButton;
    private SpriteIconButton customizeButton;
    private Button backButton;
    private Button areaButton;
    private String backTooltip;
    private Boolean areaShown;
    private final Button[] mainButtons = new Button[VISIBLE_ROWS];
    private final SpriteIconButton[] wrenchButtons = new SpriteIconButton[VISIBLE_ROWS];
    private final Button[] upButtons = new Button[VISIBLE_ROWS];
    private final Button[] downButtons = new Button[VISIBLE_ROWS];
    private final Button[][] actionButtons = new Button[VISIBLE_ROWS][2];
    private final NumberBox[][] settingBoxes = new NumberBox[VISIBLE_ROWS][2];
    private final MiniMeSetting[][] boxSettings = new MiniMeSetting[VISIBLE_ROWS][2];
    private final int[][] lastSeen = new int[VISIBLE_ROWS][2];
    private EditBox lastFocused;
    private final TextBox[] textBoxes = new TextBox[VISIBLE_ROWS];
    private final CustomizeKind[] textKinds = new CustomizeKind[VISIBLE_ROWS];
    private final String[] lastSeenText = new String[VISIBLE_ROWS];
    private final long[] holdUntil = new long[VISIBLE_ROWS];

    public MiniMeScreen(MiniMeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, clippedTitle(title));
        this.miniMeEntity = menu.miniMeEntity;
    }

    private static Component clippedTitle(Component title) {
        Font font = Minecraft.getInstance().font;
        String text = title.getString();
        if (font.width(text) <= TITLE_MAX_WIDTH) return title;

        return Component.literal(font.plainSubstrByWidth(text, TITLE_MAX_WIDTH - font.width("..")) + "..");
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
        return Math.max(0, lines.size() - VISIBLE_ROWS);
    }

    private boolean panelMode() {
        return taskMode || customizeMode;
    }

    @Override
    protected void init() {
        super.init();

        this.configButton = SpriteIconButton.builder(Component.literal("Jobs and settings"), button -> {
                    this.taskMode = true;
                    this.settingsTask = null;
                    this.scroll = 0;
                    updateMode();
                }, true)
                .size(CONFIG_SIZE, CONFIG_SIZE)
                .sprite(WRENCH_ICON, 12, 12)
                .withTootip()
                .build();
        this.configButton.setPosition(leftPos + CONFIG_X, topPos + CONFIG_Y);
        this.addRenderableWidget(this.configButton);

        this.customizeButton = SpriteIconButton.builder(Component.literal("Customize"), button -> {
                    this.customizeMode = true;
                    this.taskMode = false;
                    this.settingsTask = null;
                    this.scroll = 0;
                    updateMode();
                }, true)
                .size(CONFIG_SIZE, CONFIG_SIZE)
                .sprite(CUSTOMIZE_ICON, 12, 12)
                .withTootip()
                .build();
        this.customizeButton.setPosition(leftPos + CUSTOMIZE_X, topPos + CONFIG_Y);
        this.addRenderableWidget(this.customizeButton);

        this.backButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> goBack())
                .bounds(leftPos + BACK_X, topPos + BACK_Y, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE).build());

        this.areaButton = this.addRenderableWidget(Button.builder(Component.literal("[]"), button -> sendRaw(MiniMeMenu.BUTTON_TOGGLE_AREA))
                .bounds(leftPos + BACK_X, topPos + AREA_Y, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE).build());

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int r = row;

            mainButtons[row] = this.addRenderableWidget(Button.builder(Component.empty(), button -> mainClicked(r))
                    .bounds(leftPos + LIST_X, topPos, TOGGLE_WIDTH, ROW_HEIGHT).build());

            wrenchButtons[row] = SpriteIconButton.builder(Component.literal("Settings"), button -> openSettings(r), true)
                    .size(WRENCH_SIZE, ROW_HEIGHT)
                    .sprite(WRENCH_ICON, 12, 12)
                    .withTootip()
                    .build();
            this.addRenderableWidget(wrenchButtons[row]);

            upButtons[row] = this.addRenderableWidget(Button.builder(Component.literal("^"), button -> moveClicked(r, false))
                    .bounds(leftPos + LIST_X + UP_X, topPos, ARROW_WIDTH, ROW_HEIGHT).build());
            downButtons[row] = this.addRenderableWidget(Button.builder(Component.literal("v"), button -> moveClicked(r, true))
                    .bounds(leftPos + LIST_X + DOWN_X, topPos, ARROW_WIDTH, ROW_HEIGHT).build());

            TextBox text = new TextBox(this.font, TEXT_BOX_WIDTH, ROW_HEIGHT);
            text.setX(leftPos + LIST_X + TEXT_BOX_X);
            text.setY(topPos);
            textBoxes[row] = this.addRenderableWidget(text);

            Arrays.fill(lastSeen[row], Integer.MIN_VALUE);
            Arrays.fill(boxSettings[row], null);

            for (int slot = 0; slot < 2; slot++) {
                final int s = slot;

                NumberBox box = new NumberBox(this.font, BOX_WIDTH, ROW_HEIGHT);
                box.setX(leftPos + LIST_X + SLOT_X[slot]);
                box.setY(topPos);
                settingBoxes[row][slot] = this.addRenderableWidget(box);

                actionButtons[row][slot] = this.addRenderableWidget(Button.builder(Component.empty(), button -> runAction(r, s))
                        .bounds(leftPos + LIST_X + SLOT_X[slot], topPos, BOX_WIDTH, ROW_HEIGHT).build());
            }
        }

        Arrays.fill(tooltipKeys, null);
        this.backTooltip = null;
        this.areaShown = null;
        this.lastFocused = null;
        Arrays.fill(textKinds, null);
        Arrays.fill(lastSeenText, null);
        updateMode();
    }

    private void sendRaw(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void goBack() {
        if (customizeMode) {
            customizeMode = false;
            updateMode();
            return;
        }
        if (settingsTask != null) {
            settingsTask = null;
            scroll = 0;
        } else {
            taskMode = false;
        }
        updateMode();
    }

    private void mainClicked(int row) {
        switch (rowLines[row]) {
            case TaskLine task -> sendRaw(MiniMeMenu.BUTTON_TOGGLE_TASK + task.listIndex());
            case CustomizeLine custom -> {
                if (custom.kind() == CustomizeKind.NAME_TAG) sendRaw(MiniMeMenu.BUTTON_TOGGLE_NAME_TAG);
            }
            case null, default -> {
            }
        }
    }

    private void moveClicked(int row, boolean down) {
        if (rowLines[row] instanceof TaskLine task) {
            sendRaw((down ? MiniMeMenu.BUTTON_TASK_DOWN : MiniMeMenu.BUTTON_TASK_UP) + task.listIndex());
        }
    }

    private void openSettings(int row) {
        if (rowLines[row] instanceof TaskLine task && task.task().hasSettings()) {
            settingsTask = task.task();
            scroll = 0;
            updateMode();
        }
    }

    private List<SettingRow> settingsFor(MiniMeTask task) {
        if (task == MiniMeTask.MINE) {
            return List.of(
                    SettingRow.pair("Size", "x",
                            new Control(MiniMeSetting.QUARRY_SIZE_X, "Quarry size east-west (X) in blocks."),
                            new Control(MiniMeSetting.QUARRY_SIZE_Z, "Quarry size north-south (Z) in blocks.")),
                    SettingRow.pair("Offset", ",",
                            new Control(MiniMeSetting.QUARRY_OFFSET_X, "Moves the quarry east (+) or west (-) of the home block."),
                            new Control(MiniMeSetting.QUARRY_OFFSET_Z, "Moves the quarry south (+) or north (-) of the home block.")),
                    SettingRow.pair("Depth", "to",
                            new Control(MiniMeSetting.MIN_DEPTH, "First layer dug, in blocks below the home block. "
                                    + "1 is the layer just under it, 0 is the home block's own layer, negative is above it."),
                            new Control(MiniMeSetting.MAX_DEPTH, "Last layer dug, in blocks below the home block.")),
                    SettingRow.actions("Limits",
                            new Action("Min", MiniMeSetting.MIN_DEPTH, false, "Set the first layer to the highest possible: the top of the world."),
                            new Action("Max", MiniMeSetting.MAX_DEPTH, true, "Set the last layer to the lowest possible: the bottom of the world.")),
                    SettingRow.of("Dump at",
                            new Control(MiniMeSetting.DUMP_AFTER, "Go and empty into the chest after collecting this many "
                                    + "mined items (0 = only when the inventory is full). Needs the Use chest job and a chest.")));
        }

        if (task == MiniMeTask.DUMP) {
            return List.of(
                    SettingRow.pair("Size", "x",
                            new Control(MiniMeSetting.CHEST_SIZE_X, "Width (east-west, X) of the area searched for a chest."),
                            new Control(MiniMeSetting.CHEST_SIZE_Z, "Length (north-south, Z) of the area searched for a chest.")),
                    SettingRow.pair("Offset", ",",
                            new Control(MiniMeSetting.CHEST_OFFSET_X, "Moves the search area east (+) or west (-) of the home block."),
                            new Control(MiniMeSetting.CHEST_OFFSET_Z, "Moves the search area south (+) or north (-) of the home block.")));
        }

        return List.of();
    }

    private void runAction(int row, int slot) {
        if (miniMeEntity == null || !(rowLines[row] instanceof SettingLine line)) return;

        int index = line.row().actionAt(slot);
        if (index < 0) return;

        Action action = line.row().actions().get(index);
        int value = action.toMax() ? miniMeEntity.getSettingMax(action.setting()) : miniMeEntity.getSettingMin(action.setting());
        sendRaw(MiniMeMenu.settingValueButtonId(action.setting(), value));
    }

    private String withLimits(MiniMeSetting setting, String text) {
        if (setting != MiniMeSetting.MIN_DEPTH && setting != MiniMeSetting.MAX_DEPTH) return text;
        return text + " Allowed in this world: " + miniMeEntity.getSettingMin(setting) + " to " + miniMeEntity.getSettingMax(setting) + ".";
    }

    private EditBox focusedBox() {
        for (NumberBox[] row : settingBoxes) {
            for (NumberBox box : row) {
                if (box.isFocused()) return box;
            }
        }
        for (TextBox box : textBoxes) {
            if (box.isFocused()) return box;
        }
        return null;
    }

    private void commitText(int row) {
        CustomizeKind kind = textKinds[row];
        if (kind == null) return;

        int packetKind = kind == CustomizeKind.NAME ? CustomizePacket.NAME : CustomizePacket.SKIN;
        ClientPacketDistributor.sendToServer(new CustomizePacket(this.menu.containerId, packetKind, textBoxes[row].getValue()));
        holdUntil[row] = Util.getMillis() + TEXT_HOLD_MILLIS;
        lastSeenText[row] = null;
    }

    private void commit(EditBox box) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            if (textBoxes[row] == box) {
                commitText(row);
                return;
            }
        }

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int slot = 0; slot < 2; slot++) {
                if (settingBoxes[row][slot] != box || boxSettings[row][slot] == null) continue;

                MiniMeSetting setting = boxSettings[row][slot];
                try {
                    int value = miniMeEntity.clampSetting(setting, Integer.parseInt(box.getValue().trim()));
                    box.setValue(String.valueOf(value));
                    lastSeen[row][slot] = value;
                    sendRaw(MiniMeMenu.settingValueButtonId(setting, value));
                } catch (NumberFormatException e) {
                    lastSeen[row][slot] = Integer.MIN_VALUE;
                }
                return;
            }
        }
    }

    private void updateMode() {
        this.menu.setSlotsActive(!panelMode());
        updateRows();
    }

    private List<Line> buildLines(List<Integer> shownTasks) {
        List<Line> built = new ArrayList<>();

        if (customizeMode) {
            built.add(new CustomizeLine(CustomizeKind.NAME_TAG, miniMeEntity.isCustomNameVisible()));
            built.add(new CustomizeLine(CustomizeKind.NAME, false));
            built.add(new CustomizeLine(CustomizeKind.SKIN, false));
            return built;
        }

        if (settingsTask != null) {
            for (SettingRow row : settingsFor(settingsTask)) built.add(new SettingLine(row));
            return built;
        }

        List<MiniMeEntity.TaskSetting> settings = miniMeEntity.getTaskSettings();
        for (int position = 0; position < shownTasks.size(); position++) {
            int index = shownTasks.get(position);
            MiniMeEntity.TaskSetting setting = settings.get(index);
            built.add(new TaskLine(index, setting.task(), setting.enabled(), position > 0, position < shownTasks.size() - 1));
        }
        return built;
    }

    private void hideRow(int row) {
        textBoxes[row].visible = false;
        mainButtons[row].visible = false;
        wrenchButtons[row].visible = false;
        upButtons[row].visible = false;
        downButtons[row].visible = false;
        for (int slot = 0; slot < 2; slot++) {
            actionButtons[row][slot].visible = false;
            settingBoxes[row][slot].visible = false;
        }
    }

    private static Object tooltipKey(Line line) {
        return switch (line) {
            case TaskLine task -> task.task();
            case SettingLine setting -> setting.row();
            case CustomizeLine custom -> custom.kind();
        };
    }

    private void updateRows() {
        boolean hasInstructions = this.menu.hasInstructions();
        if (taskMode && (!hasInstructions || miniMeEntity == null)) {
            taskMode = false;
            settingsTask = null;
        }
        if (customizeMode && miniMeEntity == null) {
            customizeMode = false;
        }
        this.menu.setSlotsActive(!panelMode());
        if (settingsTask != null && (!taskMode || !miniMeEntity.isTaskApplicable(settingsTask))) {
            settingsTask = null;
        }

        EditBox focused = focusedBox();
        if (lastFocused != null && lastFocused != focused) commit(lastFocused);
        lastFocused = focused;

        boolean panel = panelMode();
        titleLabelX = panel ? 1000 : 72;
        titleLabelY = 12;
        inventoryLabelX = panel ? 8 : 1000;

        configButton.visible = !panel && hasInstructions;
        customizeButton.visible = !panel;
        backButton.visible = panel;
        areaButton.visible = taskMode;

        String back = settingsTask != null ? "Back to jobs" : "Back to inventory";
        if (!back.equals(backTooltip)) {
            backTooltip = back;
            backButton.setTooltip(Tooltip.create(Component.literal(back)));
        }

        boolean showing = miniMeEntity != null && miniMeEntity.isShowingArea();
        if (areaShown == null || areaShown != showing) {
            areaShown = showing;
            areaButton.setTooltip(Tooltip.create(Component.literal("Show home area: " + (showing ? "ON" : "OFF"))));
        }

        if (!panel || miniMeEntity == null) {
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                rowLines[row] = null;
                if (textBoxes[row].isFocused()) {
                    commit(textBoxes[row]);
                    textBoxes[row].setFocused(false);
                }
                textKinds[row] = null;
                hideRow(row);
                for (int slot = 0; slot < 2; slot++) {
                    if (settingBoxes[row][slot].isFocused()) {
                        commit(settingBoxes[row][slot]);
                        settingBoxes[row][slot].setFocused(false);
                    }
                    boxSettings[row][slot] = null;
                }
            }
            lines = List.of();
            return;
        }

        List<Integer> shownTasks = new ArrayList<>();
        List<MiniMeEntity.TaskSetting> settings = miniMeEntity.getTaskSettings();
        for (int i = 0; i < settings.size(); i++) {
            if (miniMeEntity.isTaskApplicable(settings.get(i).task())) shownTasks.add(i);
        }

        lines = buildLines(shownTasks);
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            Line line = index < lines.size() ? lines.get(index) : null;
            rowLines[row] = line;

            for (int slot = 0; slot < 2; slot++) {
                if (settingBoxes[row][slot].isFocused() && !(line instanceof SettingLine)) {
                    commit(settingBoxes[row][slot]);
                    settingBoxes[row][slot].setFocused(false);
                }
            }

            boolean textLine = line instanceof CustomizeLine custom && custom.kind() != CustomizeKind.NAME_TAG && custom.kind() == textKinds[row];
            if (textBoxes[row].isFocused() && !textLine) {
                commit(textBoxes[row]);
                textBoxes[row].setFocused(false);
            }
            hideRow(row);

            int y = rowY(row);
            boolean setTooltips = line != null && !Objects.equals(tooltipKeys[row], tooltipKey(line));
            if (line == null) {
                tooltipKeys[row] = null;
                for (int slot = 0; slot < 2; slot++) boxSettings[row][slot] = null;
                continue;
            }
            if (setTooltips) tooltipKeys[row] = tooltipKey(line);

            for (int slot = 0; slot < 2; slot++) {
                if (!(line instanceof SettingLine)) boxSettings[row][slot] = null;
            }
            if (!textLine) textKinds[row] = null;

            switch (line) {
                case TaskLine task -> {
                    mainButtons[row].visible = true;
                    mainButtons[row].setWidth(TOGGLE_WIDTH);
                    mainButtons[row].setMessage(Component.empty());
                    mainButtons[row].setX(leftPos + LIST_X);
                    mainButtons[row].setY(y);

                    wrenchButtons[row].visible = task.task().hasSettings();
                    wrenchButtons[row].setPosition(leftPos + LIST_X + WRENCH_X, y);

                    upButtons[row].visible = true;
                    upButtons[row].active = task.canMoveUp();
                    upButtons[row].setY(y);
                    downButtons[row].visible = true;
                    downButtons[row].active = task.canMoveDown();
                    downButtons[row].setY(y);

                    if (setTooltips) {
                        mainButtons[row].setTooltip(Tooltip.create(Component.literal("Click to turn this job on or off")));
                        upButtons[row].setTooltip(Tooltip.create(Component.literal("Higher priority")));
                        downButtons[row].setTooltip(Tooltip.create(Component.literal("Lower priority")));
                    }
                }
                case CustomizeLine custom -> {
                    if (custom.kind() == CustomizeKind.NAME_TAG) {
                        mainButtons[row].visible = true;
                        mainButtons[row].setWidth(LIST_WIDTH);
                        mainButtons[row].setMessage(Component.empty());
                        mainButtons[row].setX(leftPos + LIST_X);
                        mainButtons[row].setY(y);
                        if (setTooltips) {
                            mainButtons[row].setTooltip(Tooltip.create(Component.literal("Show or hide the name above the Mini Me")));
                        }
                    } else {
                        TextBox box = textBoxes[row];
                        if (textKinds[row] != custom.kind()) {
                            textKinds[row] = custom.kind();
                            lastSeenText[row] = null;
                            if (custom.kind() == CustomizeKind.NAME) {
                                box.setRules(MiniMeEntity.MAX_NAME_LENGTH, "");
                                box.setHint(Component.literal("Mini Me"));
                            } else {
                                box.setRules(MiniMeEntity.MAX_SKIN_NAME_LENGTH, "[^A-Za-z0-9_]");
                                box.setHint(Component.literal("Owner's skin"));
                            }
                        }

                        box.visible = true;
                        box.setX(leftPos + LIST_X + TEXT_BOX_X);
                        box.setY(y);

                        String value = custom.kind() == CustomizeKind.NAME
                                ? (miniMeEntity.hasCustomName() ? miniMeEntity.getCustomName().getString() : "")
                                : miniMeEntity.getSkinName();
                        if (!box.isFocused() && !value.equals(lastSeenText[row]) && Util.getMillis() >= holdUntil[row]) {
                            box.setValue(value);
                            lastSeenText[row] = value;
                        }

                        if (setTooltips) {
                            String tip = custom.kind() == CustomizeKind.NAME
                                    ? "Rename the Mini Me. Press Enter to apply, leave empty for the default name."
                                    : "Use the skin of any player: type their username and press Enter. Leave empty for the owner's skin.";
                            box.setTooltip(Tooltip.create(Component.literal(tip)));
                        }
                    }
                }
                case SettingLine settingLine -> {
                    SettingRow settingRow = settingLine.row();

                    for (int slot = 0; slot < 2; slot++) {
                        NumberBox box = settingBoxes[row][slot];
                        Button actionButton = actionButtons[row][slot];
                        int control = settingRow.controlAt(slot);
                        int action = settingRow.actionAt(slot);

                        actionButton.visible = action >= 0;
                        actionButton.setY(y);
                        if (action >= 0) {
                            Action shownAction = settingRow.actions().get(action);
                            actionButton.setMessage(Component.literal(shownAction.text()));
                            if (setTooltips) {
                                actionButton.setTooltip(Tooltip.create(Component.literal(withLimits(shownAction.setting(), shownAction.tooltip()))));
                            }
                        } else if (setTooltips) {
                            actionButton.setTooltip(null);
                        }

                        box.visible = control >= 0;
                        box.setY(y);
                        if (control < 0) {
                            boxSettings[row][slot] = null;
                            if (setTooltips) box.setTooltip(null);
                            continue;
                        }

                        Control shownControl = settingRow.controls().get(control);
                        if (boxSettings[row][slot] != shownControl.setting()) {
                            boxSettings[row][slot] = shownControl.setting();
                            lastSeen[row][slot] = Integer.MIN_VALUE;
                        }

                        int value = miniMeEntity.getSetting(shownControl.setting());
                        if (!box.isFocused() && value != lastSeen[row][slot]) {
                            box.setValue(String.valueOf(value));
                            lastSeen[row][slot] = value;
                        }

                        if (setTooltips) {
                            box.setTooltip(Tooltip.create(Component.literal(withLimits(shownControl.setting(), shownControl.tooltip()))));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        if (panelMode()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TASK_TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
            if (miniMeEntity != null) {
                extractMiniMe(graphics, leftPos + 26, topPos + 9, leftPos + 62, topPos + 63);
            }
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

    private Component fitted(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return Component.literal(text);
        while (text.length() > 1 && font.width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return Component.literal(text + "...");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.xMouse = (float) mouseX;
        this.yMouse = (float) mouseY;
        updateRows();

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (!panelMode() || miniMeEntity == null) return;

        String title = customizeMode ? "Customize" : settingsTask == null ? "Mini Me jobs" : settingsTask.label() + " settings";
        graphics.text(this.font, title, leftPos + 8, topPos + 6, TEXT_COLOR, false);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            Line line = rowLines[row];
            int x = leftPos + LIST_X;
            int y = rowY(row);
            int textY = y + (ROW_HEIGHT - 8) / 2 + 1;

            switch (line) {
                case TaskLine task -> {
                    String state = task.enabled() ? "ON" : "OFF";
                    int stateWidth = this.font.width(state);
                    graphics.text(this.font, fitted(task.task().label(), TOGGLE_WIDTH - stateWidth - 12), x + 4, textY, 0xFFFFFFFF, true);
                    graphics.text(this.font, state, x + TOGGLE_WIDTH - 4 - stateWidth, textY, task.enabled() ? ON_COLOR : OFF_COLOR, true);
                }
                case CustomizeLine custom -> {
                    switch (custom.kind()) {
                        case NAME_TAG -> {
                            String state = custom.value() ? "ON" : "OFF";
                            int stateWidth = this.font.width(state);
                            graphics.text(this.font, "Name tag", x + 4, textY, 0xFFFFFFFF, true);
                            graphics.text(this.font, state, x + LIST_WIDTH - 4 - stateWidth, textY, custom.value() ? ON_COLOR : OFF_COLOR, true);
                        }
                        case NAME -> graphics.text(this.font, "Name", x + 2, textY, TEXT_COLOR, false);
                        case SKIN -> graphics.text(this.font, "Skin", x + 2, textY, TEXT_COLOR, false);
                    }
                }
                case SettingLine settingLine -> {
                    SettingRow settingRow = settingLine.row();
                    graphics.text(this.font, settingRow.label(), x + 2, textY, TEXT_COLOR, false);

                    if (settingRow.controls().size() == 2) {
                        int gapCenter = x + (SLOT_X[0] + BOX_WIDTH + SLOT_X[1]) / 2;
                        graphics.text(this.font, settingRow.separator(), gapCenter - this.font.width(settingRow.separator()) / 2, textY, TEXT_COLOR, false);
                    }
                }
                case null, default -> {
                }
            }
        }

        if (taskMode && settingsTask == null && lines.isEmpty()) {
            graphics.textWithWordWrap(this.font,
                    Component.literal("No jobs yet. Write jobs onto these instructions at an Instruction Desk."),
                    leftPos + LIST_X + 2, rowY(0) + 2, LIST_WIDTH - 4, TEXT_COLOR);
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
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput input) {
        if (panelMode() && (input == ContainerInput.QUICK_MOVE || input == ContainerInput.PICKUP_ALL)) return;
        super.slotClicked(slot, slotId, mouseButton, input);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!panelMode()) return super.mouseClicked(event, doubleClick);

        EditBox focused = focusedBox();
        if (focused != null && !focused.isMouseOver(event.x(), event.y())) {
            focused.setFocused(false);
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
        if (panelMode() && mouseX >= leftPos + LIST_X && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= topPos + LIST_Y && mouseY < topPos + LIST_Y + VISIBLE_ROWS * ROW_SPACING) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        EditBox focused = focusedBox();
        if (focused != null) {
            int key = event.key();
            if (key == KEY_ESCAPE || key == KEY_ENTER || key == KEY_KP_ENTER) {
                commit(focused);
                focused.setFocused(false);
                this.setFocused(null);
                lastFocused = null;
                return true;
            }
            if (focused.keyPressed(event) || focused.canConsumeInput()) return true;
        }

        if (panelMode() && event.key() == KEY_ESCAPE) {
            goBack();
            return true;
        }
        return super.keyPressed(event);
    }
}
