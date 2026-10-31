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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BeeNestDestroyedTrigger extends SimpleCriterionTrigger<BeeNestDestroyedTrigger.TriggerInstance> {
   public BeeNestDestroyedTrigger.TriggerInstance createInstance(
      JsonObject p_286717_, Optional<ContextAwarePredicate> p_298838_, DeserializationContext p_286840_
   ) {
      Block block = deserializeBlock(p_286717_);
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(p_286717_.get("item"));
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(p_286717_.get("num_bees_inside"));
      return new BeeNestDestroyedTrigger.TriggerInstance(p_298838_, block, optional, minmaxbounds$ints);
   }

   @Nullable
   private static Block deserializeBlock(JsonObject p_17488_) {
      if (p_17488_.has("block")) {
         ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(p_17488_, "block"));
         return BuiltInRegistries.BLOCK
            .getOptional(resourcelocation)
            .orElseThrow(() -> new JsonSyntaxException("Unknown block type '" + resourcelocation + "'"));
      } else {
         return null;
      }
   }

   public void trigger(ServerPlayer p_146652_, BlockState p_146653_, ItemStack p_146654_, int p_146655_) {
      this.trigger(p_146652_, p_146660_ -> p_146660_.matches(p_146653_, p_146654_, p_146655_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      @Nullable
      private final Block block;
      private final Optional<ItemPredicate> item;
      private final MinMaxBounds.Ints numBees;

      public TriggerInstance(
         Optional<ContextAwarePredicate> p_298811_, @Nullable Block p_286264_, Optional<ItemPredicate> p_299115_, MinMaxBounds.Ints p_286574_
      ) {
         super(p_298811_);
         this.block = p_286264_;
         this.item = p_299115_;
         this.numBees = p_286574_;
      }

      public static Criterion<BeeNestDestroyedTrigger.TriggerInstance> destroyedBeeNest(
         Block p_17513_, ItemPredicate.Builder p_17514_, MinMaxBounds.Ints p_17515_
      ) {
         return CriteriaTriggers.BEE_NEST_DESTROYED
            .createCriterion(new BeeNestDestroyedTrigger.TriggerInstance(Optional.empty(), p_17513_, Optional.of(p_17514_.build()), p_17515_));
      }

      public boolean matches(BlockState p_146662_, ItemStack p_146663_, int p_146664_) {
         if (this.block != null && !p_146662_.is(this.block)) {
            return false;
         } else {
            return this.item.isPresent() && !this.item.get().matches(p_146663_) ? false : this.numBees.matches(p_146664_);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         if (this.block != null) {
            jsonobject.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.block).toString());
         }

         this.item.ifPresent(p_298961_ -> jsonobject.add("item", p_298961_.serializeToJson()));
         jsonobject.add("num_bees_inside", this.numBees.serializeToJson());
         return jsonobject;
      }
   }
}
