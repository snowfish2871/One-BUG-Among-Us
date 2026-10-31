package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ClientboundUpdateAdvancementsPacket implements Packet<ClientGamePacketListener> {
   private final boolean reset;
   private final List<AdvancementHolder> added;
   private final Set<ResourceLocation> removed;
   private final Map<ResourceLocation, AdvancementProgress> progress;

   public ClientboundUpdateAdvancementsPacket(
      boolean p_133560_, Collection<AdvancementHolder> p_133561_, Set<ResourceLocation> p_133562_, Map<ResourceLocation, AdvancementProgress> p_133563_
   ) {
      this.reset = p_133560_;
      this.added = List.copyOf(p_133561_);
      this.removed = Set.copyOf(p_133562_);
      this.progress = Map.copyOf(p_133563_);
   }

   public ClientboundUpdateAdvancementsPacket(FriendlyByteBuf p_179439_) {
      this.reset = p_179439_.readBoolean();
      this.added = p_179439_.readList(AdvancementHolder::read);
      this.removed = p_179439_.readCollection(Sets::newLinkedHashSetWithExpectedSize, FriendlyByteBuf::readResourceLocation);
      this.progress = p_179439_.readMap(FriendlyByteBuf::readResourceLocation, AdvancementProgress::fromNetwork);
   }

   @Override
   public void write(FriendlyByteBuf p_133572_) {
      p_133572_.writeBoolean(this.reset);
      p_133572_.writeCollection(this.added, (p_300719_, p_300720_) -> p_300720_.write(p_300719_));
      p_133572_.writeCollection(this.removed, FriendlyByteBuf::writeResourceLocation);
      p_133572_.writeMap(this.progress, FriendlyByteBuf::writeResourceLocation, (p_179444_, p_179445_) -> p_179445_.serializeToNetwork(p_179444_));
   }

   public void handle(ClientGamePacketListener p_133569_) {
      p_133569_.handleUpdateAdvancementsPacket(this);
   }

   public List<AdvancementHolder> getAdded() {
      return this.added;
   }

   public Set<ResourceLocation> getRemoved() {
      return this.removed;
   }

   public Map<ResourceLocation, AdvancementProgress> getProgress() {
      return this.progress;
   }

   public boolean shouldReset() {
      return this.reset;
   }
}
