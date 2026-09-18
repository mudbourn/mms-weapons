package info.mudbourn.mmsweapons.throwable;

import java.util.Collection;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A melee weapon in flight.
 *
 * <p>This is vanilla's {@code ThrownTrident} rebuilt around an arbitrary item
 * rather than subclassed from it. Subclassing looked cheaper and isn't:
 * {@code ThrownTrident}'s useful constructors hard-code {@code EntityType.TRIDENT},
 * and the fields the return flight depends on — {@code dealtDamage}, the loyalty
 * accessor — are private, so every one of them would have to be reached through
 * an accessor mixin into a vanilla class. Owning the ~150 lines outright costs
 * less than owning four mixins into someone else's private state, and it is what
 * lets impact damage come from the weapon instead of the trident's flat 8.
 *
 * <p>Everything the trident enchantments hook into is preserved deliberately:
 * Loyalty rides {@code getTridentReturnToOwnerAcceleration}, Impaling rides
 * {@code EnchantmentHelper.modifyDamage}, and Channeling rides
 * {@code onHitBlock} — all generic hooks that fire for any weapon the
 * enchantment's {@code supported_items} tag accepts. Riptide never reaches this
 * class at all; it is handled item-side, where the player is launched instead of
 * the weapon.
 */
public class ThrownWeaponEntity extends AbstractArrow {

