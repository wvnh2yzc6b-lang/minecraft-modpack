package com.warfront.client;

import com.warfront.network.AltarOpenPayload;
import net.minecraft.client.Minecraft;

/** Client-only entry points called from network handlers, kept apart so servers never load screen classes. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openAltar(AltarOpenPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AltarScreen open && open.pos().equals(payload.pos())) open.refresh(payload);
        else mc.setScreen(new AltarScreen(payload));
    }
}
