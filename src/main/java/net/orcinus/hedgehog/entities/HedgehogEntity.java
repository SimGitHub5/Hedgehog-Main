package net.orcinus.hedgehog.entities;

import com.google.common.collect.Maps;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogAfraidOfSkullGoal;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogBegGoal;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogBreedGoal;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogEatSpiderEyeGoal;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogFollowOwnerGoal;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogLookAtPlayerGoal;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogMeleeAttackGoal;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogNearestAttackableTargetGoal;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogOwnerHurtByTargetGoal;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogOwnerHurtTargetGoal;
import net.orcinus.hedgehog.entities.ai.hedgehog.HedgehogRandomLookAroundGal;
import net.orcinus.hedgehog.init.HEntities;
import net.orcinus.hedgehog.init.HItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HedgehogEntity extends TamableAnimal implements NeutralMob {
    private static final EntityDataAccessor<Integer> ANGER_TIME = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BAND_COLOR = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SCARED_TICKS = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> POTION_TICKS = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ASSISTANCE_TICKS = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ANOINTED = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_INSTANTANEOUS = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_INTERESTED = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FED_WITH_KIWI = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private int snifingTicks;
    @Nullable
    private UUID persistentAngerTarget;
    private float interestedAngle;
    private float interestedAngleO;
    private Potion potion = Potions.EMPTY;
    private static final Map<DyeColor, Item> ITEM_BY_DYE = Util.make(Maps.newEnumMap(DyeColor.class), (map) -> {
        map.put(DyeColor.WHITE, Items.WHITE_WOOL);
        map.put(DyeColor.ORANGE, Items.ORANGE_WOOL);
        map.put(DyeColor.MAGENTA, Items.MAGENTA_WOOL);
        map.put(DyeColor.LIGHT_BLUE, Items.LIGHT_BLUE_WOOL);
        map.put(DyeColor.YELLOW, Items.YELLOW_WOOL);
        map.put(DyeColor.LIME, Items.LIME_WOOL);
        map.put(DyeColor.PINK, Items.PINK_WOOL);
        map.put(DyeColor.GRAY, Items.GRAY_WOOL);
        map.put(DyeColor.LIGHT_GRAY, Items.LIGHT_GRAY_WOOL);
        map.put(DyeColor.CYAN, Items.CYAN_WOOL);
        map.put(DyeColor.PURPLE, Items.PURPLE_WOOL);
        map.put(DyeColor.BLUE, Items.BLUE_WOOL);
        map.put(DyeColor.BROWN, Items.BROWN_WOOL);
        map.put(DyeColor.GREEN, Items.GREEN_WOOL);
        map.put(DyeColor.RED, Items.RED_WOOL);
        map.put(DyeColor.BLACK, Items.BLACK_WOOL);
    });

    public HedgehogEntity(EntityType<? extends HedgehogEntity> type, Level world) {
        super(type, world);
        this.lookControl = new HedgehogLookControl(this);
        this.moveControl = new HedgehogMoveControl(this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HedgehogAfraidOfSkullGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new HedgehogEatSpiderEyeGoal(this));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new HedgehogMeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new HedgehogFollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(7, new HedgehogBreedGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(9, new HedgehogBegGoal(this, 8.0F));
        this.goalSelector.addGoal(10, new HedgehogLookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new HedgehogRandomLookAroundGal(this));
        this.targetSelector.addGoal(1, new HedgehogOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HedgehogOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this, Fox.class)).setAlertOthers());
        this.targetSelector.addGoal(4, new HedgehogNearestAttackableTargetGoal<>(this, Spider.class, false));
        this.targetSelector.addGoal(5, new HedgehogNearestAttackableTargetGoal<>(this, CaveSpider.class, false));
        this.targetSelector.addGoal(6, new ResetUniversalAngerTargetGoal<>(this, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BAND_COLOR, -1);
        this.entityData.define(POTION_TICKS, 0);
        this.entityData.define(ANGER_TIME, 0);
        this.entityData.define(SCARED_TICKS, 0);
        this.entityData.define(ASSISTANCE_TICKS, 0);
        this.entityData.define(IS_INTERESTED, false);
        this.entityData.define(IS_INSTANTANEOUS, false);
        this.entityData.define(ANOINTED, false);
        this.entityData.define(IS_FED_WITH_KIWI, false);
    }

    @Override
    public SoundEvent getEatingSound(ItemStack stack) {
        return SoundEvents.FOX_EAT;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    public DyeColor getBandColor() {
        int i = this.entityData.get(BAND_COLOR);
        return i == -1 ? null : DyeColor.byId(i);
    }

    public void setBandColor(DyeColor color) {
        this.entityData.set(BAND_COLOR, color == null ? -1 : color.getId());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0.2F).add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getBandColor() != null) {
            tag.putByte("BandColor", (byte) this.getBandColor().getId());
        }
        tag.putString("Potion", Registry.POTION.toString());
        tag.putBoolean("Anointed", this.isAnointed());
        tag.putInt("ScaredTicks", this.getScaredTicks());
        tag.putInt("PotionTicks", this.getPotionTicks());
        tag.putInt("AssistanceTicks", this.getAssistanceTicks());
        tag.putBoolean("instantaneous", this.isInstantaneous());
        this.addPersistentAngerSaveData(tag);
    }

    public int getAssistanceTicks() {
        return this.entityData.get(ASSISTANCE_TICKS);
    }

    public void setAssistanceTicks(int assistanceTicks) {
        this.entityData.set(ASSISTANCE_TICKS, assistanceTicks);
    }

    public boolean isInstantaneous() {
        return this.entityData.get(IS_INSTANTANEOUS);
    }

    public void setIsInstantaneous(boolean instantaneous) {
        this.entityData.set(IS_INSTANTANEOUS, instantaneous);
    }

    public boolean hasPotion() {
        return this.potion != Potions.EMPTY;
    }

    public void setPotion(Potion potion) {
        this.potion = potion;
    }

    public boolean isAnointed() {
        return this.entityData.get(ANOINTED);
    }

    public void setAnointed(boolean anointed) {
        this.entityData.set(ANOINTED, anointed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setScaredTicks(tag.getInt("ScaredTicks"));
        this.setAnointed(tag.getBoolean("Anointed"));
        this.setIsInstantaneous(tag.getBoolean("Instantaneous"));
        this.readPersistentAngerSaveData(this.level, tag);
        this.setPotionTicks(tag.getInt("PotionTicks"));
        this.setAssistanceTicks(tag.getInt("AssistanceTicks"));
        if (tag.contains("BandColor", 99)) {
            this.setBandColor(DyeColor.byId(tag.getInt("BandColor")));
        }
        this.setPotion(PotionUtils.getPotion(tag));
    }

    public int getSnifingTicks() {
        return this.snifingTicks;
    }

    public void setSnifingTicks(int snifingTicks) {
        this.snifingTicks = snifingTicks;
    }

    public int getPotionTicks() {
        return this.entityData.get(POTION_TICKS);
    }

    public void setPotionTicks(int potionTick) {
        this.entityData.set(POTION_TICKS, potionTick);
    }

    public int getScaredTicks() {
        return this.entityData.get(SCARED_TICKS);
    }

    public void setScaredTicks(int scaredTicks) {
        this.entityData.set(SCARED_TICKS, scaredTicks);
    }

    public void setIsInterested(boolean isInterested) {
        this.entityData.set(IS_INTERESTED, isInterested);
    }

    public boolean isInterested() {
        return this.entityData.get(IS_INTERESTED);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        MobEffect effect = effectInstance.getEffect();
        if (effect == MobEffects.POISON) {
            return false;
        }
        return super.canBeAffected(effectInstance);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.hasPotion()) {
            this.level.broadcastEntityEvent(this, (byte)8);
        }
        if (this.isAnointed()) {
            this.level.broadcastEntityEvent(this, (byte)9);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        List<Player> players = this.level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(32.0D));
        for (Player player : players) {
            if (player.isAlive()) {
                TranslatableComponent component = new TranslatableComponent("The Potion Ticks are " + this.getPotionTicks());
                player.sendMessage(component, player.getUUID());
            }
        }
        List<Fox> foxes = this.level.getEntitiesOfClass(Fox.class, this.getBoundingBox().inflate(3.0D));
        List<LivingEntity> closestLivings = this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6D), (entity -> {
            if (entity instanceof Player) {
                return !((Player) entity).getAbilities().instabuild;
            }
            return !entity.isSpectator();
        }));
        for (Fox nearbyFoxes : foxes) {
            if (nearbyFoxes.isAlive()) {
                this.setScaredTicks(300);
            }
        }
        for (LivingEntity nearbyMobs : closestLivings) {
            if (nearbyMobs.isAlive() && this.getScaredTicks() > 0) {
                if (!(nearbyMobs instanceof HedgehogEntity)) {
                    if (nearbyMobs instanceof TamableAnimal) {
                        if (((TamableAnimal) nearbyMobs).isTame()) continue;
                    }
                    if (this.hasPotion()) {
                        if (!this.level.isClientSide()) {
                            for (MobEffectInstance mobEffectInstance : this.potion.getEffects()) {
                                MobEffect effect = mobEffectInstance.getEffect();
                                if (!nearbyMobs.canBeAffected(mobEffectInstance)) continue;
                                if (nearbyMobs.hasEffect(effect)) continue;
                                if (effect.isInstantenous() && this.isInstantaneous()) {
                                    this.setIsInstantaneous(false);
                                    effect.applyInstantenousEffect(null, null, nearbyMobs, mobEffectInstance.getAmplifier(), 1.0D);
                                } else {
                                    nearbyMobs.addEffect(mobEffectInstance);
                                }
                            }
                        }
                    }
                    nearbyMobs.hurt(DamageSource.mobAttack(this), 2);
                    this.jumping = false;
                    this.navigation.stop();
                    this.setTarget(null);
                }
            }
        }
        if (this.getAssistanceTicks() > 0) {
            this.setAssistanceTicks(this.getAssistanceTicks() - 1);
            this.setScaredTicks(0);
        }
        if (this.getPotionTicks() > 0) {
            this.setPotionTicks(this.getPotionTicks() - 1);
        }
        if (this.getPotionTicks() == 0 && !this.isInstantaneous()) {
            this.setPotion(Potions.EMPTY);
        }
        if (random.nextInt(15) == 0) {
            if (random.nextBoolean()) {
                this.setSnifingTicks(20);
            }
        }
        if (this.getSnifingTicks() > 0) {
            this.setSnifingTicks(this.getSnifingTicks() - 1);
        }
        if (this.getScaredTicks() > 0) {
            this.jumping = false;
            this.navigation.stop();
            this.setTarget(null);
            this.setScaredTicks(this.getScaredTicks() - 1);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAlive()) {
            this.interestedAngleO = this.interestedAngle;
            if (this.isInterested()) {
                this.interestedAngle += (1.0F - this.interestedAngle) * 0.4F;
            } else {
                this.interestedAngle += (0.0F - this.interestedAngle) * 0.4F;
            }
        }
    }

    public float getHeadRollAngle(float angle) {
        return Mth.lerp(angle, this.interestedAngleO, this.interestedAngle) * 0.15F * (float)Math.PI;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
//        ItemStack stack = player.getItemInHand(hand);
//        if (this.level.isClientSide) {
//            boolean flag = this.isOwnedBy(player) || this.isTame() || stack.is(Items.APPLE) || stack.is(HItems.KIWI.get()) && !this.isTame();
//            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
//        } else {
//            if (stack.getItem() == Items.MILK_BUCKET) {
//                this.kill();
//                ItemStack itemstack1 = ItemUtils.createFilledResult(stack, player, Items.BUCKET.getDefaultInstance());
//                player.setItemInHand(hand, itemstack1);
//                return InteractionResult.SUCCESS;
//            }
//            else if (stack.is(Items.SPIDER_EYE)&& !this.hasPotion() && !this.isAnointed() && this.getScaredTicks() == 0 && !this.isBaby()) {
//                Vec3 vec3 = new Vec3(((double) random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, ((double) random.nextFloat() - 0.5D) * 0.1D);
//                vec3 = vec3.xRot(-this.getXRot() * ((float)Math.PI / 180F));
//                vec3 = vec3.yRot(-this.getYRot() * ((float)Math.PI / 180F));
//                double d0 = (double)(-random.nextFloat()) * 0.6D - 0.3D;
//                Vec3 vec31 = new Vec3(((double) random.nextFloat() - 0.5D) * 0.8D, d0, 1.0D + ((double) random.nextFloat() - 0.5D) * 0.4D);
//                vec31 = vec31.add(this.getX(), this.getEyeY(), this.getZ());
//                ((ServerLevel)this.level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPIDER_EYE)), vec31.x, vec31.y, vec31.z, 1, vec3.x, vec3.y + 0.05D, vec3.z, 0.5F);
//                this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
//                this.setAnointed(true);
//                if (!player.getAbilities().instabuild) {
//                    stack.shrink(1);
//                }
//                return InteractionResult.SUCCESS;
//            }
//            else if (this.potion == Potions.EMPTY && stack.getItem() instanceof PotionItem && this.isAnointed() && !this.isBaby()) {
//                Potion potion = PotionUtils.getPotion(stack);
//                List<MobEffectInstance> instance = potion.getEffects();
//                for (MobEffectInstance effectInstance : instance) {
//                    if (!effectInstance.getEffect().isInstantenous()) {
//                        this.setPotionTicks(effectInstance.getDuration());
//                    } else {
//                        this.setIsInstantaneous(true);
//                    }
//                }
//                this.setPotion(potion);
//                this.setAnointed(false);
//                stack.shrink(1);
//                if (stack.isEmpty()) {
//                    player.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
//                }  else if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
//                    player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
//                }
//                return InteractionResult.SUCCESS;
//            }
//            if (this.isTame()) {
//                if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
//                    if (!player.getAbilities().instabuild) {
//                        stack.shrink(1);
//                    }
//                    this.heal((float)stack.getItem().getFoodProperties().getNutrition());
//                    this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
//                    return InteractionResult.SUCCESS;
//                }
//                if (!(stack.getItem() instanceof DyeItem || stack.getItem() instanceof BlockItem)) {
//                    InteractionResult result = super.mobInteract(player, hand);
//                    if ((!result.consumesAction() || this.isBaby()) && this.isOwnedBy(player)) {
//                        this.setOrderedToSit(!this.isOrderedToSit());
//                        this.jumping = false;
//                        this.navigation.stop();
//                        this.setTarget(null);
//                        return InteractionResult.SUCCESS;
//                    }
//                }
//                for (DyeColor dyeColor : ITEM_BY_DYE.keySet()) {
//                    Item item = ITEM_BY_DYE.get(dyeColor);
//                    if (stack.getItem() == item) {
//                        this.setBandColor(dyeColor);
//                        if (!player.getAbilities().instabuild) {
//                            stack.shrink(1);
//                        }
//                        return InteractionResult.SUCCESS;
//                    }
//                }
//                DyeColor dyecolor = ((DyeItem)stack.getItem()).getDyeColor();
//                if (dyecolor != this.getBandColor()) {
//                    if (this.getBandColor().getId() != -1) {
//                        this.setBandColor(dyecolor);
//                        if (!player.getAbilities().instabuild) {
//                            stack.shrink(1);
//                        }
//                        return InteractionResult.SUCCESS;
//                    }
//                }
//            }
//            else if (this.getScaredTicks() == 0 && (stack.is(HItems.KIWI.get()) || stack.is(Items.APPLE))) {
//                if (!player.getAbilities().instabuild) {
//                    stack.shrink(1);
//                }
//                if (this.random.nextInt(3) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)) {
//                    this.tame(player);
//                    this.navigation.stop();
//                    this.setTarget(null);
//                    this.setOrderedToSit(true);
//                    this.level.broadcastEntityEvent(this, (byte)7);
//                } else {
//                    this.level.broadcastEntityEvent(this, (byte)6);
//                }
//                return InteractionResult.SUCCESS;
//            }
//            return super.mobInteract(player, hand);
//        }
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (this.level.isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isTame() || itemstack.is(Items.BONE) && !this.isTame() && !this.isAngry();
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else {
            if (itemstack.is(Items.MILK_BUCKET)) {
                this.kill();
                ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, player, Items.BUCKET.getDefaultInstance());
                player.setItemInHand(hand, itemstack1);
                return InteractionResult.SUCCESS;
            }
            if (itemstack.is(Items.SPIDER_EYE) && this.getScaredTicks() == 0 && !this.isBaby() && !this.hasPotion() && !this.isAnointed()) {
                for (int i = 0; i < UniformInt.of(40, 80).sample(random); i++){
                    Vec3 vec3 = new Vec3(((double) random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, ((double) random.nextFloat() - 0.5D) * 0.1D);
                    vec3 = vec3.xRot(-this.getXRot() * ((float) Math.PI / 180F));
                    vec3 = vec3.yRot(-this.getYRot() * ((float) Math.PI / 180F));
                    double d0 = (double) (-random.nextFloat()) * 0.6D - 0.3D;
                    Vec3 vec31 = new Vec3(((double) random.nextFloat() - 0.5D) * 0.8D, d0, 1.0D + ((double) random.nextFloat() - 0.5D) * 0.4D);
                    vec31 = vec31.add(this.getX(), this.getEyeY(), this.getZ());
                    ((ServerLevel) this.level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPIDER_EYE)), vec31.x, vec31.y, vec31.z, 1, vec3.x, vec3.y + 0.05D, vec3.z, 0.5F);
                }
                this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                this.setAnointed(true);
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            if (!this.isBaby() && itemstack.getItem() instanceof PotionItem && this.isAnointed() && !this.hasPotion()) {
                Potion potion = PotionUtils.getPotion(itemstack);
                List<MobEffectInstance> instance = potion.getEffects();
                for (MobEffectInstance effectInstance : instance) {
                    if (!effectInstance.getEffect().isInstantenous()) {
                        this.setPotionTicks(effectInstance.getDuration());
                    } else {
                        this.setIsInstantaneous(true);
                    }
                }
                this.setPotion(potion);
                this.setAnointed(false);
                itemstack.shrink(1);
                if (!player.getAbilities().instabuild) {
                    if (itemstack.isEmpty()) {
                        player.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
                    } else if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                        player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (this.isTame()) {
                if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                    if (itemstack.is(HItems.KIWI.get())) {
                        this.setAssistanceTicks(1200);
                    }
                    if (!player.getAbilities().instabuild) {
                        itemstack.shrink(1);
                    }

                    this.heal((float)item.getFoodProperties().getNutrition());
                    this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
                    return InteractionResult.SUCCESS;
                }
                for (DyeColor dyeColor : ITEM_BY_DYE.keySet()) {
                    Item dyeItem = ITEM_BY_DYE.get(dyeColor);
                    if (itemstack.getItem() == dyeItem) {
                        this.setBandColor(dyeColor);
                        if (!player.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
                if (!(item instanceof DyeItem)) {
                    InteractionResult interactionresult = super.mobInteract(player, hand);
                    if ((!interactionresult.consumesAction() || this.isBaby()) && this.isOwnedBy(player)) {
                        this.setOrderedToSit(!this.isOrderedToSit());
                        this.jumping = false;
                        this.navigation.stop();
                        this.setTarget((LivingEntity)null);
                        return InteractionResult.SUCCESS;
                    }

                    return interactionresult;
                }
                DyeColor dyecolor = ((DyeItem)item).getDyeColor();
                if (dyecolor != this.getBandColor()) {
                    if (this.getBandColor().getId() != -1) {
                        this.setBandColor(dyecolor);
                        if (!player.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                    }

                    return InteractionResult.SUCCESS;
                }
            } else if ((itemstack.is(HItems.KIWI.get()) || itemstack.is(Items.APPLE)) && !this.isAngry() && this.getScaredTicks() == 0) {
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }

                if (this.random.nextInt(3) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget((LivingEntity)null);
                    this.setOrderedToSit(true);
                    this.level.broadcastEntityEvent(this, (byte)7);
                } else {
                    this.level.broadcastEntityEvent(this, (byte)6);
                }

                return InteractionResult.SUCCESS;
            }

            return super.mobInteract(player, hand);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == 8) {
            int i = PotionUtils.getColor(potion);
            double d0 = (double) (i >> 16 & 255) / 255.0D;
            double d1 = (double) (i >> 8 & 255) / 255.0D;
            double d2 = (double) (i & 255) / 255.0D;
            this.level.addParticle(ParticleTypes.ENTITY_EFFECT, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), d0, d1, d2);
        }
        if (id == 9) {
            if (random.nextInt(15) == 0) {
                for (int k = 0; k < UniformInt.of(1, 2).sample(this.getRandom()); k++) {
                    this.level.addParticle(ParticleTypes.SNOWFLAKE, this.eyeBlockPosition().getX() + 0.5D, (this.eyeBlockPosition().getY()) + 0.8D, this.eyeBlockPosition().getZ() + 0.5D, (Mth.randomBetween(random, -1.0F, 1.0F) * 0.083333336F), 0.05F, (Mth.randomBetween(random, -1.0F, 1.0F) * 0.083333336F));
                }
            }
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.APPLE || stack.getItem() == HItems.KIWI.get();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob mob) {
        HedgehogEntity hedgehog = HEntities.HEDGEHOG.get().create(world);
        UUID uuid = this.getOwnerUUID();
        if (uuid != null) {
            hedgehog.setOwnerUUID(uuid);
            hedgehog.setTame(true);
        }
        return hedgehog;
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(ANGER_TIME);
    }

    @Override
    public void setRemainingPersistentAngerTime(int time) {
        this.entityData.set(ANGER_TIME, time);
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID uuid) {
        this.persistentAngerTarget = uuid;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public boolean canMate(Animal animal) {
        if (animal == this) {
            return false;
        } else if (!this.isTame()) {
            return false;
        } else if (!(animal instanceof HedgehogEntity)) {
            return false;
        } else {
            HedgehogEntity hedgehog = (HedgehogEntity)animal;
            if (!hedgehog.isTame()) {
                return false;
            } else if (hedgehog.isInSittingPose()) {
                return false;
            } else {
                return this.isInLove() && hedgehog.isInLove();
            }
        }
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity entity) {
        if (!(target instanceof Creeper) && !(target instanceof Ghast)) {
            if (target instanceof HedgehogEntity) {
                HedgehogEntity hedgehog = (HedgehogEntity)target;
                return !hedgehog.isTame() || hedgehog.getOwner() != entity;
            } else if (target instanceof Player && entity instanceof Player && !((Player)entity).canHarmPlayer((Player)target)) {
                return false;
            } else if (target instanceof AbstractHorse && ((AbstractHorse)target).isTamed()) {
                return false;
            } else {
                return !(target instanceof TamableAnimal) || !((TamableAnimal)target).isTame();
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean flag = entity.hurt(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (flag) {
            this.doEnchantDamageEffects(this, entity);
        }
        return flag;
    }

    @Override
    public void setTame(boolean tamed) {
        super.setTame(tamed);
        if (tamed) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D);
            this.setHealth(20.0F);
        } else {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0D);
        }
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0D);
    }

    public static class HedgehogMoveControl extends MoveControl {
        private final HedgehogEntity entity;

        public HedgehogMoveControl(HedgehogEntity mob) {
            super(mob);
            this.entity = mob;
        }

        @Override
        public void tick() {
            if (this.entity.getScaredTicks() == 0) {
                super.tick();
            }
        }
    }

    public static class HedgehogLookControl extends LookControl {
        private final HedgehogEntity entity;

        public HedgehogLookControl(HedgehogEntity entity) {
            super(entity);
            this.entity = entity;
        }

        @Override
        public void tick() {
            if (this.entity.getScaredTicks() == 0) {
                super.tick();
            }
        }
    }

}