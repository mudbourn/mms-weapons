package info.mudbourn.mmsweapons.chainlink;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

// A Chainlink weapon in flight: flies out on its chain, then reels back to its thrower, striking whatever it passes.
public class ThrownChainEntity extends Projectile {

    private static final EntityDataAccessor<ItemStack> DATA_WEAPON = SynchedEntityData.defineId(ThrownChainEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_REELING = SynchedEntityData.defineId(ThrownChainEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double GRAVITY = 0.04;
    private static final double REEL_SPEED = 1.6;
    private static final double CATCH_DISTANCE = 1.5;
    private static final int MAX_LIFETIME = 200;

    private final Set<Integer> struck = new HashSet<>();
    private ItemStack weapon = ItemStack.EMPTY;
    private float charge;
    private double maxRange;

    public ThrownChainEntity(EntityType<? extends ThrownChainEntity> type, Level level) {
        super(type, level);
    }

    public ThrownChainEntity(Level level, LivingEntity owner, ItemStack weapon, float charge) {
        this(Chainlink.THROWN_CHAIN, level);
        this.setOwner(owner);
        this.weapon = weapon;
        this.entityData.set(DATA_WEAPON, weapon.copyWithCount(1));
        this.charge = charge;
        this.maxRange = 8.0 + 16.0 * charge;
        Vec3 look = owner.getLookAngle();
        this.setPos(owner.getEyePosition().add(look.scale(0.5)).subtract(0.0, 0.3, 0.0));
        this.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0F, 1.0F + 1.6F * charge, 0.5F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WEAPON, ItemStack.EMPTY);
        builder.define(DATA_REELING, false);
    }

    // The thrown weapon as the client sees it, for drawing.
    public ItemStack weaponForRender() {
        return this.entityData.get(DATA_WEAPON);
    }

    public boolean isReeling() {
        return this.entityData.get(DATA_REELING);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = this.getOwner();
        if (!this.level().isClientSide()) {
            if (!(owner instanceof LivingEntity living) || !living.isAlive() || owner.level() != this.level() || this.tickCount > MAX_LIFETIME) {
                this.finish(owner);
                return;
            }
            if (!this.isReeling() && (this.distanceTo(owner) > this.maxRange || this.tickCount > 30)) {
                this.startReeling();
            }
        }
        if (this.isReeling()) {
            this.tickReeling(owner);
        } else {
            this.tickFlying();
        }
    }

    private void tickFlying() {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -GRAVITY, 0.0));
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !this.level().isClientSide()) {
            this.onHit(hit);
        }
        this.setPos(this.position().add(this.getDeltaMovement()));
    }

    private void tickReeling(Entity owner) {
        this.noPhysics = true;
        if (owner == null) {
            return;
        }
        Vec3 target = owner.getEyePosition().subtract(0.0, 0.4, 0.0);
        Vec3 toOwner = target.subtract(this.position());
        if (!this.level().isClientSide() && toOwner.length() < CATCH_DISTANCE) {
            this.finish(owner);
            return;
        }
        this.setDeltaMovement(toOwner.normalize().scale(Math.min(REEL_SPEED, toOwner.length())));
        this.setPos(this.position().add(this.getDeltaMovement()));
        if (!this.level().isClientSide()) {
            AABB sweep = this.getBoundingBox().inflate(0.6);
            for (Entity hit : this.level().getEntities(this, sweep, this::canHitEntity)) {
                this.strike(hit, 6.0F, toOwner.normalize().scale(0.5));
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner() && !this.struck.contains(entity.getId());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        this.strike(result.getEntity(), 6.0F + 10.0F * this.charge, this.getDeltaMovement().normalize().scale(0.8));
        this.startReeling();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.playSound(SoundEvents.ANVIL_LAND, 0.5F, 0.6F);
        this.startReeling();
    }

    private void strike(Entity target, float damage, Vec3 push) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        this.struck.add(target.getId());
        Entity owner = this.getOwner();
        if (target.hurtServer(server, this.damageSources().trident(this, owner == null ? this : owner), damage)) {
            target.push(push.x, 0.25, push.z);
            this.playSound(SoundEvents.CHAIN_HIT, 1.0F, 0.7F);
        }
    }

    private void startReeling() {
        if (this.isReeling()) {
            return;
        }
        this.struck.clear();
        this.entityData.set(DATA_REELING, true);
        this.playSound(SoundEvents.CHAIN_HIT, 1.0F, 1.2F);
    }

    private void finish(Entity owner) {
        this.weapon.remove(Chainlink.CHAIN_OUT);
        if (owner instanceof Player player) {
            ItemStack cooldownStack = this.weapon.isEmpty() ? player.getMainHandItem() : this.weapon;
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(cooldownStack));
            player.getCooldowns().addCooldown(cooldownStack, Chainlink.RETURN_COOLDOWN);
            this.level().playSound(
                null,
                owner.getX(),
                owner.getY(),
                owner.getZ(),
                SoundEvents.TRIDENT_RETURN,
                SoundSource.PLAYERS,
                1.0F,
                0.6F
            );
        }
        this.discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }
}
