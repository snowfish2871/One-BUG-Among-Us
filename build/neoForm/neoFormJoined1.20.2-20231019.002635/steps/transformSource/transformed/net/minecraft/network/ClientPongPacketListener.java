package net.minecraft.network;

import net.minecraft.network.protocol.status.ClientboundPongResponsePacket;

public interface ClientPongPacketListener extends PacketListener {
   void handlePongResponse(ClientboundPongResponsePacket p_299154_);
}
