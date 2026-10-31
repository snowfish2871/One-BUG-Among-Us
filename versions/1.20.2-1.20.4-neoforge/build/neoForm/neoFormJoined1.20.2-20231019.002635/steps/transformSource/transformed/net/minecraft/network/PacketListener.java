package net.minecraft.network;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public interface PacketListener {
   PacketFlow flow();

   ConnectionProtocol protocol();

   void onDisconnect(Component p_130552_);

   boolean isAcceptingMessages();

   default boolean shouldHandleMessage(Packet<?> p_295210_) {
      return this.isAcceptingMessages();
   }

   default boolean shouldPropagateHandlingExceptions() {
      return true;
   }
}
