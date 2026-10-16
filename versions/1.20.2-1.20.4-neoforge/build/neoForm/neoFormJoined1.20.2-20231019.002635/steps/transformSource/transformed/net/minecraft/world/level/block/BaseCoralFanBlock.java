package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BaseCoralFanBlock extends BaseCoralPlantTypeBlock {
   private static final VoxelShape AABB = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);

   public BaseCoralFanBlock(BlockBehaviour.Properties p_49106_) {
      super(p_49106_);
   }

   @Override
   public VoxelShape getShape(BlockState p_49108_, BlockGetter p_49109_, BlockPos p_49110_, CollisionContext p_49111_) {
      return AABB;
   }
}
