package com.warfront.client;

import com.warfront.network.AltarOpenPayload;
import net.minecraft.client.Minecraft;

/** Client-only entry points called from network handlers, kept apart so servers never load screen classes. */
public final class ClientHooks {
    private ClientHooks() {}

    /** One of the advisor's lines, as a toast. */
    public static void advisorToast(String title, String line) {
        Minecraft mc = Minecraft.getInstance();
        String shortLine = line.length() > 60 ? line.substring(0, 57) + "..." : line;
        net.minecraft.client.gui.components.toasts.SystemToast.addOrUpdate(mc.getToasts(),
                net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                net.minecraft.network.chat.Component.literal(title), net.minecraft.network.chat.Component.literal(shortLine));
    }

    public static void openAltar(AltarOpenPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AltarScreen open && open.pos().equals(payload.pos())) open.refresh(payload);
        else mc.setScreen(new AltarScreen(payload));
    }
}
