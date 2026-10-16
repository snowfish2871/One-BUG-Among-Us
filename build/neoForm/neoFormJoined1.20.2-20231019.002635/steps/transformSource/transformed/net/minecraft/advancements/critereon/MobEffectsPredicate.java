package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public record MobEffectsPredicate(Map<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> effectMap) {
   public static final Codec<MobEffectsPredicate> CODEC = Codec.unboundedMap(
         BuiltInRegistries.MOB_EFFECT.holderByNameCodec(), MobEffectsPredicate.MobEffectInstancePredicate.CODEC
      )
      .xmap(MobEffectsPredicate::new, MobEffectsPredicate::effectMap);

   public boolean matches(Entity p_56556_) {
      if (p_56556_ instanceof LivingEntity livingentity && this.matches(livingentity.getActiveEffectsMap())) {
         return true;
      }

      return false;
   }

   public boolean matches(LivingEntity p_56558_) {
      return this.matches(p_56558_.getActiveEffectsMap());
   }

   public boolean matches(Map<MobEffect, MobEffectInstance> p_56562_) {
      for(Entry<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> entry : this.effectMap.entrySet()) {
         MobEffectInstance mobeffectinstance = p_56562_.get(entry.getKey().value());
         if (!entry.getValue().matches(mobeffectinstance)) {
            return false;
         }
      }

      return true;
   }

   public static Optional<MobEffectsPredicate> fromJson(@Nullable JsonElement p_56560_) {
      return p_56560_ != null && !p_56560_.isJsonNull()
         ? Optional.of(Util.getOrThrow(CODEC.parse(JsonOps.INSTANCE, p_56560_), JsonParseException::new))
         : Optional.empty();
   }

   public JsonElement serializeToJson() {
      return Util.getOrThrow(CODEC.encodeStart(JsonOps.INSTANCE, this), IllegalStateException::new);
   }

   public static class Builder {
      private final ImmutableMap.Builder<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> effectMap = ImmutableMap.builder();

      public static MobEffectsPredicate.Builder effects() {
         return new MobEffectsPredicate.Builder();
      }

      public MobEffectsPredicate.Builder and(MobEffect p_298766_) {
         this.effectMap.put(p_298766_.builtInRegistryHolder(), new MobEffectsPredicate.MobEffectInstancePredicate());
         return this;
      }

      public MobEffectsPredicate.Builder and(MobEffect p_298825_, MobEffectsPredicate.MobEffectInstancePredicate p_298742_) {
         this.effectMap.put(p_298825_.builtInRegistryHolder(), p_298742_);
         return this;
      }

      public Optional<MobEffectsPredicate> build() {
         return Optional.of(new MobEffectsPredicate(this.effectMap.build()));
      }
   }

   public static record MobEffectInstancePredicate(
      MinMaxBounds.Ints amplifier, MinMaxBounds.Ints duration, Optional<Boolean> ambient, Optional<Boolean> visible
   ) {
      public static final Codec<MobEffectsPredicate.MobEffectInstancePredicate> CODEC = RecordCodecBuilder.create(
         p_298378_ -> p_298378_.group(
                  ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "amplifier", MinMaxBounds.Ints.ANY)
                     .forGetter(MobEffectsPredicate.MobEffectInstancePredicate::amplifier),
                  ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "duration", MinMaxBounds.Ints.ANY)
                     .forGetter(MobEffectsPredicate.MobEffectInstancePredicate::duration),
                  ExtraCodecs.strictOptionalField(Codec.BOOL, "ambient").forGetter(MobEffectsPredicate.MobEffectInstancePredicate::ambient),
                  ExtraCodecs.strictOptionalField(Codec.BOOL, "visible").forGetter(MobEffectsPredicate.MobEffectInstancePredicate::visible)
               )
               .apply(p_298378_, MobEffectsPredicate.MobEffectInstancePredicate::new)
      );

      public MobEffectInstancePredicate() {
         this(MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, Optional.empty(), Optional.empty());
      }

      public boolean matches(@Nullable MobEffectInstance p_56578_) {
         if (p_56578_ == null) {
            return false;
         } else if (!this.amplifier.matches(p_56578_.getAmplifier())) {
            return false;
         } else if (!this.duration.matches(p_56578_.getDuration())) {
            return false;
         } else if (this.ambient.isPresent() && this.ambient.get() != p_56578_.isAmbient()) {
            return false;
         } else {
            return !this.visible.isPresent() || this.visible.get() == p_56578_.isVisible();
         }
      }
   }
}
