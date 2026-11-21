package info.mudbourn.mmsweapons.mixin.headshot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Temporary: logs whether a projectile reaching a player was deflected before its hit ran.
@Mixin(Projectile.class)
public abstract class ProjectileDeflectDebugMixin {

    @Unique
    private static final Logger LOG = LoggerFactory.getLogger("mms_weapons/arrowdebug");

    @Inject(method = "hitTargetOrDeflectSelf", at = @At("RETURN"))
    private void mmsWeapons$debugDeflect(HitResult hit, CallbackInfoReturnable<ProjectileDeflection> cir) {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Player player) {
            LOG.info(
                "projectile {} reached {}: deflection={} side={}",
                ((Projectile) (Object) this).getType().toShortString(),
                player.getName().getString(),
                cir.getReturnValue(),
                player.level().isClientSide() ? "client" : "server"
            );
        }
    }
}
