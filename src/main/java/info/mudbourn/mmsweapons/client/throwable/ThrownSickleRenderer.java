package info.mudbourn.mmsweapons.client.throwable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsweapons.throwable.sickle.ThrownSickleEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

// Draws a thrown sickle as its item model, aimed along its flight and spun end over end until it lands.
public class ThrownSickleRenderer extends EntityRenderer<ThrownSickleEntity, ThrownSickleRenderer.State> {

    // Spin a stuck sickle is drawn at: blade buried along its flight, handle tipped up out of the ground.
    private static final float ANCHOR_SPIN = -155.0F;
    // Blocks a stuck sickle is pulled back out of the ground along its flight.
    private static final float ANCHOR_PULLBACK = 0.15F;

    private final ItemModelResolver itemModelResolver;

    public ThrownSickleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ThrownSickleEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot(partialTick);
        state.xRot = entity.getXRot(partialTick);
        state.anchored = entity.isAnchored();
        state.spin = state.anchored ? ANCHOR_SPIN : entity.getSpin(partialTick);
        this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.FIXED, entity);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
        if (state.anchored) {
            poseStack.translate(-ANCHOR_PULLBACK, 0.0F, 0.0F);
        }
        poseStack.mulPose(Axis.ZN.rotationDegrees(state.spin));
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    // Per-frame snapshot of a thrown sickle.
    public static class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float yRot;
        public float xRot;
        public float spin;
        public boolean anchored;
    }
}
