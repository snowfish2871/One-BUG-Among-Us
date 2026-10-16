package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProviders;

public class CopyNbtFunction extends LootItemConditionalFunction {
   public static final Codec<CopyNbtFunction> CODEC = RecordCodecBuilder.create(
      p_298070_ -> commonFields(p_298070_)
            .and(
               p_298070_.group(
                  NbtProviders.CODEC.fieldOf("source").forGetter(p_298069_ -> p_298069_.source),
                  CopyNbtFunction.CopyOperation.CODEC.listOf().fieldOf("ops").forGetter(p_298071_ -> p_298071_.operations)
               )
            )
            .apply(p_298070_, CopyNbtFunction::new)
   );
   private final NbtProvider source;
   private final List<CopyNbtFunction.CopyOperation> operations;

   CopyNbtFunction(List<LootItemCondition> p_165177_, NbtProvider p_165176_, List<CopyNbtFunction.CopyOperation> p_298837_) {
      super(p_165177_);
      this.source = p_165176_;
      this.operations = List.copyOf(p_298837_);
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.COPY_NBT;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.source.getReferencedContextParams();
   }

   @Override
   public ItemStack run(ItemStack p_80250_, LootContext p_80251_) {
      Tag tag = this.source.get(p_80251_);
      if (tag != null) {
         this.operations.forEach(p_80255_ -> p_80255_.apply(p_80250_::getOrCreateTag, tag));
      }

      return p_80250_;
   }

   public static CopyNbtFunction.Builder copyData(NbtProvider p_165181_) {
      return new CopyNbtFunction.Builder(p_165181_);
   }

   public static CopyNbtFunction.Builder copyData(LootContext.EntityTarget p_165179_) {
      return new CopyNbtFunction.Builder(ContextNbtProvider.forContextEntity(p_165179_));
   }

   public static class Builder extends LootItemConditionalFunction.Builder<CopyNbtFunction.Builder> {
      private final NbtProvider source;
      private final List<CopyNbtFunction.CopyOperation> ops = Lists.newArrayList();

      Builder(NbtProvider p_165183_) {
         this.source = p_165183_;
      }

      public CopyNbtFunction.Builder copy(String p_80283_, String p_80284_, CopyNbtFunction.MergeStrategy p_80285_) {
         try {
            this.ops.add(new CopyNbtFunction.CopyOperation(CopyNbtFunction.Path.of(p_80283_), CopyNbtFunction.Path.of(p_80284_), p_80285_));
            return this;
         } catch (CommandSyntaxException commandsyntaxexception) {
            throw new IllegalArgumentException(commandsyntaxexception);
         }
      }

      public CopyNbtFunction.Builder copy(String p_80280_, String p_80281_) {
         return this.copy(p_80280_, p_80281_, CopyNbtFunction.MergeStrategy.REPLACE);
      }

      protected CopyNbtFunction.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new CopyNbtFunction(this.getConditions(), this.source, this.ops);
      }
   }

   static record CopyOperation(CopyNbtFunction.Path sourcePath, CopyNbtFunction.Path targetPath, CopyNbtFunction.MergeStrategy op) {
      public static final Codec<CopyNbtFunction.CopyOperation> CODEC = RecordCodecBuilder.create(
         p_298936_ -> p_298936_.group(
                  CopyNbtFunction.Path.CODEC.fieldOf("source").forGetter(CopyNbtFunction.CopyOperation::sourcePath),
                  CopyNbtFunction.Path.CODEC.fieldOf("target").forGetter(CopyNbtFunction.CopyOperation::targetPath),
                  CopyNbtFunction.MergeStrategy.CODEC.fieldOf("op").forGetter(CopyNbtFunction.CopyOperation::op)
               )
               .apply(p_298936_, CopyNbtFunction.CopyOperation::new)
      );

      public void apply(Supplier<Tag> p_80306_, Tag p_80307_) {
         try {
            List<Tag> list = this.sourcePath.path().get(p_80307_);
            if (!list.isEmpty()) {
               this.op.merge(p_80306_.get(), this.targetPath.path(), list);
            }
         } catch (CommandSyntaxException commandsyntaxexception) {
         }
      }
   }

   public static enum MergeStrategy implements StringRepresentable {
      REPLACE("replace") {
         @Override
         public void merge(Tag p_80362_, NbtPathArgument.NbtPath p_80363_, List<Tag> p_80364_) throws CommandSyntaxException {
            p_80363_.set(p_80362_, Iterables.getLast(p_80364_));
         }
      },
      APPEND("append") {
         @Override
         public void merge(Tag p_80373_, NbtPathArgument.NbtPath p_80374_, List<Tag> p_80375_) throws CommandSyntaxException {
            List<Tag> list = p_80374_.getOrCreate(p_80373_, ListTag::new);
            list.forEach(p_80371_ -> {
               if (p_80371_ instanceof ListTag) {
                  p_80375_.forEach(p_165187_ -> ((ListTag)p_80371_).add(p_165187_.copy()));
               }
            });
         }
      },
      MERGE("merge") {
         @Override
         public void merge(Tag p_80387_, NbtPathArgument.NbtPath p_80388_, List<Tag> p_80389_) throws CommandSyntaxException {
            List<Tag> list = p_80388_.getOrCreate(p_80387_, CompoundTag::new);
            list.forEach(p_80385_ -> {
               if (p_80385_ instanceof CompoundTag) {
                  p_80389_.forEach(p_165190_ -> {
                     if (p_165190_ instanceof CompoundTag) {
                        ((CompoundTag)p_80385_).merge((CompoundTag)p_165190_);
                     }
                  });
               }
            });
         }
      };

      public static final Codec<CopyNbtFunction.MergeStrategy> CODEC = StringRepresentable.fromEnum(CopyNbtFunction.MergeStrategy::values);
      private final String name;

      public abstract void merge(Tag p_80351_, NbtPathArgument.NbtPath p_80352_, List<Tag> p_80353_) throws CommandSyntaxException;

      MergeStrategy(String p_80341_) {
         this.name = p_80341_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   static record Path(String string, NbtPathArgument.NbtPath path) {
      public static final Codec<CopyNbtFunction.Path> CODEC = Codec.STRING.comapFlatMap(p_298670_ -> {
         try {
            return DataResult.success(of(p_298670_));
         } catch (CommandSyntaxException commandsyntaxexception) {
            return DataResult.error(() -> "Failed to parse path " + p_298670_ + ": " + commandsyntaxexception.getMessage());
         }
      }, CopyNbtFunction.Path::string);

      public static CopyNbtFunction.Path of(String p_298251_) throws CommandSyntaxException {
         NbtPathArgument.NbtPath nbtpathargument$nbtpath = new NbtPathArgument().parse(new StringReader(p_298251_));
         return new CopyNbtFunction.Path(p_298251_, nbtpathargument$nbtpath);
      }
   }
}
