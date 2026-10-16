package net.minecraft.network.chat.contents;

import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record StorageDataSource(ResourceLocation id) implements DataSource {
   @Override
   public Stream<CompoundTag> getData(CommandSourceStack p_237491_) {
      CompoundTag compoundtag = p_237491_.getServer().getCommandStorage().get(this.id);
      return Stream.of(compoundtag);
   }

   @Override
   public String toString() {
      return "storage=" + this.id;
   }
}
