package info.mudbourn.mmsweapons.client.mixin.focus;

import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmsweapons.client.focus.FocusSmokeClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Hands the focus smoke the item's model-space matrix, right after its display transform has been applied.
@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public abstract class ItemLayerSmokeMixin {

    @Inject(method = "submit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/model/ItemTransform;apply(ZLcom/mojang/blaze3d/vertex/PoseStack$Pose;)V",
            shift = At.Shift.AFTER))
    private void mmsWeapons$smokeAtModel(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outline, CallbackInfo ci) {
        FocusSmokeClient.atModel(poseStack.last().pose());
    }
}
