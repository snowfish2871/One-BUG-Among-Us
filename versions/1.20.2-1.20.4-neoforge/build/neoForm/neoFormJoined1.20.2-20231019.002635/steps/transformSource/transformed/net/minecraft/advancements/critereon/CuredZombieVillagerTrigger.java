package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.storage.loot.LootContext;

public class CuredZombieVillagerTrigger extends SimpleCriterionTrigger<CuredZombieVillagerTrigger.TriggerInstance> {
   public CuredZombieVillagerTrigger.TriggerInstance createInstance(
      JsonObject p_286894_, Optional<ContextAwarePredicate> p_298530_, DeserializationContext p_286477_
   ) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(p_286894_, "zombie", p_286477_);
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(p_286894_, "villager", p_286477_);
      return new CuredZombieVillagerTrigger.TriggerInstance(p_298530_, optional, optional1);
   }

   public void trigger(ServerPlayer p_24275_, Zombie p_24276_, Villager p_24277_) {
      LootContext lootcontext = EntityPredicate.createContext(p_24275_, p_24276_);
      LootContext lootcontext1 = EntityPredicate.createContext(p_24275_, p_24277_);
      this.trigger(p_24275_, p_24285_ -> p_24285_.matches(lootcontext, lootcontext1));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> zombie;
      private final Optional<ContextAwarePredicate> villager;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298995_, Optional<ContextAwarePredicate> p_298745_, Optional<ContextAwarePredicate> p_299314_) {
         super(p_298995_);
         this.zombie = p_298745_;
         this.villager = p_299314_;
      }

      public static Criterion<CuredZombieVillagerTrigger.TriggerInstance> curedZombieVillager() {
         return CriteriaTriggers.CURED_ZOMBIE_VILLAGER
            .createCriterion(new CuredZombieVillagerTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public boolean matches(LootContext p_24300_, LootContext p_24301_) {
         if (this.zombie.isPresent() && !this.zombie.get().matches(p_24300_)) {
            return false;
         } else {
            return !this.villager.isPresent() || this.villager.get().matches(p_24301_);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.zombie.ifPresent(p_299127_ -> jsonobject.add("zombie", p_299127_.toJson()));
         this.villager.ifPresent(p_298459_ -> jsonobject.add("villager", p_298459_.toJson()));
         return jsonobject;
      }
   }
}
