package net.minecraft.network.protocol.common.custom;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record VillageSectionsDebugPayload(Set<SectionPos> villageChunks, Set<SectionPos> notVillageChunks) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/village_sections");

   public VillageSectionsDebugPayload(FriendlyByteBuf p_295961_) {
      this(p_295961_.readCollection(HashSet::new, FriendlyByteBuf::readSectionPos), p_295961_.readCollection(HashSet::new, FriendlyByteBuf::readSectionPos));
   }

   @Override
   public void write(FriendlyByteBuf p_295698_) {
      p_295698_.writeCollection(this.villageChunks, FriendlyByteBuf::writeSectionPos);
      p_295698_.writeCollection(this.notVillageChunks, FriendlyByteBuf::writeSectionPos);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}
