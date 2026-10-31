package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

public class NumberProviders {
   private static final Codec<NumberProvider> TYPED_CODEC = BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE
      .byNameCodec()
      .dispatch(NumberProvider::getType, LootNumberProviderType::codec);
   public static final Codec<NumberProvider> CODEC = ExtraCodecs.lazyInitializedCodec(
      () -> {
         Codec<NumberProvider> codec = ExtraCodecs.withAlternative(TYPED_CODEC, UniformGenerator.CODEC);
         return Codec.either(ConstantValue.INLINE_CODEC, codec)
            .xmap(
               p_298754_ -> p_298754_.map(Function.identity(), Function.identity()),
               p_299325_ -> p_299325_ instanceof ConstantValue constantvalue ? Either.left(constantvalue) : Either.right(p_299325_)
            );
      }
   );
   public static final LootNumberProviderType CONSTANT = register("constant", ConstantValue.CODEC);
   public static final LootNumberProviderType UNIFORM = register("uniform", UniformGenerator.CODEC);
   public static final LootNumberProviderType BINOMIAL = register("binomial", BinomialDistributionGenerator.CODEC);
   public static final LootNumberProviderType SCORE = register("score", ScoreboardValue.CODEC);

   private static LootNumberProviderType register(String p_165739_, Codec<? extends NumberProvider> p_298354_) {
      return Registry.register(BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE, new ResourceLocation(p_165739_), new LootNumberProviderType(p_298354_));
   }
}
