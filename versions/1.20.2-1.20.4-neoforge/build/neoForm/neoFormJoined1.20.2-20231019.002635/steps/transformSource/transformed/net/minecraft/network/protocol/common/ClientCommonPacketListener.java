package net.minecraft.network.protocol.common;

import net.minecraft.network.ClientboundPacketListener;

public interface ClientCommonPacketListener extends ClientboundPacketListener {
   void handleKeepAlive(ClientboundKeepAlivePacket p_295236_);

   void handlePing(ClientboundPingPacket p_296451_);

   void handleCustomPayload(ClientboundCustomPayloadPacket p_294344_);

   void handleDisconnect(ClientboundDisconnectPacket p_294847_);

   void handleResourcePack(ClientboundResourcePackPacket p_295747_);

   void handleUpdateTags(ClientboundUpdateTagsPacket p_294883_);
}
