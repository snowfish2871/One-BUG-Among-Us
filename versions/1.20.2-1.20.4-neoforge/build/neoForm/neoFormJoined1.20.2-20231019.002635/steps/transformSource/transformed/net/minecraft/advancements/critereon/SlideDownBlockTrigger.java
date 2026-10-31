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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SlideDownBlockTrigger extends SimpleCriterionTrigger<SlideDownBlockTrigger.TriggerInstance> {
   public SlideDownBlockTrigger.TriggerInstance createInstance(
      JsonObject p_286879_, Optional<ContextAwarePredicate> p_298480_, DeserializationContext p_286581_
   ) {
      Block block = deserializeBlock(p_286879_);
      Optional<StatePropertiesPredicate> optional = StatePropertiesPredicate.fromJson(p_286879_.get("state"));
      if (block != null) {
         optional.ifPresent(p_297913_ -> p_297913_.checkState(block.getStateDefinition(), p_66983_ -> {
               throw new JsonSyntaxException("Block " + block + " has no property " + p_66983_);
            }));
      }

      return new SlideDownBlockTrigger.TriggerInstance(p_298480_, block, optional);
   }

   @Nullable
   private static Block deserializeBlock(JsonObject p_66988_) {
      if (p_66988_.has("block")) {
         ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(p_66988_, "block"));
         return BuiltInRegistries.BLOCK
            .getOptional(resourcelocation)
            .orElseThrow(() -> new JsonSyntaxException("Unknown block type '" + resourcelocation + "'"));
      } else {
         return null;
      }
   }

   public void trigger(ServerPlayer p_66979_, BlockState p_66980_) {
      this.trigger(p_66979_, p_66986_ -> p_66986_.matches(p_66980_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      @Nullable
      private final Block block;
      private final Optional<StatePropertiesPredicate> state;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298510_, @Nullable Block p_286622_, Optional<StatePropertiesPredicate> p_299110_) {
         super(p_298510_);
         this.block = p_286622_;
         this.state = p_299110_;
      }

      public static Criterion<SlideDownBlockTrigger.TriggerInstance> slidesDownBlock(Block p_67007_) {
         return CriteriaTriggers.HONEY_BLOCK_SLIDE.createCriterion(new SlideDownBlockTrigger.TriggerInstance(Optional.empty(), p_67007_, Optional.empty()));
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         if (this.block != null) {
            jsonobject.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.block).toString());
         }

         this.state.ifPresent(p_298384_ -> jsonobject.add("state", p_298384_.serializeToJson()));
         return jsonobject;
      }

      public boolean matches(BlockState p_67009_) {
         if (this.block != null && !p_67009_.is(this.block)) {
            return false;
         } else {
            return !this.state.isPresent() || this.state.get().matches(p_67009_);
         }
      }
   }
}
