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
import java.lang.reflect.Method;
import java.util.function.Consumer;

import net.neoforged.bus.api.IEventBus;
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

    // 两套事件类在编译期不可能同时存在，所以只能走反射：先按名字拿到类，再按类挂监听器。
    //
    // 这里不能用 bus.register(带 @SubscribeEvent 的匿名对象) 那一套：匿名类的
    // handle(Event) 参数写的是 Event 本身，而事件总线不接受给抽象类注册监听器，
    // 实机上会直接抛
    //   IllegalArgumentException: Cannot register listeners for abstract class
    //   net.neoforged.bus.api.Event
    // 一个监听器都挂不上。addListener(Class, Consumer) 收的是具体事件类，没有这个问题。
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void register() {
        IEventBus bus = NeoForge.EVENT_BUS;

        // 服务器停止事件两边是同一个具体类，直接挂，不跟 tick 绑在一起：
        // 以前两个放在同一个 try 里，tick 一抛，停止通知也跟着没了
        try {
            bus.addListener((Consumer<ServerStoppingEvent>) event -> onStopping.run());
        } catch (Throwable t) {
            grumble(t);
        }

        if (flavour.isEmpty()) {
            return;
        }
        try {
            // 两套事件类里只有一套在运行时存在，按探测到的那个名字取 Class
            Class<?> eventClass = Class.forName(flavour, false, NeoForgeTickHook.class.getClassLoader());
            Method addListener = IEventBus.class.getMethod("addListener", Class.class, Consumer.class);
            Consumer<Object> consumer = event -> {
                if (MODERN.equals(flavour)) {
                    // 新版一次 tick 只触发一回（Pre / Post 拆成了两个子类）
                    onTick.run();
                } else if (isTickEnd(event)) {
                    // 老版一次 tick 会触发 START 和 END 两回，只认 END，
                    // 这样和新版一次 tick 触发一回的节奏对得上
                    onTick.run();
                }
            };
            addListener.invoke(bus, eventClass, consumer);
        } catch (Throwable t) {
            // 挂不上就退化成"没有 tick 感知"，看门狗还在，只是不测 tick 间隔
            grumble(t);
        }
    }

    private static void grumble(Throwable t) {
        if (!grumbled) {
            grumbled = true;
            System.out.println(Oba.LOG_PREFIX + Lang.get("tick.adapterFailed", t));
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
