package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public class DispenserBlock extends BaseEntityBlock {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final DirectionProperty FACING = DirectionalBlock.FACING;
   public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
   private static final Map<Item, DispenseItemBehavior> DISPENSER_REGISTRY = Util.make(
      new Object2ObjectOpenHashMap<>(), p_52723_ -> p_52723_.defaultReturnValue(new DefaultDispenseItemBehavior())
   );
   private static final int TRIGGER_DURATION = 4;

   public static void registerBehavior(ItemLike p_52673_, DispenseItemBehavior p_52674_) {
      DISPENSER_REGISTRY.put(p_52673_.asItem(), p_52674_);
   }

   public DispenserBlock(BlockBehaviour.Properties p_52664_) {
      super(p_52664_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, Boolean.valueOf(false)));
   }

   @Override
   public InteractionResult use(BlockState p_52693_, Level p_52694_, BlockPos p_52695_, Player p_52696_, InteractionHand p_52697_, BlockHitResult p_52698_) {
      if (p_52694_.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockEntity blockentity = p_52694_.getBlockEntity(p_52695_);
         if (blockentity instanceof DispenserBlockEntity) {
            p_52696_.openMenu((DispenserBlockEntity)blockentity);
            if (blockentity instanceof DropperBlockEntity) {
               p_52696_.awardStat(Stats.INSPECT_DROPPER);
            } else {
               p_52696_.awardStat(Stats.INSPECT_DISPENSER);
            }
         }

         return InteractionResult.CONSUME;
      }
   }

   protected void dispenseFrom(ServerLevel p_52665_, BlockState p_302451_, BlockPos p_52666_) {
      DispenserBlockEntity dispenserblockentity = p_52665_.getBlockEntity(p_52666_, BlockEntityType.DISPENSER).orElse(null);
      if (dispenserblockentity == null) {
         LOGGER.warn("Ignoring dispensing attempt for Dispenser without matching block entity at {}", p_52666_);
      } else {
         BlockSource blocksource = new BlockSource(p_52665_, p_52666_, p_302451_, dispenserblockentity);
         int i = dispenserblockentity.getRandomSlot(p_52665_.random);
         if (i < 0) {
            p_52665_.levelEvent(1001, p_52666_, 0);
            p_52665_.gameEvent(GameEvent.BLOCK_ACTIVATE, p_52666_, GameEvent.Context.of(dispenserblockentity.getBlockState()));
         } else {
            ItemStack itemstack = dispenserblockentity.getItem(i);
            DispenseItemBehavior dispenseitembehavior = this.getDispenseMethod(itemstack);
            if (dispenseitembehavior != DispenseItemBehavior.NOOP) {
               dispenserblockentity.setItem(i, dispenseitembehavior.dispense(blocksource, itemstack));
            }
         }
      }
   }

   protected DispenseItemBehavior getDispenseMethod(ItemStack p_52667_) {
      return DISPENSER_REGISTRY.get(p_52667_.getItem());
   }

   @Override
   public void neighborChanged(BlockState p_52700_, Level p_52701_, BlockPos p_52702_, Block p_52703_, BlockPos p_52704_, boolean p_52705_) {
      boolean flag = p_52701_.hasNeighborSignal(p_52702_) || p_52701_.hasNeighborSignal(p_52702_.above());
      boolean flag1 = p_52700_.getValue(TRIGGERED);
      if (flag && !flag1) {
         p_52701_.scheduleTick(p_52702_, this, 4);
         p_52701_.setBlock(p_52702_, p_52700_.setValue(TRIGGERED, Boolean.valueOf(true)), 2);
      } else if (!flag && flag1) {
         p_52701_.setBlock(p_52702_, p_52700_.setValue(TRIGGERED, Boolean.valueOf(false)), 2);
      }
   }

   @Override
   public void tick(BlockState p_221075_, ServerLevel p_221076_, BlockPos p_221077_, RandomSource p_221078_) {
      this.dispenseFrom(p_221076_, p_221075_, p_221077_);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153162_, BlockState p_153163_) {
      return new DispenserBlockEntity(p_153162_, p_153163_);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_52669_) {
      return this.defaultBlockState().setValue(FACING, p_52669_.getNearestLookingDirection().getOpposite());
   }

   @Override
   public void setPlacedBy(Level p_52676_, BlockPos p_52677_, BlockState p_52678_, LivingEntity p_52679_, ItemStack p_52680_) {
      if (p_52680_.hasCustomHoverName()) {
         BlockEntity blockentity = p_52676_.getBlockEntity(p_52677_);
         if (blockentity instanceof DispenserBlockEntity) {
            ((DispenserBlockEntity)blockentity).setCustomName(p_52680_.getHoverName());
         }
      }
   }

   @Override
   public void onRemove(BlockState p_52707_, Level p_52708_, BlockPos p_52709_, BlockState p_52710_, boolean p_52711_) {
      if (!p_52707_.is(p_52710_.getBlock())) {
         BlockEntity blockentity = p_52708_.getBlockEntity(p_52709_);
         if (blockentity instanceof DispenserBlockEntity) {
            Containers.dropContents(p_52708_, p_52709_, (DispenserBlockEntity)blockentity);
            p_52708_.updateNeighbourForOutputSignal(p_52709_, this);
         }

         super.onRemove(p_52707_, p_52708_, p_52709_, p_52710_, p_52711_);
      }
   }

   public static Position getDispensePosition(BlockSource p_302440_) {
      Direction direction = p_302440_.state().getValue(FACING);
      return p_302440_.center().add(0.7 * (double)direction.getStepX(), 0.7 * (double)direction.getStepY(), 0.7 * (double)direction.getStepZ());
   }

   @Override
   public boolean hasAnalogOutputSignal(BlockState p_52682_) {
      return true;
   }

   @Override
   public int getAnalogOutputSignal(BlockState p_52689_, Level p_52690_, BlockPos p_52691_) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(p_52690_.getBlockEntity(p_52691_));
   }

   @Override
   public RenderShape getRenderShape(BlockState p_52725_) {
      return RenderShape.MODEL;
   }

   @Override
   public BlockState rotate(BlockState p_52716_, Rotation p_52717_) {
      return p_52716_.setValue(FACING, p_52717_.rotate(p_52716_.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState p_52713_, Mirror p_52714_) {
      return p_52713_.rotate(p_52714_.getRotation(p_52713_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_52719_) {
      p_52719_.add(FACING, TRIGGERED);
   }
}
