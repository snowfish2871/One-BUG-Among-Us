package net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;

public class ContextAwarePredicate {
   private final List<LootItemCondition> conditions;
   private final Predicate<LootContext> compositePredicates;

   ContextAwarePredicate(List<LootItemCondition> p_298428_) {
      if (p_298428_.isEmpty()) {
         throw new IllegalArgumentException("ContextAwarePredicate must have at least one condition");
      } else {
         this.conditions = p_298428_;
         this.compositePredicates = LootItemConditions.andConditions(p_298428_);
      }
   }

   public static ContextAwarePredicate create(LootItemCondition... p_286844_) {
      return new ContextAwarePredicate(List.of(p_286844_));
   }

   public static Optional<Optional<ContextAwarePredicate>> fromElement(
      String p_286647_, DeserializationContext p_286323_, @Nullable JsonElement p_286520_, LootContextParamSet p_286912_
   ) {
      if (p_286520_ != null && p_286520_.isJsonArray()) {
         List<LootItemCondition> list = p_286323_.deserializeConditions(p_286520_.getAsJsonArray(), p_286323_.getAdvancementId() + "/" + p_286647_, p_286912_);
         return list.isEmpty() ? Optional.of(Optional.empty()) : Optional.of(Optional.of(new ContextAwarePredicate(list)));
      } else {
         return Optional.empty();
      }
   }

   public boolean matches(LootContext p_286260_) {
      return this.compositePredicates.test(p_286260_);
   }

   public JsonElement toJson() {
      return Util.getOrThrow(LootItemConditions.CODEC.listOf().encodeStart(JsonOps.INSTANCE, this.conditions), IllegalStateException::new);
   }

   public static JsonElement toJson(List<ContextAwarePredicate> p_298324_) {
      if (p_298324_.isEmpty()) {
         return JsonNull.INSTANCE;
      } else {
         JsonArray jsonarray = new JsonArray();

         for(ContextAwarePredicate contextawarepredicate : p_298324_) {
            jsonarray.add(contextawarepredicate.toJson());
         }

         return jsonarray;
      }
   }
}
