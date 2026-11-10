package info.mudbourn.mmsweapons.client.throwable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsweapons.throwable.ThrownWeaponEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Draws a thrown weapon as its own item model, oriented along its flight path.
 *
 * <p>The model is resolved in {@link ItemDisplayContext#NONE}. That choice is
 * load-bearing for Basic Weapons: its client item definitions switch on display
 * context, mapping {@code gui}/{@code ground}/{@code fixed}/{@code on_shelf} to
 * the flat sprite and everything else to the 3D held model. {@code NONE} lands
 * in the "everything else" fallback and carries no display transform of its own,
 * so we get the real geometry in a clean frame rather than a model pre-rotated
 * for someone's hand.
 *
 * <p>The orientation constants below are the part that has to be checked by
 * eye — the two flight rotations are vanilla's, but the correction that turns
 * "item model as authored" into "point-first along travel" depends on which axis
 * a given model was built along, and no amount of reading the JSON substitutes
 * for watching one fly.
 */
@Environment(EnvType.CLIENT)
public class ThrownWeaponRenderer extends EntityRenderer<ThrownWeaponEntity, ThrownWeaponRenderState> {

    /**
     * Item models are authored pointing away from the viewer along -Z; the
     * flight rotations below expect the weapon to lie along -Y. This is the
     * quarter turn between the two.
     */
    private static final float MODEL_PITCH_CORRECTION = -90.0F;

    /** Rolls the blade flat-side-out so a thin spearhead isn't seen edge-on. */
    private static final float MODEL_ROLL_CORRECTION = 0.0F;

    /** Expanded Weaponry hammers are modelled head-up; this flips them so the head leads along the flight path, as Hurl's mace does. */
    private static final float HEAVY_MODEL_PITCH_CORRECTION = -90.0F;

    /** How far, in blocks, a stuck hammer is drawn back out of the block it hit. */
    private static final float HEAVY_GROUND_PULLBACK = 0.45F;

    /** Turns a hammer about its handle so the head stands upright in the arc of an overhand throw. */
    private static final float HEAVY_MODEL_ROLL_CORRECTION = 90.0F;

    private final ItemModelResolver itemModelResolver;

    public ThrownWeaponRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public ThrownWeaponRenderState createRenderState() {
        return new ThrownWeaponRenderState();
    }

    @Override
    public void extractRenderState(ThrownWeaponEntity entity, ThrownWeaponRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot(partialTick);
        state.xRot = entity.getXRot(partialTick);
        state.heavy = entity.isHeavy();
        state.inGround = entity.stuckInGround();
        this.itemModelResolver.updateForNonLiving(
                state.item, entity.getRenderedWeapon(), ItemDisplayContext.NONE, entity);
    }

    @Override
    public void submit(ThrownWeaponRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.item.isEmpty()) {
            poseStack.pushPose();
            if (state.heavy && state.inGround) {
                // A stuck projectile stops at the block face with the model centred on it, which buries
                // half a hammer. Pull the model back along its flight path so the head sticks out.
                float yaw = state.yRot * Mth.DEG_TO_RAD;
                float pitch = state.xRot * Mth.DEG_TO_RAD;
                float cos = Mth.cos(pitch);
                poseStack.translate(
                        -Mth.sin(yaw) * cos * HEAVY_GROUND_PULLBACK,
                        -Mth.sin(pitch) * HEAVY_GROUND_PULLBACK,
                        -Mth.cos(yaw) * cos * HEAVY_GROUND_PULLBACK);
            }
            // Vanilla's projectile alignment, verbatim from ThrownTridentRenderer.
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot + 90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(MODEL_PITCH_CORRECTION));
            if (MODEL_ROLL_CORRECTION != 0.0F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(MODEL_ROLL_CORRECTION));
            }
            if (state.heavy) {
                poseStack.mulPose(Axis.XP.rotationDegrees(HEAVY_MODEL_PITCH_CORRECTION));
                poseStack.mulPose(Axis.YP.rotationDegrees(HEAVY_MODEL_ROLL_CORRECTION));
            }
            state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
