package net.minecraft.advancements;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record Criterion<T extends CriterionTriggerInstance>(CriterionTrigger<T> trigger, T triggerInstance) {
   public static Criterion<?> criterionFromJson(JsonObject p_11418_, DeserializationContext p_11419_) {
      ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(p_11418_, "trigger"));
      CriterionTrigger<?> criteriontrigger = CriteriaTriggers.getCriterion(resourcelocation);
      if (criteriontrigger == null) {
         throw new JsonSyntaxException("Invalid criterion trigger: " + resourcelocation);
      } else {
         return criterionFromJson(p_11418_, p_11419_, criteriontrigger);
      }
   }

   private static <T extends CriterionTriggerInstance> Criterion<T> criterionFromJson(
      JsonObject p_301046_, DeserializationContext p_301193_, CriterionTrigger<T> p_300992_
   ) {
      T t = p_300992_.createInstance(GsonHelper.getAsJsonObject(p_301046_, "conditions", new JsonObject()), p_301193_);
      return new Criterion<>(p_300992_, t);
   }

   public static Map<String, Criterion<?>> criteriaFromJson(JsonObject p_11427_, DeserializationContext p_11428_) {
      Map<String, Criterion<?>> map = Maps.newHashMap();

      for(Entry<String, JsonElement> entry : p_11427_.entrySet()) {
         map.put(entry.getKey(), criterionFromJson(GsonHelper.convertToJsonObject(entry.getValue(), "criterion"), p_11428_));
      }

      return map;
   }

   public JsonElement serializeToJson() {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("trigger", Objects.requireNonNull(CriteriaTriggers.getId(this.trigger), "Unregistered trigger").toString());
      JsonObject jsonobject1 = this.triggerInstance.serializeToJson();
      if (jsonobject1.size() != 0) {
         jsonobject.add("conditions", jsonobject1);
      }

      return jsonobject;
   }
}
