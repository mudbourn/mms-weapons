package info.mudbourn.mmsweapons.mixin.aerialcompat;

import fr.factionbedrock.aerialhell.Item.Ability.MiningUseSituationInfo;
import fr.factionbedrock.aerialhell.Item.AerialHellItem;
import fr.factionbedrock.aerialhell.Util.FieldAccessor;
import fr.factionbedrock.aerialhell.Util.MutableFloat;
import info.mudbourn.mmsweapons.aerial.AerialCompat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Mining abilities of the Aerial Hell original, run for the held compat item.
@Mixin(Player.class)
public abstract class PlayerAerialCompatMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void mmsWeapons$aerialMiningAbility(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        ItemStack held = self.getMainHandItem();
        AerialHellItem original = AerialCompat.abilities(held.getItem());
        if (original != null) {
            MutableFloat speed = new MutableFloat(cir.getReturnValueF());
            original.onMining(held, self, new MiningUseSituationInfo(state, new FieldAccessor<>(speed::get, speed::set)));
            cir.setReturnValue(speed.get());
        }
    }
}
