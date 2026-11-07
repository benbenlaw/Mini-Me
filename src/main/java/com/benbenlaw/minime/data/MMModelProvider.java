package com.benbenlaw.minime.data;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.MMBlocks;
import com.benbenlaw.minime.compat.neosync.ConstructorSpecial;
import com.benbenlaw.minime.compat.neosync.NeoSyncLoadedProperty;
import com.benbenlaw.minime.item.MMItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.resources.Identifier;
import java.util.Optional;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.data.PackOutput;

public class MMModelProvider extends ModelProvider {
    public MMModelProvider(PackOutput output) {
        super(output, MiniMe.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createTrivialBlock(MMBlocks.INSTRUCTION_DESK.get(), TexturedModel.CUBE_TOP_BOTTOM);
        blockModels.createTrivialBlock(MMBlocks.MINI_ME_FACTORY.get(), TexturedModel.CUBE_TOP_BOTTOM);
        blockModels.createTrivialBlock(MMBlocks.MINI_ME_CONSTRUCTOR.get(), TexturedModel.CUBE_TOP_BOTTOM);
        Identifier constructorModel = ModelLocationUtils.getModelLocation(MMBlocks.MINI_ME_CONSTRUCTOR.get());
        itemModels.itemModelOutput.accept(MMBlocks.MINI_ME_CONSTRUCTOR.get().asItem(), new ConditionalItemModel.Unbaked(
                Optional.empty(), new NeoSyncLoadedProperty(),
                new SpecialModelWrapper.Unbaked(constructorModel, Optional.empty(), new ConstructorSpecial()),
                ItemModelUtils.plainModel(constructorModel)));

        itemModels.generateFlatItem(MMItems.INSTRUCTIONS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(MMItems.COMMAND_TOOL.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
    }
}
