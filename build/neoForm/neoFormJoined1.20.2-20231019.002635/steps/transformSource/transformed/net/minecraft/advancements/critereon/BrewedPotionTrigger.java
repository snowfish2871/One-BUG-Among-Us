package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.alchemy.Potion;

public class BrewedPotionTrigger extends SimpleCriterionTrigger<BrewedPotionTrigger.TriggerInstance> {
   public BrewedPotionTrigger.TriggerInstance createInstance(JsonObject p_286606_, Optional<ContextAwarePredicate> p_299076_, DeserializationContext p_286605_) {
      Potion potion = null;
      if (p_286606_.has("potion")) {
         ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(p_286606_, "potion"));
         potion = BuiltInRegistries.POTION
            .getOptional(resourcelocation)
            .orElseThrow(() -> new JsonSyntaxException("Unknown potion '" + resourcelocation + "'"));
      }

      return new BrewedPotionTrigger.TriggerInstance(p_299076_, potion);
   }

   public void trigger(ServerPlayer p_19121_, Potion p_19122_) {
      this.trigger(p_19121_, p_19125_ -> p_19125_.matches(p_19122_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      @Nullable
      private final Potion potion;

      public TriggerInstance(Optional<ContextAwarePredicate> p_299023_, @Nullable Potion p_286830_) {
         super(p_299023_);
         this.potion = p_286830_;
      }

      public static Criterion<BrewedPotionTrigger.TriggerInstance> brewedPotion() {
         return CriteriaTriggers.BREWED_POTION.createCriterion(new BrewedPotionTrigger.TriggerInstance(Optional.empty(), null));
      }

      public boolean matches(Potion p_19142_) {
         return this.potion == null || this.potion == p_19142_;
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         if (this.potion != null) {
            jsonobject.addProperty("potion", BuiltInRegistries.POTION.getKey(this.potion).toString());
         }

         return jsonobject;
      }
   }
}
