package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;

public record DamagePredicate(
   MinMaxBounds.Doubles dealtDamage,
   MinMaxBounds.Doubles takenDamage,
   Optional<EntityPredicate> sourceEntity,
   Optional<Boolean> blocked,
   Optional<DamageSourcePredicate> type
) {
   public boolean matches(ServerPlayer p_24918_, DamageSource p_24919_, float p_24920_, float p_24921_, boolean p_24922_) {
      if (!this.dealtDamage.matches((double)p_24920_)) {
         return false;
      } else if (!this.takenDamage.matches((double)p_24921_)) {
         return false;
      } else if (this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(p_24918_, p_24919_.getEntity())) {
         return false;
      } else if (this.blocked.isPresent() && this.blocked.get() != p_24922_) {
         return false;
      } else {
         return !this.type.isPresent() || this.type.get().matches(p_24918_, p_24919_);
      }
   }

   public static Optional<DamagePredicate> fromJson(@Nullable JsonElement p_24924_) {
      if (p_24924_ != null && !p_24924_.isJsonNull()) {
         JsonObject jsonobject = GsonHelper.convertToJsonObject(p_24924_, "damage");
         MinMaxBounds.Doubles minmaxbounds$doubles = MinMaxBounds.Doubles.fromJson(jsonobject.get("dealt"));
         MinMaxBounds.Doubles minmaxbounds$doubles1 = MinMaxBounds.Doubles.fromJson(jsonobject.get("taken"));
         Optional<Boolean> optional = jsonobject.has("blocked") ? Optional.of(GsonHelper.getAsBoolean(jsonobject, "blocked")) : Optional.empty();
         Optional<EntityPredicate> optional1 = EntityPredicate.fromJson(jsonobject.get("source_entity"));
         Optional<DamageSourcePredicate> optional2 = DamageSourcePredicate.fromJson(jsonobject.get("type"));
         return minmaxbounds$doubles.isAny() && minmaxbounds$doubles1.isAny() && optional1.isEmpty() && optional.isEmpty() && optional2.isEmpty()
            ? Optional.empty()
            : Optional.of(new DamagePredicate(minmaxbounds$doubles, minmaxbounds$doubles1, optional1, optional, optional2));
      } else {
         return Optional.empty();
      }
   }

   public JsonElement serializeToJson() {
      JsonObject jsonobject = new JsonObject();
      jsonobject.add("dealt", this.dealtDamage.serializeToJson());
      jsonobject.add("taken", this.takenDamage.serializeToJson());
      this.sourceEntity.ifPresent(p_299181_ -> jsonobject.add("source_entity", p_299181_.serializeToJson()));
      this.type.ifPresent(p_298240_ -> jsonobject.add("type", p_298240_.serializeToJson()));
      this.blocked.ifPresent(p_298848_ -> jsonobject.addProperty("blocked", p_298848_));
      return jsonobject;
   }

   public static class Builder {
      private MinMaxBounds.Doubles dealtDamage = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles takenDamage = MinMaxBounds.Doubles.ANY;
      private Optional<EntityPredicate> sourceEntity = Optional.empty();
      private Optional<Boolean> blocked = Optional.empty();
      private Optional<DamageSourcePredicate> type = Optional.empty();

      public static DamagePredicate.Builder damageInstance() {
         return new DamagePredicate.Builder();
      }

      public DamagePredicate.Builder dealtDamage(MinMaxBounds.Doubles p_148146_) {
         this.dealtDamage = p_148146_;
         return this;
      }

      public DamagePredicate.Builder takenDamage(MinMaxBounds.Doubles p_148148_) {
         this.takenDamage = p_148148_;
         return this;
      }

      public DamagePredicate.Builder sourceEntity(EntityPredicate p_148144_) {
         this.sourceEntity = Optional.of(p_148144_);
         return this;
      }

      public DamagePredicate.Builder blocked(Boolean p_24935_) {
         this.blocked = Optional.of(p_24935_);
         return this;
      }

      public DamagePredicate.Builder type(DamageSourcePredicate p_148142_) {
         this.type = Optional.of(p_148142_);
         return this;
      }

      public DamagePredicate.Builder type(DamageSourcePredicate.Builder p_24933_) {
         this.type = Optional.of(p_24933_.build());
         return this;
      }

      public DamagePredicate build() {
         return new DamagePredicate(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
      }
   }
}
