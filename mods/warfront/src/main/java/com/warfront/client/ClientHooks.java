package com.warfront.client;

import com.warfront.network.AltarOpenPayload;
import net.minecraft.client.Minecraft;

/** Client-only entry points called from network handlers, kept apart so servers never load screen classes. */
public final class ClientHooks {
    private ClientHooks() {}

    /** A themed toast or the siege banner; also a chat line if the player asked for chat alerts. */
    public static void alert(com.warfront.network.AlertPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (p.kind() == 1) com.warfront.client.ui.SiegeBanner.show(p.title(), p.text(), p.accent());
        else mc.getToasts().addToast(new com.warfront.client.ui.WFToast(p.title(), p.text(), p.accent()));
        if (com.warfront.config.WFClientConfig.CHAT_ALERTS.get() && mc.player != null) {
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(p.title()
                    + (p.text().isEmpty() ? "" : ": " + p.text())).withColor(p.accent()), false);
        }
    }

    public static void openAltar(AltarOpenPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AltarScreen open && open.pos().equals(payload.pos())) open.refresh(payload);
        else mc.setScreen(new AltarScreen(payload));
    }
}
