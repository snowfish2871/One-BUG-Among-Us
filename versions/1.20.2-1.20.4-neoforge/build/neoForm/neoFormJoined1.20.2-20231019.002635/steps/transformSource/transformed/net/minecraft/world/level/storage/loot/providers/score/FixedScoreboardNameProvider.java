package net.minecraft.world.level.storage.loot.providers.score;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public record FixedScoreboardNameProvider(String name) implements ScoreboardNameProvider {
   public static final Codec<FixedScoreboardNameProvider> CODEC = RecordCodecBuilder.create(
      p_298540_ -> p_298540_.group(Codec.STRING.fieldOf("name").forGetter(FixedScoreboardNameProvider::name))
            .apply(p_298540_, FixedScoreboardNameProvider::new)
   );

   public static ScoreboardNameProvider forName(String p_165847_) {
      return new FixedScoreboardNameProvider(p_165847_);
   }

   @Override
   public LootScoreProviderType getType() {
      return ScoreboardNameProviders.FIXED;
   }

   @Nullable
   @Override
   public String getScoreboardName(LootContext p_165845_) {
      return this.name;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of();
   }
}
