package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AirBlock extends Block {
   public AirBlock(BlockBehaviour.Properties p_48756_) {
      super(p_48756_);
   }

   @Override
   public RenderShape getRenderShape(BlockState p_48758_) {
      return RenderShape.INVISIBLE;
   }

   @Override
   public VoxelShape getShape(BlockState p_48760_, BlockGetter p_48761_, BlockPos p_48762_, CollisionContext p_48763_) {
      return Shapes.empty();
   }
}
