package net.minecraft.advancements.critereon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public record LightningBoltPredicate(MinMaxBounds.Ints blocksSetOnFire, Optional<EntityPredicate> entityStruck) implements EntitySubPredicate {
   public static final MapCodec<LightningBoltPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_298977_ -> p_298977_.group(
               ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "blocks_set_on_fire", MinMaxBounds.Ints.ANY)
                  .forGetter(LightningBoltPredicate::blocksSetOnFire),
               ExtraCodecs.strictOptionalField(EntityPredicate.CODEC, "entity_struck").forGetter(LightningBoltPredicate::entityStruck)
            )
            .apply(p_298977_, LightningBoltPredicate::new)
   );

   public static LightningBoltPredicate blockSetOnFire(MinMaxBounds.Ints p_299013_) {
      return new LightningBoltPredicate(p_299013_, Optional.empty());
   }

   @Override
   public EntitySubPredicate.Type type() {
      return EntitySubPredicate.Types.LIGHTNING;
   }

   @Override
   public boolean matches(Entity p_299034_, ServerLevel p_299101_, @Nullable Vec3 p_298929_) {
      if (!(p_299034_ instanceof LightningBolt)) {
         return false;
      } else {
         LightningBolt lightningbolt = (LightningBolt)p_299034_;
         return this.blocksSetOnFire.matches(lightningbolt.getBlocksSetOnFire())
            && (
               this.entityStruck.isEmpty()
                  || lightningbolt.getHitEntities().anyMatch(p_298360_ -> this.entityStruck.get().matches(p_299101_, p_298929_, p_298360_))
            );
      }
   }
}
