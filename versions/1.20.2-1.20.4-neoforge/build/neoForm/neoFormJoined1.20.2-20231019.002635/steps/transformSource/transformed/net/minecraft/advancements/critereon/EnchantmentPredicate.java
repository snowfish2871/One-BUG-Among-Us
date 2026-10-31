package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.enchantment.Enchantment;

public record EnchantmentPredicate(Optional<Holder<Enchantment>> enchantment, MinMaxBounds.Ints level) {
   public static final Codec<EnchantmentPredicate> CODEC = RecordCodecBuilder.create(
      p_297884_ -> p_297884_.group(
               ExtraCodecs.strictOptionalField(BuiltInRegistries.ENCHANTMENT.holderByNameCodec(), "enchantment").forGetter(EnchantmentPredicate::enchantment),
               ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "levels", MinMaxBounds.Ints.ANY).forGetter(EnchantmentPredicate::level)
            )
            .apply(p_297884_, EnchantmentPredicate::new)
   );

   public EnchantmentPredicate(Enchantment p_30471_, MinMaxBounds.Ints p_30472_) {
      this(Optional.of(p_30471_.builtInRegistryHolder()), p_30472_);
   }

   public boolean containedIn(Map<Enchantment, Integer> p_30477_) {
      if (this.enchantment.isPresent()) {
         Enchantment enchantment = this.enchantment.get().value();
         if (!p_30477_.containsKey(enchantment)) {
            return false;
         }

         int i = p_30477_.get(enchantment);
         if (this.level != MinMaxBounds.Ints.ANY && !this.level.matches(i)) {
            return false;
         }
      } else if (this.level != MinMaxBounds.Ints.ANY) {
         for(Integer integer : p_30477_.values()) {
            if (this.level.matches(integer)) {
               return true;
            }
         }

         return false;
      }

      return true;
   }
}
