package net.minecraft.util.profiling.jfr.event;

import java.net.SocketAddress;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import net.minecraft.obfuscate.DontObfuscate;

@Name("minecraft.PacketReceived")
@Label("Network Packet Received")
@DontObfuscate
public class PacketReceivedEvent extends PacketEvent {
   public static final String NAME = "minecraft.PacketReceived";
   public static final EventType TYPE = EventType.getEventType(PacketReceivedEvent.class);

   public PacketReceivedEvent(String p_295637_, int p_195585_, SocketAddress p_195587_, int p_195586_) {
      super(p_295637_, p_195585_, p_195587_, p_195586_);
   }
}
