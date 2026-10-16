package net.minecraft.world.effect;

import net.minecraft.world.entity.LivingEntity;

class RegenerationMobEffect extends MobEffect {
   protected RegenerationMobEffect(MobEffectCategory p_296242_, int p_294288_) {
      super(p_296242_, p_294288_);
   }

   @Override
   public void applyEffectTick(LivingEntity p_295924_, int p_296417_) {
      super.applyEffectTick(p_295924_, p_296417_);
      if (p_295924_.getHealth() < p_295924_.getMaxHealth()) {
         p_295924_.heal(1.0F);
      }
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_295946_, int p_295536_) {
      int i = 50 >> p_295536_;
      if (i > 0) {
         return p_295946_ % i == 0;
      } else {
         return true;
      }
   }
}