    private static final EntityDataAccessor<Byte> ID_LOYALTY =
            SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.BYTE);

    /**
     * The weapon itself, synched purely so the client can render it. Arrows keep
     * their pickup stack in save data only, which is fine when the renderer draws
     * one fixed model — ours has to draw whichever pike was thrown.
     */
    private static final EntityDataAccessor<ItemStack> ID_WEAPON =
            SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final float WATER_INERTIA = 0.99F;

    private boolean dealtDamage;
    public int clientSideReturnTickCount;

    /**
     * How hard this throw was wound up, 0..1. Server-only — it is consumed the
     * instant the weapon hits something and never needs to reach the client.
     * Defaults to full so a weapon reloaded from a save (already in flight, its
     * charge long spent) is never accidentally weakened.
     */
    private float chargeScale = 1.0F;

    public ThrownWeaponEntity(EntityType<? extends ThrownWeaponEntity> entityType, Level level) {
        super(entityType, level);
    }

    /** Matches {@code Projectile.ProjectileFactory}, which is how the throw spawns it. */
    public ThrownWeaponEntity(ServerLevel level, LivingEntity owner, ItemStack stack) {
        super(MmsThrowables.THROWN_WEAPON, owner, level, stack, null);
        this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(stack));
        this.entityData.set(ID_WEAPON, stack.copy());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ID_LOYALTY, (byte) 0);
        builder.define(ID_WEAPON, ItemStack.EMPTY);
    }

    /** Records how far the throw was drawn; scales impact damage. See {@link #onHitEntity}. */
    public void setChargeScale(float chargeScale) {
        this.chargeScale = chargeScale;
    }

    /** The stack the client should draw. Server-side callers want {@link #getWeaponItem()}. */
    public ItemStack getRenderedWeapon() {
        return this.entityData.get(ID_WEAPON);
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }

        Entity owner = this.getOwner();
        int loyalty = this.entityData.get(ID_LOYALTY);
        if (loyalty > 0 && (this.dealtDamage || this.isNoPhysics()) && owner != null) {
            if (!this.isAcceptibleReturnOwner()) {
                if (this.level() instanceof ServerLevel serverLevel && this.pickup == Pickup.ALLOWED) {
                    this.spawnAtLocation(serverLevel, this.getPickupItem(), 0.1F);
                }

                this.discard();
            } else {
                if (!(owner instanceof Player) && this.position().distanceTo(owner.getEyePosition()) < owner.getBbWidth() + 1.0) {
                    this.discard();
                    return;
                }

                this.setNoPhysics(true);
                Vec3 toOwner = owner.getEyePosition().subtract(this.position());
                this.setPosRaw(this.getX(), this.getY() + toOwner.y * 0.015 * loyalty, this.getZ());
                double pull = 0.05 * loyalty;
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add(toOwner.normalize().scale(pull)));
                if (this.clientSideReturnTickCount == 0) {
                    this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
                }

                this.clientSideReturnTickCount++;
            }
        }

        super.tick();
    }

    private boolean isAcceptibleReturnOwner() {
        Entity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }
        return !(owner instanceof ServerPlayer) || !owner.isSpectator();
    }

    @Override
    protected @Nullable EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return this.dealtDamage ? null : super.findHitEntity(from, to);
    }

    @Override
    protected Collection<EntityHitResult> findHitEntities(Vec3 from, Vec3 to) {
        EntityHitResult hit = this.findHitEntity(from, to);
        return hit != null ? List.of(hit) : List.of();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        // Scaled off the weapon rather than the trident's flat 8, so a wooden
        // pike and a netherite one are not the same projectile — and further by
        // how far the throw was drawn, up to 1.25x the weapon's melee damage.
        float damage = MmsThrowables.throwDamage(this.getWeaponItem(), this.chargeScale);
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);
        if (this.level() instanceof ServerLevel serverLevel) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), target, source, damage);
        }

        this.dealtDamage = true;
        if (target.hurtOrSimulate(source, damage)) {
            if (target.getType() == EntityType.ENDERMAN) {
                return;
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(
                        serverLevel, target, source, this.getWeaponItem(), item -> this.kill(serverLevel));
            }

            if (target instanceof LivingEntity living) {
                this.doKnockback(living, source);
                this.doPostHurtEffects(living);
            }
        }

        this.deflect(ProjectileDeflection.REVERSE, target, this.owner, false);
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.02, 0.2, 0.02));
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    /** Channeling's lightning strike hangs off this. */
    @Override
    protected void hitBlockEnchantmentEffects(ServerLevel serverLevel, BlockHitResult hitResult, ItemStack stack) {
        Vec3 impact = hitResult.getBlockPos().clampLocationWithin(hitResult.getLocation());
        EnchantmentHelper.onHitBlock(
                serverLevel,
                stack,
                this.getOwner() instanceof LivingEntity living ? living : null,
                this,
                null,
                impact,
                serverLevel.getBlockState(hitResult.getBlockPos()),
                item -> this.kill(serverLevel));
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.getPickupItemStackOrigin();
    }

    @Override
    protected boolean tryPickup(Player player) {
        return super.tryPickup(player)
                || this.isNoPhysics() && this.ownedBy(player) && player.getInventory().add(this.getPickupItem());
    }

    /**
     * Only ever reached by a save file that lost its item — a thrown weapon
     * always carries the stack it was thrown with. Empty is the honest answer;
     * inventing a replacement weapon would be worse than dropping nothing.
     */
    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public void playerTouch(Player player) {
        if (this.ownedBy(player) || this.getOwner() == null) {
            super.playerTouch(player);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.dealtDamage = input.getBooleanOr("DealtDamage", false);
        this.chargeScale = input.getFloatOr("ChargeScale", 1.0F);
        this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(this.getPickupItemStackOrigin()));
        this.entityData.set(ID_WEAPON, this.getPickupItemStackOrigin().copy());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DealtDamage", this.dealtDamage);
        output.putFloat("ChargeScale", this.chargeScale);
    }

    private byte getLoyaltyFromItem(ItemStack stack) {
        return this.level() instanceof ServerLevel serverLevel
                ? (byte) Mth.clamp(EnchantmentHelper.getTridentReturnToOwnerAcceleration(serverLevel, stack, this), 0, 127)
                : 0;
    }

    /** A Loyalty weapon is on its way home; despawning it would eat the weapon. */
    @Override
    public void tickDespawn() {
        if (this.pickup != Pickup.ALLOWED || this.entityData.get(ID_LOYALTY) <= 0) {
            super.tickDespawn();
        }
    }

    @Override
    protected float getWaterInertia() {
        return WATER_INERTIA;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }
}
