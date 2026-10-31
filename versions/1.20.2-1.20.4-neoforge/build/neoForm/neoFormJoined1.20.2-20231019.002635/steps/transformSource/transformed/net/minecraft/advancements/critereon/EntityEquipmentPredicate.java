package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Items;

public record EntityEquipmentPredicate(
   Optional<ItemPredicate> head,
   Optional<ItemPredicate> chest,
   Optional<ItemPredicate> legs,
   Optional<ItemPredicate> feet,
   Optional<ItemPredicate> mainhand,
   Optional<ItemPredicate> offhand
) {
   public static final Codec<EntityEquipmentPredicate> CODEC = RecordCodecBuilder.create(
      p_298479_ -> p_298479_.group(
               ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "head").forGetter(EntityEquipmentPredicate::head),
               ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "chest").forGetter(EntityEquipmentPredicate::chest),
               ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "legs").forGetter(EntityEquipmentPredicate::legs),
               ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "feet").forGetter(EntityEquipmentPredicate::feet),
               ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "mainhand").forGetter(EntityEquipmentPredicate::mainhand),
               ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "offhand").forGetter(EntityEquipmentPredicate::offhand)
            )
            .apply(p_298479_, EntityEquipmentPredicate::new)
   );
   public static final EntityEquipmentPredicate CAPTAIN = EntityEquipmentPredicate.Builder.equipment()
      .head(ItemPredicate.Builder.item().of(Items.WHITE_BANNER).hasNbt(Raid.getLeaderBannerInstance().getTag()))
      .build();

   public boolean matches(@Nullable Entity p_32194_) {
      if (p_32194_ instanceof LivingEntity livingentity) {
         if (this.head.isPresent() && !this.head.get().matches(livingentity.getItemBySlot(EquipmentSlot.HEAD))) {
            return false;
         } else if (this.chest.isPresent() && !this.chest.get().matches(livingentity.getItemBySlot(EquipmentSlot.CHEST))) {
            return false;
         } else if (this.legs.isPresent() && !this.legs.get().matches(livingentity.getItemBySlot(EquipmentSlot.LEGS))) {
            return false;
         } else if (this.feet.isPresent() && !this.feet.get().matches(livingentity.getItemBySlot(EquipmentSlot.FEET))) {
            return false;
         } else if (this.mainhand.isPresent() && !this.mainhand.get().matches(livingentity.getItemBySlot(EquipmentSlot.MAINHAND))) {
            return false;
         } else {
            return !this.offhand.isPresent() || this.offhand.get().matches(livingentity.getItemBySlot(EquipmentSlot.OFFHAND));
         }
      } else {
         return false;
      }
   }

   public static class Builder {
      private Optional<ItemPredicate> head = Optional.empty();
      private Optional<ItemPredicate> chest = Optional.empty();
      private Optional<ItemPredicate> legs = Optional.empty();
      private Optional<ItemPredicate> feet = Optional.empty();
      private Optional<ItemPredicate> mainhand = Optional.empty();
      private Optional<ItemPredicate> offhand = Optional.empty();

      public static EntityEquipmentPredicate.Builder equipment() {
         return new EntityEquipmentPredicate.Builder();
      }

      public EntityEquipmentPredicate.Builder head(ItemPredicate.Builder p_298927_) {
         this.head = Optional.of(p_298927_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder chest(ItemPredicate.Builder p_298201_) {
         this.chest = Optional.of(p_298201_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder legs(ItemPredicate.Builder p_299199_) {
         this.legs = Optional.of(p_299199_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder feet(ItemPredicate.Builder p_299012_) {
         this.feet = Optional.of(p_299012_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder mainhand(ItemPredicate.Builder p_298502_) {
         this.mainhand = Optional.of(p_298502_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder offhand(ItemPredicate.Builder p_299073_) {
         this.offhand = Optional.of(p_299073_.build());
         return this;
      }

      public EntityEquipmentPredicate build() {
         return new EntityEquipmentPredicate(this.head, this.chest, this.legs, this.feet, this.mainhand, this.offhand);
      }
   }
}
