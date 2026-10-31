package net.minecraft.world.level.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlowerBlock extends BushBlock implements SuspiciousEffectHolder {
   protected static final float AABB_OFFSET = 3.0F;
   protected static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 10.0, 11.0);
   private final java.util.function.Supplier<java.util.List<net.minecraft.world.level.block.SuspiciousEffectHolder.EffectEntry>> suspiciousStewEffectSupplier;

   public FlowerBlock(java.util.function.Supplier<net.minecraft.world.effect.MobEffect> effectSupplier, int p_53513_, BlockBehaviour.Properties p_53514_) {
      super(p_53514_);
      
      this.suspiciousStewEffectSupplier = () -> {
         final MobEffect p_53512_ = effectSupplier.get();
         int i;
         if (p_53512_.isInstantenous()) {
            i = p_53513_;
         } else {
            i = p_53513_ * 20;
         }
         
         return List.of(new SuspiciousEffectHolder.EffectEntry(p_53512_, i));
      };
   }

   /** @deprecated FORGE: Use supplier version instead */
   @Deprecated
   public FlowerBlock(MobEffect p_53512_, int p_53513_, BlockBehaviour.Properties p_53514_) {
      super(p_53514_);
      int i;
      if (p_53512_.isInstantenous()) {
         i = p_53513_;
      } else {
         i = p_53513_ * 20;
      }

      this.suspiciousStewEffectSupplier = () -> List.of(new SuspiciousEffectHolder.EffectEntry(p_53512_, i));
   }

   public VoxelShape getShape(BlockState p_53517_, BlockGetter p_53518_, BlockPos p_53519_, CollisionContext p_53520_) {
      Vec3 vec3 = p_53517_.getOffset(p_53518_, p_53519_);
      return SHAPE.move(vec3.x, vec3.y, vec3.z);
   }

   @Override
   public List<SuspiciousEffectHolder.EffectEntry> getSuspiciousEffects() {
      return this.suspiciousStewEffectSupplier.get();
   }
}
