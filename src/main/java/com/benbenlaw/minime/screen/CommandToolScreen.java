package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.entity.AreaTarget;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import com.benbenlaw.minime.network.packet.CommandToolPacket;
import com.benbenlaw.minime.network.packet.MiniMeActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class CommandToolScreen extends Screen {
    private static final int WIDTH = 240;
    private static final int HEIGHT = 168;
    private static final int LIST_WIDTH = 104;
    private static final int ROW = 20;
    private static final int PER_PAGE = 5;
    private static final double RANGE = 64.0D;

    private static final Identifier TEXTURE = MiniMe.identifier( "textures/gui/command_tool.png");

    private static final int TEXT = 0xFF404040;
    private static final int WELL_TEXT = 0xFFFFFFFF;

    private int left;
    private int top;
    private int page;
    private UUID selected;

    public CommandToolScreen() {
        super(Component.literal("Mini Me Command Tool"));
    }

    private List<MiniMeEntity> nearby() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return List.of();

        return mc.level.getEntitiesOfClass(MiniMeEntity.class, mc.player.getBoundingBox().inflate(RANGE)).stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(mc.player)))
                .toList();
    }

    private MiniMeEntity selectedEntity(List<MiniMeEntity> all) {
        for (MiniMeEntity entity : all) {
            if (entity.getUUID().equals(selected)) return entity;
        }
        return null;
    }

    private static MiniMeTask taskOf(AreaTarget area) {
        return switch (area) {
            case QUARRY -> MiniMeTask.MINE;
            case CHEST -> MiniMeTask.DUMP;
        };
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;

        List<MiniMeEntity> all = nearby();
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.clamp(page, 0, pages - 1);

        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= all.size()) break;

            MiniMeEntity entity = all.get(index);
            String name = entity.getName().getString();
            if (font.width(name) > LIST_WIDTH - 8) {
                name = font.plainSubstrByWidth(name, LIST_WIDTH - 8 - font.width("..")) + "..";
            }
            Button button = Button.builder(Component.literal(name), b -> {
                selected = entity.getUUID();
                rebuildWidgets();
            }).bounds(left + 8, top + 22 + i * ROW, LIST_WIDTH, ROW - 2).build();
            button.active = !entity.getUUID().equals(selected);
            this.addRenderableWidget(button);
        }

        if (pages > 1) {
            Button prev = Button.builder(Component.literal("<"), b -> {
                page--;
                rebuildWidgets();
            }).bounds(left + 8, top + 26 + PER_PAGE * ROW, 20, 16).build();
            prev.active = page > 0;
            this.addRenderableWidget(prev);

            Button next = Button.builder(Component.literal(">"), b -> {
                page++;
                rebuildWidgets();
            }).bounds(left + 8 + LIST_WIDTH - 20, top + 26 + PER_PAGE * ROW, 20, 16).build();
            next.active = page < pages - 1;
            this.addRenderableWidget(next);
        }

        MiniMeEntity entity = selectedEntity(all);
        if (entity == null) return;

        int x = left + 8 + LIST_WIDTH + 8;
        int y = top + 38;
        int width = WIDTH - (x - left) - 8;
        for (AreaTarget area : AreaTarget.values()) {
            if (!entity.isTaskApplicable(taskOf(area))) continue;

            Button button = Button.builder(Component.literal("Set " + area.label.toLowerCase()), b -> {
                ClientPacketDistributor.sendToServer(new CommandToolPacket(entity.getUUID(), area.ordinal()));
                this.onClose();
            }).bounds(x + 3, y, width - 6, 18).build();
            button.setTooltip(Tooltip.create(Component.literal("Closes this screen. Then right-click a block to centre the "
                    + area.label.toLowerCase() + " on it.")));
            this.addRenderableWidget(button);
            y += 20;
        }

        int actionY = top + 104;
        Button sit = Button.builder(Component.literal(entity.isSitting() ? "Stand up" : "Sit"), b -> sendAction(entity, MiniMeActionPacket.SIT))
                .bounds(x + 3, actionY, width - 6, 18).build();
        sit.setTooltip(Tooltip.create(Component.literal("Tell this Mini Me to sit still, pausing its jobs, or to stand up and carry on.")));
        sit.active = !entity.isCarried();
        this.addRenderableWidget(sit);

        Button carry = Button.builder(Component.literal(entity.isCarriedBy(Minecraft.getInstance().player) ? "Put down" : "Carry"),
                        b -> sendAction(entity, MiniMeActionPacket.CARRY))
                .bounds(x + 3, actionY + 20, width - 6, 18).build();
        carry.setTooltip(Tooltip.create(Component.literal("Carry this Mini Me on your shoulder. Its jobs pause until you put it down.")));
        this.addRenderableWidget(carry);
    }

    private void sendAction(MiniMeEntity entity, int action) {
        ClientPacketDistributor.sendToServer(new MiniMeActionPacket(entity.getUUID(), action));
        this.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, WIDTH, HEIGHT, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(this.font, this.title, left + 8, top + 8, TEXT, false);

        List<MiniMeEntity> all = nearby();
        int x = left + 8 + LIST_WIDTH + 8;

        if (all.isEmpty()) {
            graphics.textWithWordWrap(this.font, Component.literal("No Mini Mes within " + (int) RANGE + " blocks."),
                    left + 10, top + 26, LIST_WIDTH - 4, WELL_TEXT);
            return;
        }

        MiniMeEntity entity = selectedEntity(all);
        if (entity == null) {
            graphics.textWithWordWrap(this.font, Component.literal("Pick a Mini Me from the list."),
                    x + 2, top + 26, WIDTH - (x - left) - 12, WELL_TEXT);
            return;
        }

        graphics.text(this.font, entity.getName().getString(), x + 2, top + 24, WELL_TEXT, true);
        if (entity.getInstructionItem().isEmpty() || !hasArea(entity)) {
            graphics.textWithWordWrap(this.font, Component.literal("No jobs with an area on its instructions."),
                    x + 2, top + 40, WIDTH - (x - left) - 12, WELL_TEXT);
        }
        if (entity.isCarried() && !entity.isCarriedBy(Minecraft.getInstance().player)) {
            graphics.text(this.font, "Carried by someone", x + 2, top + 92, WELL_TEXT, true);
        }
    }

    private static boolean hasArea(MiniMeEntity entity) {
        for (AreaTarget area : AreaTarget.values()) {
            if (entity.isTaskApplicable(taskOf(area))) return true;
        }
        return false;
    }
}
