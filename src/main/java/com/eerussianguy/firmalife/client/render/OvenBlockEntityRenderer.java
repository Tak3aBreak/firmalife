package com.eerussianguy.firmalife.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.CapabilityItemHandler;

import com.eerussianguy.firmalife.common.blockentities.OvenTopBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.dries007.tfc.client.RenderHelpers;

public class OvenBlockEntityRenderer implements BlockEntityRenderer<OvenTopBlockEntity>
{
    @Override
    public void render(OvenTopBlockEntity oven, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay)
    {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        oven.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(cap -> {
            float timeD = RenderHelpers.itemTimeRotation();
            poseStack.translate(0.25D, 0.25D, 0.25D);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            for (int i = 0; i < cap.getSlots(); i++)
            {
                ItemStack stack = cap.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                poseStack.pushPose();
                poseStack.translate((i % 2 == 0 ? 1 : 0), 0, (i < 2 ? 1 : 0));
                poseStack.mulPose(Vector3f.YP.rotationDegrees(timeD));
                itemRenderer.renderStatic(stack, ItemTransforms.TransformType.FIXED, light, overlay, poseStack, buffer, 0);
                poseStack.popPose();
            }
        });
    }
}
