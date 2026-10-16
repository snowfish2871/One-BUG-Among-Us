package net.minecraft.world.entity.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

public class Giant extends Monster {
   public Giant(EntityType<? extends Giant> p_32788_, Level p_32789_) {
      super(p_32788_, p_32789_);
   }

   @Override
   protected float getStandingEyeHeight(Pose p_32794_, EntityDimensions p_32795_) {
      return 10.440001F;
   }

   @Override
   protected float ridingOffset(Entity p_294542_) {
      return -3.75F;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.ATTACK_DAMAGE, 50.0);
   }

   @Override
   public float getWalkTargetValue(BlockPos p_32791_, LevelReader p_32792_) {
      return p_32792_.getPathfindingCostFromLightLevels(p_32791_);
   }
}
