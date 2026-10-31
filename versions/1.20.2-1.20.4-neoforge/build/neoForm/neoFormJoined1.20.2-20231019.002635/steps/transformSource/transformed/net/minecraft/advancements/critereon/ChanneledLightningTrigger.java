package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class ChanneledLightningTrigger extends SimpleCriterionTrigger<ChanneledLightningTrigger.TriggerInstance> {
   public ChanneledLightningTrigger.TriggerInstance createInstance(
      JsonObject p_286858_, Optional<ContextAwarePredicate> p_298267_, DeserializationContext p_286562_
   ) {
      List<ContextAwarePredicate> list = EntityPredicate.fromJsonArray(p_286858_, "victims", p_286562_);
      return new ChanneledLightningTrigger.TriggerInstance(p_298267_, list);
   }

   public void trigger(ServerPlayer p_21722_, Collection<? extends Entity> p_21723_) {
      List<LootContext> list = p_21723_.stream().map(p_21720_ -> EntityPredicate.createContext(p_21722_, p_21720_)).collect(Collectors.toList());
      this.trigger(p_21722_, p_21730_ -> p_21730_.matches(list));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final List<ContextAwarePredicate> victims;

      public TriggerInstance(Optional<ContextAwarePredicate> p_299166_, List<ContextAwarePredicate> p_298268_) {
         super(p_299166_);
         this.victims = p_298268_;
      }

      public static Criterion<ChanneledLightningTrigger.TriggerInstance> channeledLightning(EntityPredicate.Builder... p_298972_) {
         return CriteriaTriggers.CHANNELED_LIGHTNING
            .createCriterion(new ChanneledLightningTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_298972_)));
      }

      public boolean matches(Collection<? extends LootContext> p_21745_) {
         for(ContextAwarePredicate contextawarepredicate : this.victims) {
            boolean flag = false;

            for(LootContext lootcontext : p_21745_) {
               if (contextawarepredicate.matches(lootcontext)) {
                  flag = true;
                  break;
               }
            }

            if (!flag) {
               return false;
            }
         }

         return true;
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.add("victims", ContextAwarePredicate.toJson(this.victims));
         return jsonobject;
      }
   }
}
