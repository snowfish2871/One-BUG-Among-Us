package net.minecraft.world.effect;

import net.minecraft.world.entity.LivingEntity;

class PoisonMobEffect extends MobEffect {
   protected PoisonMobEffect(MobEffectCategory p_295076_, int p_295615_) {
      super(p_295076_, p_295615_);
   }

   @Override
   public void applyEffectTick(LivingEntity p_296276_, int p_296233_) {
      super.applyEffectTick(p_296276_, p_296233_);
      if (p_296276_.getHealth() > 1.0F) {
         p_296276_.hurt(p_296276_.damageSources().magic(), 1.0F);
      }
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_295368_, int p_294232_) {
      int i = 25 >> p_294232_;
      if (i > 0) {
         return p_295368_ % i == 0;
      } else {
         return true;
      }
   }
}
