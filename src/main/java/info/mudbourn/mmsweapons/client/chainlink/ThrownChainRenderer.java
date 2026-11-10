package info.mudbourn.mmsweapons.client.chainlink;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsweapons.chainlink.ThrownChainEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

// Draws a thrown Chainlink weapon and the iron chain linking it back to its thrower's hand.
public class ThrownChainRenderer extends EntityRenderer<ThrownChainEntity, ThrownChainRenderer.State> {

    private static final float LINK_LENGTH = 0.5F;
    private static final float ANCHOR_SCALE = 0.6F;

    private final ItemModelResolver itemModels;
    private final ItemStack chainStack = new ItemStack(Items.IRON_CHAIN);

    public ThrownChainRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModels = context.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(ThrownChainEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void extractRenderState(ThrownChainEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.itemModels.updateForNonLiving(state.anchor, entity.weaponForRender(), ItemDisplayContext.NONE, entity);
        this.itemModels.updateForNonLiving(state.chain, this.chainStack, ItemDisplayContext.NONE, entity);
        Entity owner = entity.getOwner();
        state.toHand = owner instanceof LivingEntity living ? this.handPosition(living, partialTick).subtract(entity.getPosition(partialTick)) : null;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Vec3 outward = state.toHand == null ? new Vec3(0.0, -1.0, 0.0) : state.toHand.scale(-1.0);
        Quaternionf facing = new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) outward.x, (float) outward.y, (float) outward.z);

        poseStack.pushPose();
        poseStack.mulPose(facing);
        poseStack.scale(ANCHOR_SCALE, ANCHOR_SCALE, ANCHOR_SCALE);
        state.anchor.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();

        if (state.toHand != null) {
            this.submitChain(state, poseStack, collector);
        }
        super.submit(state, poseStack, collector, camera);
    }

    private void submitChain(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        double length = state.toHand.length();
        int links = (int) Math.ceil(length / LINK_LENGTH);
        Vec3 dir = state.toHand.normalize();
        Quaternionf along = new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) dir.x, (float) dir.y, (float) dir.z);
        for (int i = 0; i < links; i++) {
            Vec3 at = dir.scale((i + 0.5) * length / links);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(along);
            poseStack.mulPose(Axis.YP.rotationDegrees(i % 2 == 0 ? 0.0F : 90.0F));
            poseStack.scale(LINK_LENGTH, LINK_LENGTH, LINK_LENGTH);
            state.chain.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
            poseStack.popPose();
        }
    }

    private Vec3 handPosition(LivingEntity owner, float partialTick) {
        int side = owner.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        Minecraft minecraft = Minecraft.getInstance();
        if (owner == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            double fovScale = 960.0 / minecraft.options.fov().get();
            Vec3 onScreen = this.entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(side * 0.525F, -0.45F).scale(fovScale);
            return owner.getEyePosition(partialTick).add(onScreen);
        }
        float yaw = Mth.lerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        double scale = owner.getScale();
        double lateral = side * 0.37 * scale;
        double forward = 0.45 * scale;
        double drop = (owner.isCrouching() ? 0.1875 : 0.0) + 0.7 * scale;
        return owner.getEyePosition(partialTick).add(
            -cos * lateral - sin * forward,
            -drop,
            -sin * lateral + cos * forward
        );
    }

    // Render state carrying the anchor and chain models and the offset to the thrower's hand.
    public static class State extends EntityRenderState {
        final ItemStackRenderState anchor = new ItemStackRenderState();
        final ItemStackRenderState chain = new ItemStackRenderState();
        Vec3 toHand;
    }
}
