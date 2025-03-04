package com.eerussianguy.firmalife;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.eerussianguy.firmalife.client.FLClientEvents;
import com.eerussianguy.firmalife.client.FLClientForgeEvents;
import com.eerussianguy.firmalife.common.FLEvents;
import com.eerussianguy.firmalife.common.FLForgeEvents;
import com.eerussianguy.firmalife.common.blockentities.FLBlockEntities;
import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.eerussianguy.firmalife.common.blocks.FLFluids;
import com.eerussianguy.firmalife.common.container.FLContainerTypes;
import com.eerussianguy.firmalife.common.entities.FLEntities;
import com.eerussianguy.firmalife.common.entities.FLParticles;
import com.eerussianguy.firmalife.common.items.FLFoodTraits;
import com.eerussianguy.firmalife.common.items.FLItems;
import com.eerussianguy.firmalife.common.misc.FLEffects;
import com.eerussianguy.firmalife.common.misc.FLInteractionManager;
import com.eerussianguy.firmalife.common.misc.FLLoot;
import com.eerussianguy.firmalife.common.network.FLPackets;
import com.eerussianguy.firmalife.common.recipes.FLRecipeSerializers;
import com.eerussianguy.firmalife.common.recipes.FLRecipeTypes;
import com.eerussianguy.firmalife.common.recipes.data.FLIngredients;
import com.eerussianguy.firmalife.common.recipes.data.FLItemStackModifiers;
import com.eerussianguy.firmalife.compat.patchouli.FLPatchouliIntegration;
import com.mojang.logging.LogUtils;
import net.dries007.tfc.config.TFCConfig;
import org.slf4j.Logger;

@Mod(FirmaLife.MOD_ID)
public class FirmaLife
{
    public static final String MOD_ID = "firmalife";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FirmaLife()
    {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        FLItems.ITEMS.register(bus);
        FLBlocks.BLOCKS.register(bus);
        FLFluids.FLUIDS.register(bus);
        FLBlockEntities.BLOCK_ENTITIES.register(bus);
        FLRecipeTypes.RECIPE_TYPES.register(bus);
        FLRecipeSerializers.RECIPE_SERIALIZERS.register(bus);
        FLContainerTypes.CONTAINERS.register(bus);
        FLEffects.EFFECTS.register(bus);
        FLEntities.ENTITIES.register(bus);
        FLParticles.PARTICLE_TYPES.register(bus);
        FLLoot.registerAll(bus);

        FLPackets.init();

        bus.addListener(this::setup);

        FLEvents.init();
        FLForgeEvents.init();
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            FLClientEvents.init();
            FLClientForgeEvents.init();
        }

    }

    public void setup(FMLCommonSetupEvent event)
    {
        // Vanilla registries are not thread safe
        event.enqueueWork(() -> {
            FLInteractionManager.init();
            FLFoodTraits.init();
            FLIngredients.init();
            TFCConfig.SERVER.enablePumpkinCarving.set(false);
        });
        FLItemStackModifiers.init();
        FLPatchouliIntegration.registerMultiBlocks();
    }

}
