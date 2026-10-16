// 让聊天栏里的排版按真实字体宽度来算。
//
// 为什么需要：终端是等宽的，一格一个字；游戏里的字体不是——
// 空格 4 像素、小写字母 6、汉字 9。引擎那套按格补空格的写法搬进聊天栏，
// 横线长短、标签对齐全都会歪。这里把客户端字体量出来的宽度交给 GameUi。
//
// 只有客户端能用：专用服务器上 net.minecraft.client.Minecraft 这个类根本不存在，
// 所以两边都是先确认是客户端再装。装不上时 GameUi 会退回它自带的那张宽表，
// 那张表是照原版 ascii.png 一格格量出来的，差不到一个像素。
package dev.oba.mod;

import net.minecraft.client.Minecraft;

public final class ClientFont {

    public static void install() {
        GameUi.setMeasurer(text -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.font == null) {
                return 0;
            }
            return minecraft.font.width(text);
        });
    }

    private ClientFont() {
    }
}
