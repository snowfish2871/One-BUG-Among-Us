package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;

public class KilledByCrossbowTrigger extends SimpleCriterionTrigger<KilledByCrossbowTrigger.TriggerInstance> {
   public KilledByCrossbowTrigger.TriggerInstance createInstance(
      JsonObject p_286238_, Optional<ContextAwarePredicate> p_298978_, DeserializationContext p_286919_
   ) {
      List<ContextAwarePredicate> list = EntityPredicate.fromJsonArray(p_286238_, "victims", p_286919_);
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(p_286238_.get("unique_entity_types"));
      return new KilledByCrossbowTrigger.TriggerInstance(p_298978_, list, minmaxbounds$ints);
   }

   public void trigger(ServerPlayer p_46872_, Collection<Entity> p_46873_) {
      List<LootContext> list = Lists.newArrayList();
      Set<EntityType<?>> set = Sets.newHashSet();

      for(Entity entity : p_46873_) {
         set.add(entity.getType());
         list.add(EntityPredicate.createContext(p_46872_, entity));
      }

      this.trigger(p_46872_, p_46881_ -> p_46881_.matches(list, set.size()));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final List<ContextAwarePredicate> victims;
      private final MinMaxBounds.Ints uniqueEntityTypes;

      public TriggerInstance(Optional<ContextAwarePredicate> p_299225_, List<ContextAwarePredicate> p_299137_, MinMaxBounds.Ints p_286571_) {
         super(p_299225_);
         this.victims = p_299137_;
         this.uniqueEntityTypes = p_286571_;
      }

      public static Criterion<KilledByCrossbowTrigger.TriggerInstance> crossbowKilled(EntityPredicate.Builder... p_301077_) {
         return CriteriaTriggers.KILLED_BY_CROSSBOW
            .createCriterion(new KilledByCrossbowTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_301077_), MinMaxBounds.Ints.ANY));
      }

      public static Criterion<KilledByCrossbowTrigger.TriggerInstance> crossbowKilled(MinMaxBounds.Ints p_301148_) {
         return CriteriaTriggers.KILLED_BY_CROSSBOW.createCriterion(new KilledByCrossbowTrigger.TriggerInstance(Optional.empty(), List.of(), p_301148_));
      }

      public boolean matches(Collection<LootContext> p_46898_, int p_46899_) {
         if (!this.victims.isEmpty()) {
            List<LootContext> list = Lists.newArrayList(p_46898_);

            for(ContextAwarePredicate contextawarepredicate : this.victims) {
               boolean flag = false;
               Iterator<LootContext> iterator = list.iterator();

               while(iterator.hasNext()) {
                  LootContext lootcontext = iterator.next();
                  if (contextawarepredicate.matches(lootcontext)) {
                     iterator.remove();
                     flag = true;
                     break;
                  }
               }

               if (!flag) {
                  return false;
               }
            }
         }

         return this.uniqueEntityTypes.matches(p_46899_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.add("victims", ContextAwarePredicate.toJson(this.victims));
         jsonobject.add("unique_entity_types", this.uniqueEntityTypes.serializeToJson());
         return jsonobject;
      }
   }
}
