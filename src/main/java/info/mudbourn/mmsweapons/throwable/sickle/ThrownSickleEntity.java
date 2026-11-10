package info.mudbourn.mmsweapons.throwable.sickle;

import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

// A thrown sickle: spins end over end in flight, hits once for the item's melee damage, then sticks where it lands until picked up or, with Loyalty, flies back to its thrower.
public class ThrownSickleEntity extends AbstractArrow {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
        SynchedEntityData.defineId(ThrownSickleEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Byte> DATA_LOYALTY =
        SynchedEntityData.defineId(ThrownSickleEntity.class, EntityDataSerializers.BYTE);
    // Degrees of spin added each client tick while airborne.
    private static final float SPIN_PER_TICK = 40.0F;

    private boolean dealtDamage;
    private boolean returnSoundPlayed;
    private float spin;
    private float spinO;

    public ThrownSickleEntity(EntityType<? extends ThrownSickleEntity> type, Level level) {
        super(type, level);
    }

    public ThrownSickleEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(MmsThrowables.THROWN_SICKLE, owner, level, stack, null);
        this.entityData.set(DATA_ITEM, stack.copy());
        this.entityData.set(DATA_LOYALTY, this.loyaltyFrom(stack));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_LOYALTY, (byte) 0);
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }
        int loyalty = this.entityData.get(DATA_LOYALTY);
        Entity owner = this.getOwner();
        if (loyalty > 0 && (this.dealtDamage || this.isNoPhysics()) && owner != null) {
            if (!owner.isAlive() || owner.isSpectator()) {
                if (this.level() instanceof ServerLevel serverLevel && this.pickup == Pickup.ALLOWED) {
                    this.spawnAtLocation(serverLevel, this.getPickupItem(), 0.1F);
                }
                this.discard();
                return;
            }
            this.returnToOwner(owner, loyalty);
        }
        super.tick();
        if (this.level().isClientSide()) {
            this.spinO = this.spin;
            if (!this.isInGround()) {
                this.spin += SPIN_PER_TICK;
            }
        }
    }

    private void returnToOwner(Entity owner, int loyalty) {
        this.setNoPhysics(true);
        Vec3 toOwner = owner.getEyePosition().subtract(this.position());
        this.setPosRaw(this.getX(), this.getY() + toOwner.y * 0.015 * loyalty, this.getZ());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add(toOwner.normalize().scale(0.05 * loyalty)));
        if (!this.returnSoundPlayed) {
            this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
            this.returnSoundPlayed = true;
        }
    }

    private byte loyaltyFrom(ItemStack stack) {
        if (this.level() instanceof ServerLevel serverLevel) {
            int acceleration = EnchantmentHelper.getTridentReturnToOwnerAcceleration(serverLevel, stack, this);
            return (byte) Math.clamp(acceleration, 0, 127);
        }
        return 0;
    }

    @Override
    protected boolean tryPickup(Player player) {
        return super.tryPickup(player)
            || this.isNoPhysics() && this.ownedBy(player) && player.getInventory().add(this.getPickupItem());
    }

    public ItemStack getItem() {
        return this.entityData.get(DATA_ITEM);
    }

    public boolean isAnchored() {
        return this.isInGround();
    }

    public float getSpin(float partialTick) {
        return this.spinO + (this.spin - this.spinO) * partialTick;
    }

    @Override
    protected EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return this.dealtDamage ? null : super.findHitEntity(from, to);
    }

    @Override
    protected Collection<EntityHitResult> findHitEntities(Vec3 from, Vec3 to) {
        EntityHitResult hit = this.findHitEntity(from, to);
        return hit == null ? List.of() : List.of(hit);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);
        float damage = this.thrownDamage();
        if (this.level() instanceof ServerLevel serverLevel) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), target, source, damage);
        }

        this.dealtDamage = true;
        if (target.hurtOrSimulate(source, damage) && target instanceof LivingEntity living) {
            this.doKnockback(living, source);
            this.doPostHurtEffects(living);
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    private float thrownDamage() {
        ItemAttributeModifiers modifiers = this.getItem().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return (float) modifiers.compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND);
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.getPickupItemStackOrigin();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return this.getItem().copy();
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected void tickDespawn() {
        if (this.pickup != Pickup.ALLOWED) {
            super.tickDespawn();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_ITEM, this.getPickupItemStackOrigin().copy());
        this.entityData.set(DATA_LOYALTY, this.loyaltyFrom(this.getPickupItemStackOrigin()));
        this.dealtDamage = input.getBooleanOr("DealtDamage", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DealtDamage", this.dealtDamage);
    }
}
