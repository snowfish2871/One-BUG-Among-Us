package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;

class BadOmenMobEffect extends MobEffect {
   protected BadOmenMobEffect(MobEffectCategory p_296418_, int p_296408_) {
      super(p_296418_, p_296408_);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_295828_, int p_295171_) {
      return true;
   }

   @Override
   public void applyEffectTick(LivingEntity p_296327_, int p_294357_) {
      super.applyEffectTick(p_296327_, p_294357_);
      if (p_296327_ instanceof ServerPlayer serverplayer && !p_296327_.isSpectator()) {
         ServerLevel serverlevel = serverplayer.serverLevel();
         if (serverlevel.getDifficulty() == Difficulty.PEACEFUL) {
            return;
         }

         if (serverlevel.isVillage(p_296327_.blockPosition())) {
            serverlevel.getRaids().createOrExtendRaid(serverplayer);
         }
      }
   }
}
