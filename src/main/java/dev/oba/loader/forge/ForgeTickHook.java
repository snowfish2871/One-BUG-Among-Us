// Forge 1.20.1 上的服务器 tick 挂载点。
//
// 这里只有一套事件，不像 NeoForge 那样中途换过类：
//   net.minecraftforge.event.TickEvent$ServerTickEvent，带 Phase（START / END），
//   一次 tick 触发两回，只认 END 才能和"一次 tick 一回"的节奏对上。
package dev.oba.loader.forge;

import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.loader.TickHook;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ForgeTickHook implements TickHook {

    private final Runnable onTick;
    private final Runnable onStopping;
    private static volatile boolean grumbled;

    public ForgeTickHook(Runnable onTick, Runnable onStopping) {
        this.onTick = onTick;
        this.onStopping = onStopping;
    }

    @Override
    public String describe() {
        return "net.minecraftforge.event.TickEvent$ServerTickEvent";
    }

    @Override
    public void onTick() {
        onTick.run();
    }

    @Override
    public void onStopping() {
        onStopping.run();
    }

    // 这套事件在编译期就是确定的，不用反射，直接挂
    @Override
    public void register() {
        try {
            Object handler = new Object() {
                @SubscribeEvent
                public void handle(TickEvent.ServerTickEvent event) {
                    // START 和 END 都会进来，只在 END 记一笔
                    if (event.phase == TickEvent.Phase.END) {
                        onTick.run();
                    }
                }
            };
            MinecraftForge.EVENT_BUS.register(handler);
        } catch (Throwable t) {
            // 挂不上就退化成"没有 tick 感知"，看门狗还在，只是不测 tick 间隔
            if (!grumbled) {
                grumbled = true;
                System.out.println(Oba.LOG_PREFIX + Lang.get("tick.adapterFailed", t));
            }
        }
    }
}
