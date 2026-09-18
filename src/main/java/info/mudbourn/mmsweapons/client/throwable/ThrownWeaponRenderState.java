package info.mudbourn.mmsweapons.client.throwable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Render state for a weapon in flight.
 *
 * <p>Unlike the trident's, this carries a whole {@link ItemStackRenderState}:
 * the trident renderer draws one fixed model, while this one draws whichever
 * weapon was thrown. The state instance is owned per entity, which is what keeps
 * the resolved model from being shared between two pikes in the air at once.
 */
@Environment(EnvType.CLIENT)
public class ThrownWeaponRenderState extends EntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public float xRot;
    public float yRot;
}
