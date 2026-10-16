package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;

public class EffectsChangedTrigger extends SimpleCriterionTrigger<EffectsChangedTrigger.TriggerInstance> {
   public EffectsChangedTrigger.TriggerInstance createInstance(
      JsonObject p_286892_, Optional<ContextAwarePredicate> p_298606_, DeserializationContext p_286271_
   ) {
      Optional<MobEffectsPredicate> optional = MobEffectsPredicate.fromJson(p_286892_.get("effects"));
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(p_286892_, "source", p_286271_);
      return new EffectsChangedTrigger.TriggerInstance(p_298606_, optional, optional1);
   }

   public void trigger(ServerPlayer p_149263_, @Nullable Entity p_149264_) {
      LootContext lootcontext = p_149264_ != null ? EntityPredicate.createContext(p_149263_, p_149264_) : null;
      this.trigger(p_149263_, p_149268_ -> p_149268_.matches(p_149263_, lootcontext));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<MobEffectsPredicate> effects;
      private final Optional<ContextAwarePredicate> source;

      public TriggerInstance(Optional<ContextAwarePredicate> p_299319_, Optional<MobEffectsPredicate> p_298712_, Optional<ContextAwarePredicate> p_299215_) {
         super(p_299319_);
         this.effects = p_298712_;
         this.source = p_299215_;
      }

      public static Criterion<EffectsChangedTrigger.TriggerInstance> hasEffects(MobEffectsPredicate.Builder p_299317_) {
         return CriteriaTriggers.EFFECTS_CHANGED
            .createCriterion(new EffectsChangedTrigger.TriggerInstance(Optional.empty(), p_299317_.build(), Optional.empty()));
      }

      public static Criterion<EffectsChangedTrigger.TriggerInstance> gotEffectsFrom(EntityPredicate.Builder p_300971_) {
         return CriteriaTriggers.EFFECTS_CHANGED
            .createCriterion(
               new EffectsChangedTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(p_300971_.build())))
            );
      }

      public boolean matches(ServerPlayer p_149275_, @Nullable LootContext p_149276_) {
         if (this.effects.isPresent() && !this.effects.get().matches((LivingEntity)p_149275_)) {
            return false;
         } else {
            return !this.source.isPresent() || p_149276_ != null && this.source.get().matches(p_149276_);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.effects.ifPresent(p_299201_ -> jsonobject.add("effects", p_299201_.serializeToJson()));
         this.source.ifPresent(p_299041_ -> jsonobject.add("source", p_299041_.toJson()));
         return jsonobject;
      }
   }
}
