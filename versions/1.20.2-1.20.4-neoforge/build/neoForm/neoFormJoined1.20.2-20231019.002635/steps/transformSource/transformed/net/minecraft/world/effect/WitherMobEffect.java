package net.minecraft.world.effect;

import net.minecraft.world.entity.LivingEntity;

class WitherMobEffect extends MobEffect {
   protected WitherMobEffect(MobEffectCategory p_295600_, int p_294141_) {
      super(p_295600_, p_294141_);
   }

   @Override
   public void applyEffectTick(LivingEntity p_296279_, int p_294798_) {
      super.applyEffectTick(p_296279_, p_294798_);
      p_296279_.hurt(p_296279_.damageSources().wither(), 1.0F);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_295629_, int p_295734_) {
      int i = 40 >> p_295734_;
      if (i > 0) {
         return p_295629_ % i == 0;
      } else {
         return true;
      }
   }
}
