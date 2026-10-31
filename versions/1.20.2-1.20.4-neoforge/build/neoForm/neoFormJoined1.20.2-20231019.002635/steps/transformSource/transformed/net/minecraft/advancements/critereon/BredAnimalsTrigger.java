package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class BredAnimalsTrigger extends SimpleCriterionTrigger<BredAnimalsTrigger.TriggerInstance> {
   public BredAnimalsTrigger.TriggerInstance createInstance(JsonObject p_286457_, Optional<ContextAwarePredicate> p_298342_, DeserializationContext p_286849_) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(p_286457_, "parent", p_286849_);
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(p_286457_, "partner", p_286849_);
      Optional<ContextAwarePredicate> optional2 = EntityPredicate.fromJson(p_286457_, "child", p_286849_);
      return new BredAnimalsTrigger.TriggerInstance(p_298342_, optional, optional1, optional2);
   }

   public void trigger(ServerPlayer p_147279_, Animal p_147280_, Animal p_147281_, @Nullable AgeableMob p_147282_) {
      LootContext lootcontext = EntityPredicate.createContext(p_147279_, p_147280_);
      LootContext lootcontext1 = EntityPredicate.createContext(p_147279_, p_147281_);
      LootContext lootcontext2 = p_147282_ != null ? EntityPredicate.createContext(p_147279_, p_147282_) : null;
      this.trigger(p_147279_, p_18653_ -> p_18653_.matches(lootcontext, lootcontext1, lootcontext2));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> parent;
      private final Optional<ContextAwarePredicate> partner;
      private final Optional<ContextAwarePredicate> child;

      public TriggerInstance(
         Optional<ContextAwarePredicate> p_299183_,
         Optional<ContextAwarePredicate> p_298219_,
         Optional<ContextAwarePredicate> p_298908_,
         Optional<ContextAwarePredicate> p_298435_
      ) {
         super(p_299183_);
         this.parent = p_298219_;
         this.partner = p_298908_;
         this.child = p_298435_;
      }

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals() {
         return CriteriaTriggers.BRED_ANIMALS
            .createCriterion(new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals(EntityPredicate.Builder p_301285_) {
         return CriteriaTriggers.BRED_ANIMALS
            .createCriterion(
               new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(p_301285_)))
            );
      }

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals(
         Optional<EntityPredicate> p_301067_, Optional<EntityPredicate> p_301159_, Optional<EntityPredicate> p_301056_
      ) {
         return CriteriaTriggers.BRED_ANIMALS
            .createCriterion(
               new BredAnimalsTrigger.TriggerInstance(
                  Optional.empty(), EntityPredicate.wrap(p_301067_), EntityPredicate.wrap(p_301159_), EntityPredicate.wrap(p_301056_)
               )
            );
      }

      public boolean matches(LootContext p_18676_, LootContext p_18677_, @Nullable LootContext p_18678_) {
         if (!this.child.isPresent() || p_18678_ != null && this.child.get().matches(p_18678_)) {
            return matches(this.parent, p_18676_) && matches(this.partner, p_18677_) || matches(this.parent, p_18677_) && matches(this.partner, p_18676_);
         } else {
            return false;
         }
      }

      private static boolean matches(Optional<ContextAwarePredicate> p_298925_, LootContext p_298317_) {
         return p_298925_.isEmpty() || p_298925_.get().matches(p_298317_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.parent.ifPresent(p_298959_ -> jsonobject.add("parent", p_298959_.toJson()));
         this.partner.ifPresent(p_298697_ -> jsonobject.add("partner", p_298697_.toJson()));
         this.child.ifPresent(p_298357_ -> jsonobject.add("child", p_298357_.toJson()));
         return jsonobject;
      }
   }
}
