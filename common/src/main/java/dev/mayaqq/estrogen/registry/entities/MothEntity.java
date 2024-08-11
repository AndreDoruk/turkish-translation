package dev.mayaqq.estrogen.registry.entities;

import dev.mayaqq.estrogen.platform.CommonPlatform;
import dev.mayaqq.estrogen.registry.EstrogenEntities;
import dev.mayaqq.estrogen.registry.EstrogenItems;
import dev.mayaqq.estrogen.registry.EstrogenTags;
import dev.mayaqq.estrogen.registry.entities.goals.TemptByLightBlockGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public class MothEntity extends Animal implements FlyingAnimal, Shearable {

    public final AnimationState flying = new AnimationState();
    public final AnimationState idle = new AnimationState();

    private static final EntityDataAccessor<Boolean> DATA_FUZZY = SynchedEntityData.defineId(MothEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int TICKS_PER_FLAP = 2;
    public int ticksToFuzzUp = 0;

    public MothEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0f);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0f);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 16.0f);
        this.setPathfindingMalus(BlockPathTypes.FENCE, -1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        flying.start(tickCount);
        if (!this.level().isClientSide && !this.isFuzzy()) {
            if (this.level().getGameTime() % this.getTicksToFuzzUp() == 0) {
                this.setFuzzy();
                //TODO: maybe shake and make cool sound? also particles? maybe jsut make the moth spawn particles when fuzzy
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            //TODO: make upset sound :(
            this.setSheared();
        }
        return super.hurt(source, amount);
    }

        @Override
    public InteractionResult mobInteract(Player player2, InteractionHand hand) {
        ItemStack itemStack = player2.getItemInHand(hand);
        if (itemStack.is(CommonPlatform.getShearsTag())) {
            if (!this.level().isClientSide && this.readyForShearing()) {
                this.shear(SoundSource.PLAYERS);
                this.gameEvent(GameEvent.SHEAR, player2);
                itemStack.hurtAndBreak(1, player2, player -> player.broadcastBreakEvent(hand));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player2, hand);
    }

    @Override
    public void shear(SoundSource source) {
        // TODO: custom shear sound?
        this.level().playSound(null, this, SoundEvents.SHEEP_SHEAR, source, 1.0f, 1.0f);
        this.setSheared();
        int i = 1 + this.random.nextInt(3);
        for (int j = 0; j < i; ++j) {
            ItemEntity itemEntity = this.spawnAtLocation(EstrogenItems.MOTH_FUZZ.get(), 1);
            if (itemEntity == null) continue;
            itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().add((this.random.nextFloat() - this.random.nextFloat()) * 0.1f, this.random.nextFloat() * 0.05f, (this.random.nextFloat() - this.random.nextFloat()) * 0.1f));
        }
    }

    @Override
    public boolean readyForShearing() {
        return this.isAlive() && this.isFuzzy() && !this.isBaby();
    }

    public int getTicksToFuzzUp() {
        if (ticksToFuzzUp == 0) {
            ticksToFuzzUp = this.random.nextIntBetweenInclusive(12000, 36000);
        }
        return ticksToFuzzUp;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("TicksToFuzzUp", this.getTicksToFuzzUp());
        compound.putBoolean("Fuzzy", this.isFuzzy());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.ticksToFuzzUp = compound.getInt("TicksToFuzzUp");
        this.setFuzzy(compound.getBoolean("Fuzzy"));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FUZZY, false);
    }

    public boolean isFuzzy() {
        return this.entityData.get(DATA_FUZZY);
    }

    public void setFuzzy(boolean fuzzy) {
        this.entityData.set(DATA_FUZZY, fuzzy);
    }

    public void setFuzzy() {
        this.setFuzzy(true);
    }

    public void setSheared() {
        this.setFuzzy(false);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getBlockState(pos).isAir()) {
            return 10.0f;
        }
        return 0.0f;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptByLightBlockGoal(this, 1.0, 5));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.25, Ingredient.of(EstrogenTags.Items.LIGHT_EMITTERS), false));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.25, Ingredient.of(EstrogenTags.Items.LEATHER_ITEMS), false));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(9, new FloatGoal(this));
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.FLYING_SPEED, 0.6f).add(Attributes.MOVEMENT_SPEED, 0.3f).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation(this, level){

            @Override
            public boolean isStableDestination(BlockPos pos) {
                return !this.level.getBlockState(pos.below()).isAir();
            }
        };
        flyingPathNavigation.setCanOpenDoors(false);
        flyingPathNavigation.setCanFloat(false);
        flyingPathNavigation.setCanPassDoors(true);
        return flyingPathNavigation;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(EstrogenTags.Items.LEATHER_ITEMS);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {}

    @Override
    public SoundEvent getAmbientSound() {
        // TODO: maybe ambient sound?
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        // TODO: custom hurt sound?
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        // TODO: custom death sound?
        return SoundEvents.BEE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Override
    public MothEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return EstrogenEntities.MOTH.get().create(level);
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        if (this.isBaby()) {
            return dimensions.height * 0.25f;
        }
        return dimensions.height * 0.5f;
    }

    @Override
    public boolean isFlapping() {
        return this.isFlying() && this.tickCount % TICKS_PER_FLAP == 0;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    @Override
    protected void jumpInLiquid(TagKey<Fluid> fluidTag) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.01, 0.0));
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.5f * this.getEyeHeight(), this.getBbWidth() * 0.2f);
    }
}