// NeoForge 上的服务器 tick 挂载点。
//
// NeoForge 在 1.20.5 前后把 tick 事件重做了一遍，两边的类完全不同：
//   1.20.2 ~ 1.20.4   （老版）net.neoforged.neoforge.event.TickEvent$ServerTickEvent
//                     靠 Phase（START / END）区分前后，一次 tick 触发两回
//   1.20.6 及以后      net.neoforged.neoforge.event.tick.ServerTickEvent$Post
//                     拆成 Pre / Post 两个子类，一次 tick 只触发一回
//
// 运行时探一下哪套在，然后注册对应的监听器。
package dev.oba.loader.neoforge;

import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.loader.TickHook;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class NeoForgeTickHook implements TickHook {

    // 1.20.6 以后的那套
    private static final String MODERN = "net.neoforged.neoforge.event.tick.ServerTickEvent$Post";
    // 1.20.2 ~ 1.20.4 的那套（内部类的 forName 写法就是这样）
    private static final String LEGACY = "net.neoforged.neoforge.event.TickEvent$ServerTickEvent";

    private final Runnable onTick;
    private final Runnable onStopping;
    private final String flavour;
    private static volatile boolean grumbled;

    public NeoForgeTickHook(Runnable onTick, Runnable onStopping) {
        this.onTick = onTick;
        this.onStopping = onStopping;

        String found = null;
        try {
            Class.forName(MODERN, false, NeoForgeTickHook.class.getClassLoader());
            found = MODERN;
        } catch (Throwable ignored) {
            // 不是新版，继续试老版
        }
        if (found == null) {
            try {
                Class.forName(LEGACY, false, NeoForgeTickHook.class.getClassLoader());
                found = LEGACY;
            } catch (Throwable ignored) {
                // 两套都没有，那就什么都不挂
            }
        }
        this.flavour = found == null ? "" : found;
    }

    @Override
    public String describe() {
        return flavour;
    }

    @Override
    public void onTick() {
        onTick.run();
    }

    @Override
    public void onStopping() {
        onStopping.run();
    }

    // 两套事件类在编译期不可能同时存在，所以只能走反射：
    // 先按名字拿到类，再把监听器挂上去。
    // 事件总线的类型参数在编译期会擦除成 Event，这样注册是安全的
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void register() {
        try {
            IEventBus bus = NeoForge.EVENT_BUS;
            if (MODERN.equals(flavour)) {
                Class<?> eventClass = Class.forName(MODERN);
                Object handler = new Object() {
                    @SubscribeEvent
                    public void handle(Event event) {
                        // 只认这一个事件类型，别的直接忽略
                        if (eventClass.isInstance(event)) {
                            onTick.run();
                        }
                    }
                };
                bus.register(handler);
            } else if (LEGACY.equals(flavour)) {
                // 老版一次 tick 会触发 START 和 END 两回，只认 END，
                // 这样和新版一次 tick 触发一回的节奏对得上
                Object handler = new Object() {
                    @SubscribeEvent
                    public void handle(Event event) {
                        if (isTickEnd(event)) {
                            onTick.run();
                        }
                    }
                };
                bus.register(handler);
            }

            // 服务器停止事件两边一样，直接挂
            Object stopHandler = new Object() {
                @SubscribeEvent
                public void handle(ServerStoppingEvent event) {
                    onStopping.run();
                }
            };
            bus.register(stopHandler);
        } catch (Throwable t) {
            // 挂不上就退化成"没有 tick 感知"，看门狗还在，只是不测 tick 间隔
            if (!grumbled) {
                grumbled = true;
                System.out.println(Oba.LOG_PREFIX + Lang.get("tick.adapterFailed", t));
            }
        }
    }

    // 老版事件里 Phase 是 END 才算一次 tick 走完
    private static boolean isTickEnd(Object event) {
        try {
            Object phase = event.getClass().getMethod("phase").invoke(event);
            return phase != null && "END".equals(phase.toString());
        } catch (Throwable ignored) {
            // 拿不到 phase 就当成 END，宁可多记一次也别漏
            return true;
        }
    }

    // 按"这个版本上有哪些类"决定走哪条路。自测直接喂布尔值进来，
    // 不需要真的加载某个版本的游戏
    public static Flavour classify(boolean hasModern, boolean hasLegacy) {
        // 新版优先：同一条线上不可能两套都在，但万一共存，新的更可信
        if (hasModern) {
            return Flavour.MODERN_POST;
        }
        return hasLegacy ? Flavour.LEGACY_PHASE : Flavour.NONE;
    }

    public enum Flavour {
        // 1.20.5 及以后
        MODERN_POST,
        // 1.20.2 到 1.20.4 用的那一套
        LEGACY_PHASE,
        // 没找到任何一套，tick 间隔检测停用
        NONE
    }

    public static boolean isModern(String className) {
        return MODERN.equals(className);
    }

    public static boolean isLegacy(String className) {
        return LEGACY.equals(className);
    }

    // 这次实际用的是哪一套
    public Flavour detected() {
        return classify(MODERN.equals(flavour), LEGACY.equals(flavour));
    }
}
