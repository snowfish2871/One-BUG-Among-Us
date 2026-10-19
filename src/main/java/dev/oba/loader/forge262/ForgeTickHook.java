// Forge 26.2 上的服务器 tick 挂载点。
//
// 26.x 的事件总线换成了 net.minecraftforge.eventbus.api.bus.EventBus，
// 每个事件类自己带一个静态 BUS 字段，直接 addListener 就行，不需要注解、也不用反射：
//   tick   net.minecraftforge.event.TickEvent$ServerTickEvent$Post
//  停服   net.minecraftforge.event.server.ServerStoppingEvent
//
// 旧版的 TickEvent$ServerTickEvent 在 26.x 变成了接口，Phase 字段随之消失，
// START/END 拆成了 Pre / Post 两个事件，这里只认 Post。
package dev.oba.loader.forge262;

import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.loader.TickHook;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

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
        return "net.minecraftforge.event.TickEvent$ServerTickEvent$Post";
    }

    @Override
    public void onTick() {
        onTick.run();
    }

    @Override
    public void onStopping() {
        onStopping.run();
    }

    @Override
    public void register() {
        // 两个监听器分开挂：停服那个挂不上不该连累 tick
        try {
            TickEvent.ServerTickEvent.Post.BUS.addListener(event -> onTick.run());
        } catch (Throwable t) {
            grumble(t);
        }
        try {
            ServerStoppingEvent.BUS.addListener(event -> onStopping.run());
        } catch (Throwable t) {
            grumble(t);
        }
    }

    private static void grumble(Throwable t) {
        // 挂不上就退化成"没有 tick 感知"，看门狗还在，只是不测 tick 间隔
        if (!grumbled) {
            grumbled = true;
            System.out.println(Oba.LOG_PREFIX + Lang.get("tick.adapterFailed", t));
        }
    }
}
