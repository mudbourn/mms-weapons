package info.mudbourn.mmsweapons.mixin.focus;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import info.mudbourn.mmsweapons.focus.Barrage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

// Drops explosion knockback, both the server push and the vector sent to the client, on a player using a focus barrage.
@Mixin(ServerExplosion.class)
public abstract class FocusExplosionMixin {

    @WrapWithCondition(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pushFromExplosion(Lnet/minecraft/world/phys/Vec3;)V"))
    private boolean mmsWeapons$holdGround(Entity entity, Vec3 push) {
        return !Barrage.knockbackImmune(entity);
    }

    @WrapWithCondition(method = "hurtEntities", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private boolean mmsWeapons$skipClientPush(Map<Object, Object> hitPlayers, Object player, Object push) {
        return !Barrage.knockbackImmune((Entity) player);
    }
}
