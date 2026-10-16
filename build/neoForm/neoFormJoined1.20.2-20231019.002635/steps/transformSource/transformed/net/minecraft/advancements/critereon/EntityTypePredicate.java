package net.minecraft.advancements.critereon;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public record EntityTypePredicate(HolderSet<EntityType<?>> types) {
   public static final Codec<EntityTypePredicate> CODEC = Codec.either(
         TagKey.hashedCodec(Registries.ENTITY_TYPE), BuiltInRegistries.ENTITY_TYPE.holderByNameCodec()
      )
      .flatComapMap(
         p_297894_ -> p_297894_.map(
               p_297895_ -> new EntityTypePredicate(BuiltInRegistries.ENTITY_TYPE.getOrCreateTag(p_297895_)),
               p_297896_ -> new EntityTypePredicate(HolderSet.direct(p_297896_))
            ),
         p_297898_ -> {
            HolderSet<EntityType<?>> holderset = p_297898_.types();
            Optional<TagKey<EntityType<?>>> optional = holderset.unwrapKey();
            if (optional.isPresent()) {
               return DataResult.success(Either.left(optional.get()));
            } else {
               return holderset.size() == 1
                  ? DataResult.success(Either.right(holderset.get(0)))
                  : DataResult.error(() -> "Entity type set must have a single element, but got " + holderset.size());
            }
         }
      );

   public static EntityTypePredicate of(EntityType<?> p_37648_) {
      return new EntityTypePredicate(HolderSet.direct(p_37648_.builtInRegistryHolder()));
   }

   public static EntityTypePredicate of(TagKey<EntityType<?>> p_204082_) {
      return new EntityTypePredicate(BuiltInRegistries.ENTITY_TYPE.getOrCreateTag(p_204082_));
   }

   public boolean matches(EntityType<?> p_37642_) {
      return p_37642_.is(this.types);
   }
}
