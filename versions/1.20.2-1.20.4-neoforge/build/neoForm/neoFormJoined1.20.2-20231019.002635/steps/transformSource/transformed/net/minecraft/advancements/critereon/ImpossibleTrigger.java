package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.server.PlayerAdvancements;

public class ImpossibleTrigger implements CriterionTrigger<ImpossibleTrigger.TriggerInstance> {
   @Override
   public void addPlayerListener(PlayerAdvancements p_41565_, CriterionTrigger.Listener<ImpossibleTrigger.TriggerInstance> p_41566_) {
   }

   @Override
   public void removePlayerListener(PlayerAdvancements p_41572_, CriterionTrigger.Listener<ImpossibleTrigger.TriggerInstance> p_41573_) {
   }

   @Override
   public void removePlayerListeners(PlayerAdvancements p_41563_) {
   }

   public ImpossibleTrigger.TriggerInstance createInstance(JsonObject p_41569_, DeserializationContext p_41570_) {
      return new ImpossibleTrigger.TriggerInstance();
   }

   public static class TriggerInstance implements CriterionTriggerInstance {
      @Override
      public JsonObject serializeToJson() {
         return new JsonObject();
      }
   }
}
