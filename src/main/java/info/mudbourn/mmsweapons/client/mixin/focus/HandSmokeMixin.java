package info.mudbourn.mmsweapons.client.mixin.focus;

import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmsweapons.client.focus.FocusSmokeClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Marks the main-hand weapon while the hand layer draws it, for the focus smoke.
@Mixin(ItemInHandLayer.class)
public abstract class HandSmokeMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void mmsWeapons$beginSmoke(ArmedEntityRenderState state, ItemStackRenderState itemState, ItemStack stack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (arm == state.mainArm) {
            FocusSmokeClient.beginHand(stack);
        } else {
            FocusSmokeClient.endHand();
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void mmsWeapons$endSmoke(ArmedEntityRenderState state, ItemStackRenderState itemState, ItemStack stack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        FocusSmokeClient.endHand();
    }
}
