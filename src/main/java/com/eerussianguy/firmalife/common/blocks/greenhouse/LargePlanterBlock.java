package com.eerussianguy.firmalife.common.blocks.greenhouse;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import com.eerussianguy.firmalife.client.FLClientHelpers;
import com.eerussianguy.firmalife.common.FLHelpers;
import com.eerussianguy.firmalife.common.blockentities.LargePlanterBlockEntity;
import com.eerussianguy.firmalife.common.blocks.FLStateProperties;
import com.eerussianguy.firmalife.common.util.Mechanics;
import com.eerussianguy.firmalife.common.util.Plantable;
import net.dries007.tfc.common.blockentities.FarmlandBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.devices.DeviceBlock;
import net.dries007.tfc.common.blocks.soil.HoeOverlayBlock;
import net.dries007.tfc.util.Fertilizer;
import net.dries007.tfc.util.Helpers;
import org.jetbrains.annotations.Nullable;

public class LargePlanterBlock extends DeviceBlock implements HoeOverlayBlock
{
    public static final BooleanProperty WATERED = FLStateProperties.WATERED;

    private static final VoxelShape LARGE_SHAPE = box(0, 0, 0, 16, 8, 16);

    public LargePlanterBlock(ExtendedProperties properties)
    {
        super(properties, InventoryRemoveBehavior.DROP);
        registerDefaultState(getStateDefinition().any().setValue(WATERED, false));
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        final ItemStack held = player.getItemInHand(hand);
        final Plantable plant = Plantable.get(held);
        final Fertilizer fertilizer = Fertilizer.get(held);
        final int slot = getUseSlot(hit, pos);
        if (level.getBlockEntity(pos) instanceof LargePlanterBlockEntity planter)
        {
            if (!held.isEmpty() && fertilizer != null)
            {
                planter.addNutrients(fertilizer);
                if (level instanceof ServerLevel server)
                {
                    Mechanics.addNutrientParticles(server, pos, fertilizer);
                }
                held.shrink(1);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            else if (plant != null)
            {
                if (plant.getPlanterType() != getPlanterType())
                {
                    player.displayClientMessage(new TranslatableComponent("firmalife.greenhouse.wrong_type").append(FLHelpers.getEnumTranslationKey(plant.getPlanterType())), true);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                if (planter.getTier() < plant.getTier())
                {
                    if (!planter.isClimateValid())
                    {
                        player.displayClientMessage(new TranslatableComponent("firmalife.greenhouse.climate_invalid"), true);
                    }
                    else
                    {
                        player.displayClientMessage(new TranslatableComponent("firmalife.greenhouse.wrong_tier"), true);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                return insertSlot(level, planter, held, player, slot);
            }
            else if (player.isShiftKeyDown() && held.isEmpty())
            {
                return takeSlot(level, planter, player, slot);
            }
        }
        return InteractionResult.PASS;
    }

    public PlanterType getPlanterType()
    {
        return PlanterType.LARGE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack); // basically a convenience thing when you place next to another planter
        for (Direction d : Helpers.DIRECTIONS)
        {
            BlockPos rel = pos.relative(d);
            if (level.getBlockEntity(rel) instanceof LargePlanterBlockEntity planter && planter.checkValid())
            {
                planter.setValid(level, rel, true, planter.getTier(), false);
            }
        }
    }

    @Override
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, List<Component> text, boolean debug)
    {
        if (!level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof LargePlanterBlockEntity planter)
        {
            BlockHitResult target = FLClientHelpers.getTargetedLocation();
            if (target == null) return;

            final int slot = getUseSlot(target, pos);
            text.add(new TranslatableComponent("firmalife.planter.growth_water", String.format("%.2f", planter.getGrowth(slot)), String.format("%.2f", planter.getWater())));
            if (planter.getGrowth(slot) >= 1)
            {
                text.add(new TranslatableComponent("tfc.tooltip.farmland.mature"));
            }
            final boolean valid = planter.checkValid();
            text.add(new TranslatableComponent(valid ? "firmalife.greenhouse.valid_block" : "firmalife.greenhouse.invalid_block"));
            if (!valid)
            {
                text.add(planter.getInvalidReason());
            }

            text.add(new TranslatableComponent("tfc.tooltip.farmland.nutrients", format(planter, FarmlandBlockEntity.NutrientType.NITROGEN), format(planter, FarmlandBlockEntity.NutrientType.PHOSPHOROUS), format(planter, FarmlandBlockEntity.NutrientType.POTASSIUM)));
        }
    }


    private String format(LargePlanterBlockEntity planter, FarmlandBlockEntity.NutrientType value)
    {
        return String.format("%.2f", planter.getNutrient(value) * 100);
    }

    protected int getUseSlot(BlockHitResult hit, BlockPos pos)
    {
        return 0;
    }

    public InteractionResult insertSlot(Level level, LargePlanterBlockEntity planter, ItemStack held, Player player, int slot)
    {
        return planter.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).map(inv -> {
            var res = InteractionResult.PASS;
            if (inv.getStackInSlot(slot).isEmpty())
            {
                res = FLHelpers.insertOne(level, held, slot, inv, player);
                if (res.consumesAction())
                {
                    planter.setGrowth(slot, 0);
                    planter.updateCache();
                }
            }
            return res;
        }).orElse(InteractionResult.PASS);
    }

    public InteractionResult takeSlot(Level level, LargePlanterBlockEntity planter, Player player, int slot)
    {
        return planter.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).map(inv -> {
            Plantable plant = planter.getPlantable(slot);
            if (plant != null && planter.getGrowth(slot) >= 1)
            {
                if (resetGrowthTo() == 0)
                {
                    inv.extractItem(slot, 1, false); // discard the internal ingredient
                }
                final int seedAmount = level.random.nextFloat() < plant.getExtraSeedChance() ? 2 : 1;
                ItemStack seed = plant.getSeed();
                if (!seed.isEmpty())
                {
                    seed.setCount(seedAmount);
                    ItemHandlerHelper.giveItemToPlayer(player, seed);
                }
                ItemHandlerHelper.giveItemToPlayer(player, plant.getCrop());
                planter.setGrowth(slot, resetGrowthTo());
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }).orElse(InteractionResult.PASS);
    }

    protected float resetGrowthTo()
    {
        return 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(WATERED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return LARGE_SHAPE;
    }
}
