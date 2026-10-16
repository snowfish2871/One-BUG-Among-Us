package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.core.RegistryAccess;

public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    private final RegistryAccess registryAccess;

    public RegistryFriendlyByteBuf(ByteBuf p_320951_, RegistryAccess p_319803_) {
        super(p_320951_);
        this.registryAccess = p_319803_;
    }

    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess p_320166_) {
        return p_320793_ -> new RegistryFriendlyByteBuf(p_320793_, p_320166_);
    }
}
