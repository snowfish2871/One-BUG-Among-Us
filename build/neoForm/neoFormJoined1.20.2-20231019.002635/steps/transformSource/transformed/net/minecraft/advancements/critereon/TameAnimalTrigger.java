package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class TameAnimalTrigger extends SimpleCriterionTrigger<TameAnimalTrigger.TriggerInstance> {
   public TameAnimalTrigger.TriggerInstance createInstance(JsonObject p_286684_, Optional<ContextAwarePredicate> p_298911_, DeserializationContext p_286550_) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(p_286684_, "entity", p_286550_);
      return new TameAnimalTrigger.TriggerInstance(p_298911_, optional);
   }

   public void trigger(ServerPlayer p_68830_, Animal p_68831_) {
      LootContext lootcontext = EntityPredicate.createContext(p_68830_, p_68831_);
      this.trigger(p_68830_, p_68838_ -> p_68838_.matches(lootcontext));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> entity;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298796_, Optional<ContextAwarePredicate> p_298728_) {
         super(p_298796_);
         this.entity = p_298728_;
      }

      public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal() {
         return CriteriaTriggers.TAME_ANIMAL.createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal(EntityPredicate.Builder p_301051_) {
         return CriteriaTriggers.TAME_ANIMAL
            .createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_301051_))));
      }

      public boolean matches(LootContext p_68853_) {
         return this.entity.isEmpty() || this.entity.get().matches(p_68853_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.entity.ifPresent(p_299208_ -> jsonobject.add("entity", p_299208_.toJson()));
         return jsonobject;
      }
   }
}
