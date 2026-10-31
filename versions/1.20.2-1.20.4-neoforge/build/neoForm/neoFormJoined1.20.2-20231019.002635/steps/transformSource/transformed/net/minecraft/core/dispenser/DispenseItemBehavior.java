package net.minecraft.core.dispenser;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public interface DispenseItemBehavior {
   Logger LOGGER = LogUtils.getLogger();
   DispenseItemBehavior NOOP = (p_302424_, p_123401_) -> p_123401_;

   ItemStack dispense(BlockSource p_302445_, ItemStack p_123404_);

   static void bootStrap() {
      DispenserBlock.registerBehavior(Items.ARROW, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level p_123407_, Position p_123408_, ItemStack p_123409_) {
            Arrow arrow = new Arrow(p_123407_, p_123408_.x(), p_123408_.y(), p_123408_.z());
            arrow.pickup = AbstractArrow.Pickup.ALLOWED;
            return arrow;
         }
      });
      DispenserBlock.registerBehavior(Items.TIPPED_ARROW, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level p_123420_, Position p_123421_, ItemStack p_123422_) {
            Arrow arrow = new Arrow(p_123420_, p_123421_.x(), p_123421_.y(), p_123421_.z());
            arrow.setEffectsFromItem(p_123422_);
            arrow.pickup = AbstractArrow.Pickup.ALLOWED;
            return arrow;
         }
      });
      DispenserBlock.registerBehavior(Items.SPECTRAL_ARROW, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level p_123456_, Position p_123457_, ItemStack p_123458_) {
            AbstractArrow abstractarrow = new SpectralArrow(p_123456_, p_123457_.x(), p_123457_.y(), p_123457_.z());
            abstractarrow.pickup = AbstractArrow.Pickup.ALLOWED;
            return abstractarrow;
         }
      });
      DispenserBlock.registerBehavior(Items.EGG, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level p_123468_, Position p_123469_, ItemStack p_123470_) {
            return Util.make(new ThrownEgg(p_123468_, p_123469_.x(), p_123469_.y(), p_123469_.z()), p_123466_ -> p_123466_.setItem(p_123470_));
         }
      });
      DispenserBlock.registerBehavior(Items.SNOWBALL, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level p_123476_, Position p_123477_, ItemStack p_123478_) {
            return Util.make(new Snowball(p_123476_, p_123477_.x(), p_123477_.y(), p_123477_.z()), p_123474_ -> p_123474_.setItem(p_123478_));
         }
      });
      DispenserBlock.registerBehavior(Items.EXPERIENCE_BOTTLE, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level p_123485_, Position p_123486_, ItemStack p_123487_) {
            return Util.make(new ThrownExperienceBottle(p_123485_, p_123486_.x(), p_123486_.y(), p_123486_.z()), p_123483_ -> p_123483_.setItem(p_123487_));
         }

         @Override
         protected float getUncertainty() {
            return super.getUncertainty() * 0.5F;
         }

         @Override
         protected float getPower() {
            return super.getPower() * 1.25F;
         }
      });
      DispenserBlock.registerBehavior(Items.SPLASH_POTION, new DispenseItemBehavior() {
         @Override
         public ItemStack dispense(BlockSource p_302458_, ItemStack p_123492_) {
            return (new AbstractProjectileDispenseBehavior() {
               @Override
               protected Projectile getProjectile(Level p_123501_, Position p_123502_, ItemStack p_123503_) {
                  return Util.make(new ThrownPotion(p_123501_, p_123502_.x(), p_123502_.y(), p_123502_.z()), p_123499_ -> p_123499_.setItem(p_123503_));
               }

               @Override
               protected float getUncertainty() {
                  return super.getUncertainty() * 0.5F;
               }

               @Override
               protected float getPower() {
                  return super.getPower() * 1.25F;
               }
            }).dispense(p_302458_, p_123492_);
         }
      });
      DispenserBlock.registerBehavior(Items.LINGERING_POTION, new DispenseItemBehavior() {
         @Override
         public ItemStack dispense(BlockSource p_302461_, ItemStack p_123508_) {
            return (new AbstractProjectileDispenseBehavior() {
               @Override
               protected Projectile getProjectile(Level p_123517_, Position p_123518_, ItemStack p_123519_) {
                  return Util.make(new ThrownPotion(p_123517_, p_123518_.x(), p_123518_.y(), p_123518_.z()), p_123515_ -> p_123515_.setItem(p_123519_));
               }

               @Override
               protected float getUncertainty() {
                  return super.getUncertainty() * 0.5F;
               }

               @Override
               protected float getPower() {
                  return super.getPower() * 1.25F;
               }
            }).dispense(p_302461_, p_123508_);
         }
      });
      DefaultDispenseItemBehavior defaultdispenseitembehavior = new DefaultDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource p_302450_, ItemStack p_123524_) {
            Direction direction = p_302450_.state().getValue(DispenserBlock.FACING);
            EntityType<?> entitytype = ((SpawnEggItem)p_123524_.getItem()).getType(p_123524_.getTag());

            try {
               entitytype.spawn(
                  p_302450_.level(), p_123524_, null, p_302450_.pos().relative(direction), MobSpawnType.DISPENSER, direction != Direction.UP, false
               );
            } catch (Exception exception) {
               LOGGER.error("Error while dispensing spawn egg from dispenser at {}", p_302450_.pos(), exception);
               return ItemStack.EMPTY;
            }

            p_123524_.shrink(1);
            p_302450_.level().gameEvent(null, GameEvent.ENTITY_PLACE, p_302450_.pos());
            return p_123524_;
         }
      };

      for(SpawnEggItem spawneggitem : SpawnEggItem.eggs()) {
         DispenserBlock.registerBehavior(spawneggitem, defaultdispenseitembehavior);
      }

      DispenserBlock.registerBehavior(
         Items.ARMOR_STAND,
         new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource p_302430_, ItemStack p_123462_) {
               Direction direction = p_302430_.state().getValue(DispenserBlock.FACING);
               BlockPos blockpos = p_302430_.pos().relative(direction);
               ServerLevel serverlevel = p_302430_.level();
               Consumer<ArmorStand> consumer = EntityType.appendDefaultStackConfig(
                  p_293695_ -> p_293695_.setYRot(direction.toYRot()), serverlevel, p_123462_, null
               );
               ArmorStand armorstand = EntityType.ARMOR_STAND.spawn(serverlevel, p_123462_.getTag(), consumer, blockpos, MobSpawnType.DISPENSER, false, false);
               if (armorstand != null) {
                  p_123462_.shrink(1);
               }
   
               return p_123462_;
            }
         }
      );
      DispenserBlock.registerBehavior(Items.SADDLE, new OptionalDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource p_302463_, ItemStack p_123530_) {
            BlockPos blockpos = p_302463_.pos().relative(p_302463_.state().getValue(DispenserBlock.FACING));
            List<LivingEntity> list = p_302463_.level().getEntitiesOfClass(LivingEntity.class, new AABB(blockpos), p_123527_ -> {
               if (!(p_123527_ instanceof Saddleable)) {
                  return false;
               } else {
                  Saddleable saddleable = (Saddleable)p_123527_;
                  return !saddleable.isSaddled() && saddleable.isSaddleable();
               }
            });
            if (!list.isEmpty()) {
               ((Saddleable)list.get(0)).equipSaddle(SoundSource.BLOCKS);
               p_123530_.shrink(1);
               this.setSuccess(true);
               return p_123530_;
            } else {
               return super.execute(p_302463_, p_123530_);
            }
         }
      });
      DefaultDispenseItemBehavior defaultdispenseitembehavior1 = new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_302425_, ItemStack p_123536_) {
            BlockPos blockpos = p_302425_.pos().relative(p_302425_.state().getValue(DispenserBlock.FACING));

            for(AbstractHorse abstracthorse : p_302425_.level()
               .getEntitiesOfClass(AbstractHorse.class, new AABB(blockpos), p_302152_ -> p_302152_.isAlive() && p_302152_.canWearArmor())) {
               if (abstracthorse.isArmor(p_123536_) && !abstracthorse.isWearingArmor() && abstracthorse.isTamed()) {
                  abstracthorse.getSlot(401).set(p_123536_.split(1));
                  this.setSuccess(true);
                  return p_123536_;
               }
            }

            return super.execute(p_302425_, p_123536_);
         }
      };
      DispenserBlock.registerBehavior(Items.LEATHER_HORSE_ARMOR, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.IRON_HORSE_ARMOR, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.GOLDEN_HORSE_ARMOR, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.DIAMOND_HORSE_ARMOR, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.WHITE_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.ORANGE_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.CYAN_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.BLUE_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.BROWN_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.BLACK_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.GRAY_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.GREEN_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.LIGHT_BLUE_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.LIGHT_GRAY_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.LIME_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.MAGENTA_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.PINK_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.PURPLE_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.RED_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.YELLOW_CARPET, defaultdispenseitembehavior1);
      DispenserBlock.registerBehavior(
         Items.CHEST,
         new OptionalDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource p_302452_, ItemStack p_123542_) {
               BlockPos blockpos = p_302452_.pos().relative(p_302452_.state().getValue(DispenserBlock.FACING));
   
               for(AbstractChestedHorse abstractchestedhorse : p_302452_.level()
                  .getEntitiesOfClass(AbstractChestedHorse.class, new AABB(blockpos), p_302153_ -> p_302153_.isAlive() && !p_302153_.hasChest())) {
                  if (abstractchestedhorse.isTamed() && abstractchestedhorse.getSlot(499).set(p_123542_)) {
                     p_123542_.shrink(1);
                     this.setSuccess(true);
                     return p_123542_;
                  }
               }
   
               return super.execute(p_302452_, p_123542_);
            }
         }
      );
      DispenserBlock.registerBehavior(Items.FIREWORK_ROCKET, new DefaultDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource p_302433_, ItemStack p_123548_) {
            Direction direction = p_302433_.state().getValue(DispenserBlock.FACING);
            Vec3 vec3 = DispenseItemBehavior.getEntityPokingOutOfBlockPos(p_302433_, EntityType.FIREWORK_ROCKET, direction);
            FireworkRocketEntity fireworkrocketentity = new FireworkRocketEntity(p_302433_.level(), p_123548_, vec3.x(), vec3.y(), vec3.z(), true);
            fireworkrocketentity.shoot((double)direction.getStepX(), (double)direction.getStepY(), (double)direction.getStepZ(), 0.5F, 1.0F);
            p_302433_.level().addFreshEntity(fireworkrocketentity);
            p_123548_.shrink(1);
            return p_123548_;
         }

         @Override
         protected void playSound(BlockSource p_302457_) {
            p_302457_.level().levelEvent(1004, p_302457_.pos(), 0);
         }
      });
      DispenserBlock.registerBehavior(Items.FIRE_CHARGE, new DefaultDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource p_302423_, ItemStack p_123557_) {
            Direction direction = p_302423_.state().getValue(DispenserBlock.FACING);
            Position position = DispenserBlock.getDispensePosition(p_302423_);
            double d0 = position.x() + (double)((float)direction.getStepX() * 0.3F);
            double d1 = position.y() + (double)((float)direction.getStepY() * 0.3F);
            double d2 = position.z() + (double)((float)direction.getStepZ() * 0.3F);
            Level level = p_302423_.level();
            RandomSource randomsource = level.random;
            double d3 = randomsource.triangle((double)direction.getStepX(), 0.11485000000000001);
            double d4 = randomsource.triangle((double)direction.getStepY(), 0.11485000000000001);
            double d5 = randomsource.triangle((double)direction.getStepZ(), 0.11485000000000001);
            SmallFireball smallfireball = new SmallFireball(level, d0, d1, d2, d3, d4, d5);
            level.addFreshEntity(Util.make(smallfireball, p_123552_ -> p_123552_.setItem(p_123557_)));
            p_123557_.shrink(1);
            return p_123557_;
         }

         @Override
         protected void playSound(BlockSource p_302436_) {
            p_302436_.level().levelEvent(1018, p_302436_.pos(), 0);
         }
      });
      DispenserBlock.registerBehavior(Items.OAK_BOAT, new BoatDispenseItemBehavior(Boat.Type.OAK));
      DispenserBlock.registerBehavior(Items.SPRUCE_BOAT, new BoatDispenseItemBehavior(Boat.Type.SPRUCE));
      DispenserBlock.registerBehavior(Items.BIRCH_BOAT, new BoatDispenseItemBehavior(Boat.Type.BIRCH));
      DispenserBlock.registerBehavior(Items.JUNGLE_BOAT, new BoatDispenseItemBehavior(Boat.Type.JUNGLE));
      DispenserBlock.registerBehavior(Items.DARK_OAK_BOAT, new BoatDispenseItemBehavior(Boat.Type.DARK_OAK));
      DispenserBlock.registerBehavior(Items.ACACIA_BOAT, new BoatDispenseItemBehavior(Boat.Type.ACACIA));
      DispenserBlock.registerBehavior(Items.CHERRY_BOAT, new BoatDispenseItemBehavior(Boat.Type.CHERRY));
      DispenserBlock.registerBehavior(Items.MANGROVE_BOAT, new BoatDispenseItemBehavior(Boat.Type.MANGROVE));
      DispenserBlock.registerBehavior(Items.BAMBOO_RAFT, new BoatDispenseItemBehavior(Boat.Type.BAMBOO));
      DispenserBlock.registerBehavior(Items.OAK_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.OAK, true));
      DispenserBlock.registerBehavior(Items.SPRUCE_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.SPRUCE, true));
      DispenserBlock.registerBehavior(Items.BIRCH_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.BIRCH, true));
      DispenserBlock.registerBehavior(Items.JUNGLE_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.JUNGLE, true));
      DispenserBlock.registerBehavior(Items.DARK_OAK_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.DARK_OAK, true));
      DispenserBlock.registerBehavior(Items.ACACIA_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.ACACIA, true));
      DispenserBlock.registerBehavior(Items.CHERRY_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.CHERRY, true));
      DispenserBlock.registerBehavior(Items.MANGROVE_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.MANGROVE, true));
      DispenserBlock.registerBehavior(Items.BAMBOO_CHEST_RAFT, new BoatDispenseItemBehavior(Boat.Type.BAMBOO, true));
      DispenseItemBehavior dispenseitembehavior1 = new DefaultDispenseItemBehavior() {
         private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

         @Override
         public ItemStack execute(BlockSource p_302435_, ItemStack p_123562_) {
            DispensibleContainerItem dispensiblecontaineritem = (DispensibleContainerItem)p_123562_.getItem();
            BlockPos blockpos = p_302435_.pos().relative(p_302435_.state().getValue(DispenserBlock.FACING));
            Level level = p_302435_.level();
            if (dispensiblecontaineritem.emptyContents(null, level, blockpos, null, p_123562_)) {
               dispensiblecontaineritem.checkExtraContent(null, level, p_123562_, blockpos);
               return new ItemStack(Items.BUCKET);
            } else {
               return this.defaultDispenseItemBehavior.dispense(p_302435_, p_123562_);
            }
         }
      };
      DispenserBlock.registerBehavior(Items.LAVA_BUCKET, dispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.WATER_BUCKET, dispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.POWDER_SNOW_BUCKET, dispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.SALMON_BUCKET, dispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.COD_BUCKET, dispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.PUFFERFISH_BUCKET, dispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.TROPICAL_FISH_BUCKET, dispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.AXOLOTL_BUCKET, dispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.TADPOLE_BUCKET, dispenseitembehavior1);
      DispenserBlock.registerBehavior(Items.BUCKET, new DefaultDispenseItemBehavior() {
         private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

         @Override
         public ItemStack execute(BlockSource p_302464_, ItemStack p_123567_) {
            LevelAccessor levelaccessor = p_302464_.level();
            BlockPos blockpos = p_302464_.pos().relative(p_302464_.state().getValue(DispenserBlock.FACING));
            BlockState blockstate = levelaccessor.getBlockState(blockpos);
            Block block = blockstate.getBlock();
            if (block instanceof BucketPickup bucketpickup) {
               ItemStack itemstack = bucketpickup.pickupBlock(null, levelaccessor, blockpos, blockstate);
               if (itemstack.isEmpty()) {
                  return super.execute(p_302464_, p_123567_);
               } else {
                  levelaccessor.gameEvent(null, GameEvent.FLUID_PICKUP, blockpos);
                  Item item = itemstack.getItem();
                  p_123567_.shrink(1);
                  if (p_123567_.isEmpty()) {
                     return new ItemStack(item);
                  } else {
                     if (p_302464_.blockEntity().addItem(new ItemStack(item)) < 0) {
                        this.defaultDispenseItemBehavior.dispense(p_302464_, new ItemStack(item));
                     }

                     return p_123567_;
                  }
               }
            } else {
               return super.execute(p_302464_, p_123567_);
            }
         }
      });
      DispenserBlock.registerBehavior(Items.FLINT_AND_STEEL, new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_302453_, ItemStack p_123413_) {
            Level level = p_302453_.level();
            this.setSuccess(true);
            Direction direction = p_302453_.state().getValue(DispenserBlock.FACING);
            BlockPos blockpos = p_302453_.pos().relative(direction);
            BlockState blockstate = level.getBlockState(blockpos);
            if (BaseFireBlock.canBePlacedAt(level, blockpos, direction)) {
               level.setBlockAndUpdate(blockpos, BaseFireBlock.getState(level, blockpos));
               level.gameEvent(null, GameEvent.BLOCK_PLACE, blockpos);
            } else if (CampfireBlock.canLight(blockstate) || CandleBlock.canLight(blockstate) || CandleCakeBlock.canLight(blockstate)) {
               level.setBlockAndUpdate(blockpos, blockstate.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)));
               level.gameEvent(null, GameEvent.BLOCK_CHANGE, blockpos);
            } else if (blockstate.isFlammable(level, blockpos, p_302453_.state().getValue(DispenserBlock.FACING).getOpposite())) {
               blockstate.onCaughtFire(level, blockpos, p_302453_.state().getValue(DispenserBlock.FACING).getOpposite(), null);
               if (blockstate.getBlock() instanceof TntBlock)
                  level.removeBlock(blockpos, false);
            } else {
               this.setSuccess(false);
            }

            if (this.isSuccess() && p_123413_.hurt(1, level.random, null)) {
               p_123413_.setCount(0);
            }

            return p_123413_;
         }
      });
      DispenserBlock.registerBehavior(Items.BONE_MEAL, new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_302429_, ItemStack p_123417_) {
            this.setSuccess(true);
            Level level = p_302429_.level();
            BlockPos blockpos = p_302429_.pos().relative(p_302429_.state().getValue(DispenserBlock.FACING));
            if (!BoneMealItem.growCrop(p_123417_, level, blockpos) && !BoneMealItem.growWaterPlant(p_123417_, level, blockpos, null)) {
               this.setSuccess(false);
            } else if (!level.isClientSide) {
               level.levelEvent(1505, blockpos, 0);
            }

            return p_123417_;
         }
      });
      DispenserBlock.registerBehavior(Blocks.TNT, new DefaultDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_302466_, ItemStack p_123426_) {
            Level level = p_302466_.level();
            BlockPos blockpos = p_302466_.pos().relative(p_302466_.state().getValue(DispenserBlock.FACING));
            PrimedTnt primedtnt = new PrimedTnt(level, (double)blockpos.getX() + 0.5, (double)blockpos.getY(), (double)blockpos.getZ() + 0.5, null);
            level.addFreshEntity(primedtnt);
            level.playSound(null, primedtnt.getX(), primedtnt.getY(), primedtnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.ENTITY_PLACE, blockpos);
            p_123426_.shrink(1);
            return p_123426_;
         }
      });
      DispenseItemBehavior dispenseitembehavior = new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_302449_, ItemStack p_123430_) {
            this.setSuccess(ArmorItem.dispenseArmor(p_302449_, p_123430_));
            return p_123430_;
         }
      };
      DispenserBlock.registerBehavior(Items.CREEPER_HEAD, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.ZOMBIE_HEAD, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.DRAGON_HEAD, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.SKELETON_SKULL, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.PIGLIN_HEAD, dispenseitembehavior);
      DispenserBlock.registerBehavior(Items.PLAYER_HEAD, dispenseitembehavior);
      DispenserBlock.registerBehavior(
         Items.WITHER_SKELETON_SKULL,
         new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource p_302428_, ItemStack p_123434_) {
               Level level = p_302428_.level();
               Direction direction = p_302428_.state().getValue(DispenserBlock.FACING);
               BlockPos blockpos = p_302428_.pos().relative(direction);
               if (level.isEmptyBlock(blockpos) && WitherSkullBlock.canSpawnMob(level, blockpos, p_123434_)) {
                  level.setBlock(
                     blockpos,
                     Blocks.WITHER_SKELETON_SKULL
                        .defaultBlockState()
                        .setValue(SkullBlock.ROTATION, Integer.valueOf(RotationSegment.convertToSegment(direction))),
                     3
                  );
                  level.gameEvent(null, GameEvent.BLOCK_PLACE, blockpos);
                  BlockEntity blockentity = level.getBlockEntity(blockpos);
                  if (blockentity instanceof SkullBlockEntity) {
                     WitherSkullBlock.checkSpawn(level, blockpos, (SkullBlockEntity)blockentity);
                  }
   
                  p_123434_.shrink(1);
                  this.setSuccess(true);
               } else {
                  this.setSuccess(ArmorItem.dispenseArmor(p_302428_, p_123434_));
               }
   
               return p_123434_;
            }
         }
      );
      DispenserBlock.registerBehavior(Blocks.CARVED_PUMPKIN, new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource p_302468_, ItemStack p_123438_) {
            Level level = p_302468_.level();
            BlockPos blockpos = p_302468_.pos().relative(p_302468_.state().getValue(DispenserBlock.FACING));
            CarvedPumpkinBlock carvedpumpkinblock = (CarvedPumpkinBlock)Blocks.CARVED_PUMPKIN;
            if (level.isEmptyBlock(blockpos) && carvedpumpkinblock.canSpawnGolem(level, blockpos)) {
               if (!level.isClientSide) {
                  level.setBlock(blockpos, carvedpumpkinblock.defaultBlockState(), 3);
                  level.gameEvent(null, GameEvent.BLOCK_PLACE, blockpos);
               }

               p_123438_.shrink(1);
               this.setSuccess(true);
            } else {
               this.setSuccess(ArmorItem.dispenseArmor(p_302468_, p_123438_));
            }

            return p_123438_;
         }
      });
      DispenserBlock.registerBehavior(Blocks.SHULKER_BOX.asItem(), new ShulkerBoxDispenseBehavior());

      for(DyeColor dyecolor : DyeColor.values()) {
         DispenserBlock.registerBehavior(ShulkerBoxBlock.getBlockByColor(dyecolor).asItem(), new ShulkerBoxDispenseBehavior());
      }

      DispenserBlock.registerBehavior(
         Items.GLASS_BOTTLE.asItem(),
         new OptionalDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
   
            private ItemStack takeLiquid(BlockSource p_302469_, ItemStack p_123448_, ItemStack p_123449_) {
               p_123448_.shrink(1);
               if (p_123448_.isEmpty()) {
                  p_302469_.level().gameEvent(null, GameEvent.FLUID_PICKUP, p_302469_.pos());
                  return p_123449_.copy();
               } else {
                  if (p_302469_.blockEntity().addItem(p_123449_.copy()) < 0) {
                     this.defaultDispenseItemBehavior.dispense(p_302469_, p_123449_.copy());
                  }
   
                  return p_123448_;
               }
            }
   
            @Override
            public ItemStack execute(BlockSource p_302437_, ItemStack p_123445_) {
               this.setSuccess(false);
               ServerLevel serverlevel = p_302437_.level();
               BlockPos blockpos = p_302437_.pos().relative(p_302437_.state().getValue(DispenserBlock.FACING));
               BlockState blockstate = serverlevel.getBlockState(blockpos);
               if (blockstate.is(
                     BlockTags.BEEHIVES, p_123442_ -> p_123442_.hasProperty(BeehiveBlock.HONEY_LEVEL) && p_123442_.getBlock() instanceof BeehiveBlock
                  )
                  && blockstate.getValue(BeehiveBlock.HONEY_LEVEL) >= 5) {
                  ((BeehiveBlock)blockstate.getBlock())
                     .releaseBeesAndResetHoneyLevel(serverlevel, blockstate, blockpos, null, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
                  this.setSuccess(true);
                  return this.takeLiquid(p_302437_, p_123445_, new ItemStack(Items.HONEY_BOTTLE));
               } else if (serverlevel.getFluidState(blockpos).is(FluidTags.WATER)) {
                  this.setSuccess(true);
                  return this.takeLiquid(p_302437_, p_123445_, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
               } else {
                  return super.execute(p_302437_, p_123445_);
               }
            }
         }
      );
      DispenserBlock.registerBehavior(Items.GLOWSTONE, new OptionalDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource p_302454_, ItemStack p_123453_) {
            Direction direction = p_302454_.state().getValue(DispenserBlock.FACING);
            BlockPos blockpos = p_302454_.pos().relative(direction);
            Level level = p_302454_.level();
            BlockState blockstate = level.getBlockState(blockpos);
            this.setSuccess(true);
            if (blockstate.is(Blocks.RESPAWN_ANCHOR)) {
               if (blockstate.getValue(RespawnAnchorBlock.CHARGE) != 4) {
                  RespawnAnchorBlock.charge(null, level, blockpos, blockstate);
                  p_123453_.shrink(1);
               } else {
                  this.setSuccess(false);
               }

               return p_123453_;
            } else {
               return super.execute(p_302454_, p_123453_);
            }
         }
      });
      DispenserBlock.registerBehavior(Items.SHEARS.asItem(), new ShearsDispenseItemBehavior());
      DispenserBlock.registerBehavior(Items.HONEYCOMB, new OptionalDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource p_302447_, ItemStack p_175748_) {
            BlockPos blockpos = p_302447_.pos().relative(p_302447_.state().getValue(DispenserBlock.FACING));
            Level level = p_302447_.level();
            BlockState blockstate = level.getBlockState(blockpos);
            Optional<BlockState> optional = HoneycombItem.getWaxed(blockstate);
            if (optional.isPresent()) {
               level.setBlockAndUpdate(blockpos, optional.get());
               level.levelEvent(3003, blockpos, 0);
               p_175748_.shrink(1);
               this.setSuccess(true);
               return p_175748_;
            } else {
               return super.execute(p_302447_, p_175748_);
            }
         }
      });
      DispenserBlock.registerBehavior(
         Items.POTION,
         new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
   
            @Override
            public ItemStack execute(BlockSource p_302442_, ItemStack p_235897_) {
               if (PotionUtils.getPotion(p_235897_) != Potions.WATER) {
                  return this.defaultDispenseItemBehavior.dispense(p_302442_, p_235897_);
               } else {
                  ServerLevel serverlevel = p_302442_.level();
                  BlockPos blockpos = p_302442_.pos();
                  BlockPos blockpos1 = p_302442_.pos().relative(p_302442_.state().getValue(DispenserBlock.FACING));
                  if (!serverlevel.getBlockState(blockpos1).is(BlockTags.CONVERTABLE_TO_MUD)) {
                     return this.defaultDispenseItemBehavior.dispense(p_302442_, p_235897_);
                  } else {
                     if (!serverlevel.isClientSide) {
                        for(int i = 0; i < 5; ++i) {
                           serverlevel.sendParticles(
                              ParticleTypes.SPLASH,
                              (double)blockpos.getX() + serverlevel.random.nextDouble(),
                              (double)(blockpos.getY() + 1),
                              (double)blockpos.getZ() + serverlevel.random.nextDouble(),
                              1,
                              0.0,
                              0.0,
                              0.0,
                              1.0
                           );
                        }
                     }
   
                     serverlevel.playSound(null, blockpos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                     serverlevel.gameEvent(null, GameEvent.FLUID_PLACE, blockpos);
                     serverlevel.setBlockAndUpdate(blockpos1, Blocks.MUD.defaultBlockState());
                     return new ItemStack(Items.GLASS_BOTTLE);
                  }
               }
            }
         }
      );
   }

   static Vec3 getEntityPokingOutOfBlockPos(BlockSource p_302467_, EntityType<?> p_302422_, Direction p_302439_) {
      return p_302467_.center()
         .add(
            (double)p_302439_.getStepX() * (0.5000099999997474 - (double)p_302422_.getWidth() / 2.0),
            (double)p_302439_.getStepY() * (0.5000099999997474 - (double)p_302422_.getHeight() / 2.0) - (double)p_302422_.getHeight() / 2.0,
            (double)p_302439_.getStepZ() * (0.5000099999997474 - (double)p_302422_.getWidth() / 2.0)
         );
   }
}
