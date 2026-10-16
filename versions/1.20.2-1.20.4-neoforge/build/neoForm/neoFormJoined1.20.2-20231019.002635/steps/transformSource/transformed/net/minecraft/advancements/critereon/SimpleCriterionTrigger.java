package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;

public abstract class SimpleCriterionTrigger<T extends SimpleCriterionTrigger.SimpleInstance> implements CriterionTrigger<T> {
   private final Map<PlayerAdvancements, Set<CriterionTrigger.Listener<T>>> players = Maps.newIdentityHashMap();

   @Override
   public final void addPlayerListener(PlayerAdvancements p_66243_, CriterionTrigger.Listener<T> p_66244_) {
      this.players.computeIfAbsent(p_66243_, p_66252_ -> Sets.newHashSet()).add(p_66244_);
   }

   @Override
   public final void removePlayerListener(PlayerAdvancements p_66254_, CriterionTrigger.Listener<T> p_66255_) {
      Set<CriterionTrigger.Listener<T>> set = this.players.get(p_66254_);
      if (set != null) {
         set.remove(p_66255_);
         if (set.isEmpty()) {
            this.players.remove(p_66254_);
         }
      }
   }

   @Override
   public final void removePlayerListeners(PlayerAdvancements p_66241_) {
      this.players.remove(p_66241_);
   }

   protected abstract T createInstance(JsonObject p_66248_, Optional<ContextAwarePredicate> p_298709_, DeserializationContext p_66250_);

   public final T createInstance(JsonObject p_66246_, DeserializationContext p_66247_) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(p_66246_, "player", p_66247_);
      return this.createInstance(p_66246_, optional, p_66247_);
   }

   protected void trigger(ServerPlayer p_66235_, Predicate<T> p_66236_) {
      PlayerAdvancements playeradvancements = p_66235_.getAdvancements();
      Set<CriterionTrigger.Listener<T>> set = this.players.get(playeradvancements);
      if (set != null && !set.isEmpty()) {
         LootContext lootcontext = EntityPredicate.createContext(p_66235_, p_66235_);
         List<CriterionTrigger.Listener<T>> list = null;

         for(CriterionTrigger.Listener<T> listener : set) {
            T t = listener.trigger();
            if (p_66236_.test(t)) {
               Optional<ContextAwarePredicate> optional = t.playerPredicate();
               if (optional.isEmpty() || optional.get().matches(lootcontext)) {
                  if (list == null) {
                     list = Lists.newArrayList();
                  }

                  list.add(listener);
               }
            }
         }

         if (list != null) {
            for(CriterionTrigger.Listener<T> listener1 : list) {
               listener1.run(playeradvancements);
            }
         }
      }
   }

   public interface SimpleInstance extends CriterionTriggerInstance {
      Optional<ContextAwarePredicate> playerPredicate();
   }
}
