package net.minecraft.world.entity.monster;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public abstract class AbstractIllager extends Raider {
   protected AbstractIllager(EntityType<? extends AbstractIllager> p_32105_, Level p_32106_) {
      super(p_32105_, p_32106_);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
   }

   @Override
   public MobType getMobType() {
      return MobType.ILLAGER;
   }

   public AbstractIllager.IllagerArmPose getArmPose() {
      return AbstractIllager.IllagerArmPose.CROSSED;
   }

   @Override
   public boolean canAttack(LivingEntity p_186270_) {
      return p_186270_ instanceof AbstractVillager && p_186270_.isBaby() ? false : super.canAttack(p_186270_);
   }

   @Override
   protected float ridingOffset(Entity p_295619_) {
      return -0.6F;
   }

   @Override
   protected Vector3f getPassengerAttachmentPoint(Entity p_295963_, EntityDimensions p_295364_, float p_296196_) {
      return new Vector3f(0.0F, p_295364_.height + 0.05F * p_296196_, 0.0F);
   }

   public static enum IllagerArmPose {
      CROSSED,
      ATTACKING,
      SPELLCASTING,
      BOW_AND_ARROW,
      CROSSBOW_HOLD,
      CROSSBOW_CHARGE,
      CELEBRATING,
      NEUTRAL;
   }

   protected class RaiderOpenDoorGoal extends OpenDoorGoal {
      public RaiderOpenDoorGoal(Raider p_32128_) {
         super(p_32128_, false);
      }

      @Override
      public boolean canUse() {
         return super.canUse() && AbstractIllager.this.hasActiveRaid();
      }
   }
}
