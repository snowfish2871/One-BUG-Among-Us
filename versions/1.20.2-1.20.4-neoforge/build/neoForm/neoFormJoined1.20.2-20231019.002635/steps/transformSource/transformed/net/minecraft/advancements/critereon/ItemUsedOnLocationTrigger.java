package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

public class ItemUsedOnLocationTrigger extends SimpleCriterionTrigger<ItemUsedOnLocationTrigger.TriggerInstance> {
   public ItemUsedOnLocationTrigger.TriggerInstance createInstance(
      JsonObject p_286301_, Optional<ContextAwarePredicate> p_299015_, DeserializationContext p_286322_
   ) {
      Optional<Optional<ContextAwarePredicate>> optional = ContextAwarePredicate.fromElement(
         "location", p_286322_, p_286301_.get("location"), LootContextParamSets.ADVANCEMENT_LOCATION
      );
      if (optional.isEmpty()) {
         throw new JsonParseException("Failed to parse 'location' field");
      } else {
         return new ItemUsedOnLocationTrigger.TriggerInstance(p_299015_, optional.get());
      }
   }

   public void trigger(ServerPlayer p_286813_, BlockPos p_286625_, ItemStack p_286620_) {
      ServerLevel serverlevel = p_286813_.serverLevel();
      BlockState blockstate = serverlevel.getBlockState(p_286625_);
      LootParams lootparams = new LootParams.Builder(serverlevel)
         .withParameter(LootContextParams.ORIGIN, p_286625_.getCenter())
         .withParameter(LootContextParams.THIS_ENTITY, p_286813_)
         .withParameter(LootContextParams.BLOCK_STATE, blockstate)
         .withParameter(LootContextParams.TOOL, p_286620_)
         .create(LootContextParamSets.ADVANCEMENT_LOCATION);
      LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
      this.trigger(p_286813_, p_286596_ -> p_286596_.matches(lootcontext));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> location;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298789_, Optional<ContextAwarePredicate> p_298316_) {
         super(p_298789_);
         this.location = p_298316_;
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(Block p_301158_) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_301158_).build()
         );
         return CriteriaTriggers.PLACED_BLOCK
            .createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(LootItemCondition.Builder... p_301013_) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            Arrays.stream(p_301013_).map(LootItemCondition.Builder::build).toArray(p_286827_ -> new LootItemCondition[p_286827_])
         );
         return CriteriaTriggers.PLACED_BLOCK
            .createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
      }

      private static ItemUsedOnLocationTrigger.TriggerInstance itemUsedOnLocation(LocationPredicate.Builder p_286740_, ItemPredicate.Builder p_286777_) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            LocationCheck.checkLocation(p_286740_).build(), MatchTool.toolMatches(p_286777_).build()
         );
         return new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> itemUsedOnBlock(LocationPredicate.Builder p_286808_, ItemPredicate.Builder p_286486_) {
         return CriteriaTriggers.ITEM_USED_ON_BLOCK.createCriterion(itemUsedOnLocation(p_286808_, p_286486_));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> allayDropItemOnBlock(
         LocationPredicate.Builder p_286325_, ItemPredicate.Builder p_286531_
      ) {
         return CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.createCriterion(itemUsedOnLocation(p_286325_, p_286531_));
      }

      public boolean matches(LootContext p_286800_) {
         return this.location.isEmpty() || this.location.get().matches(p_286800_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.location.ifPresent(p_297906_ -> jsonobject.add("location", p_297906_.toJson()));
         return jsonobject;
      }
   }
}
