package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public interface SuspiciousEffectHolder {
   List<SuspiciousEffectHolder.EffectEntry> getSuspiciousEffects();

   static List<SuspiciousEffectHolder> getAllEffectHolders() {
      return BuiltInRegistries.ITEM.stream().map(SuspiciousEffectHolder::tryGet).filter(Objects::nonNull).collect(Collectors.toList());
   }

   @Nullable
   static SuspiciousEffectHolder tryGet(ItemLike p_259322_) {
      Item item1 = p_259322_.asItem();
      if (item1 instanceof BlockItem blockitem) {
         Block block = blockitem.getBlock();
         if (block instanceof SuspiciousEffectHolder suspiciouseffectholder1) {
            return suspiciouseffectholder1;
         }
      }

      Item $$2 = p_259322_.asItem();
      return $$2 instanceof SuspiciousEffectHolder suspiciouseffectholder ? suspiciouseffectholder : null;
   }

   public static record EffectEntry(MobEffect effect, int duration) {
      public static final Codec<SuspiciousEffectHolder.EffectEntry> CODEC = RecordCodecBuilder.create(
         p_298696_ -> p_298696_.group(
                  BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("id").forGetter(SuspiciousEffectHolder.EffectEntry::effect),
                  Codec.INT.optionalFieldOf("duration", Integer.valueOf(160)).forGetter(SuspiciousEffectHolder.EffectEntry::duration)
               )
               .apply(p_298696_, SuspiciousEffectHolder.EffectEntry::new)
      );
      public static final Codec<List<SuspiciousEffectHolder.EffectEntry>> LIST_CODEC = CODEC.listOf();

      public MobEffectInstance createEffectInstance() {
         return new MobEffectInstance(this.effect, this.duration);
      }
   }
}
