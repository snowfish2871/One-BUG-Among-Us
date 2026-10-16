package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class FishingRodHookedTrigger extends SimpleCriterionTrigger<FishingRodHookedTrigger.TriggerInstance> {
   public FishingRodHookedTrigger.TriggerInstance createInstance(
      JsonObject p_286600_, Optional<ContextAwarePredicate> p_299021_, DeserializationContext p_286299_
   ) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(p_286600_.get("rod"));
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(p_286600_, "entity", p_286299_);
      Optional<ItemPredicate> optional2 = ItemPredicate.fromJson(p_286600_.get("item"));
      return new FishingRodHookedTrigger.TriggerInstance(p_299021_, optional, optional1, optional2);
   }

   public void trigger(ServerPlayer p_40417_, ItemStack p_40418_, FishingHook p_40419_, Collection<ItemStack> p_40420_) {
      LootContext lootcontext = EntityPredicate.createContext(p_40417_, (Entity)(p_40419_.getHookedIn() != null ? p_40419_.getHookedIn() : p_40419_));
      this.trigger(p_40417_, p_40425_ -> p_40425_.matches(p_40418_, lootcontext, p_40420_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> rod;
      private final Optional<ContextAwarePredicate> entity;
      private final Optional<ItemPredicate> item;

      public TriggerInstance(
         Optional<ContextAwarePredicate> p_299190_,
         Optional<ItemPredicate> p_298599_,
         Optional<ContextAwarePredicate> p_298872_,
         Optional<ItemPredicate> p_299028_
      ) {
         super(p_299190_);
         this.rod = p_298599_;
         this.entity = p_298872_;
         this.item = p_299028_;
      }

      public static Criterion<FishingRodHookedTrigger.TriggerInstance> fishedItem(
         Optional<ItemPredicate> p_298233_, Optional<EntityPredicate> p_298660_, Optional<ItemPredicate> p_298847_
      ) {
         return CriteriaTriggers.FISHING_ROD_HOOKED
            .createCriterion(new FishingRodHookedTrigger.TriggerInstance(Optional.empty(), p_298233_, EntityPredicate.wrap(p_298660_), p_298847_));
      }

      public boolean matches(ItemStack p_40444_, LootContext p_40445_, Collection<ItemStack> p_40446_) {
         if (this.rod.isPresent() && !this.rod.get().matches(p_40444_)) {
            return false;
         } else if (this.entity.isPresent() && !this.entity.get().matches(p_40445_)) {
            return false;
         } else {
            if (this.item.isPresent()) {
               boolean flag = false;
               Entity entity = p_40445_.getParamOrNull(LootContextParams.THIS_ENTITY);
               if (entity instanceof ItemEntity itementity && this.item.get().matches(itementity.getItem())) {
                  flag = true;
               }

               for(ItemStack itemstack : p_40446_) {
                  if (this.item.get().matches(itemstack)) {
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
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.rod.ifPresent(p_298887_ -> jsonobject.add("rod", p_298887_.serializeToJson()));
         this.entity.ifPresent(p_298482_ -> jsonobject.add("entity", p_298482_.toJson()));
         this.item.ifPresent(p_298358_ -> jsonobject.add("item", p_298358_.serializeToJson()));
         return jsonobject;
      }
   }
}
