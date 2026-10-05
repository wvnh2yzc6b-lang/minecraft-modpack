package com.warfront.network;

/** Last mana values received from the server; read by the HUD. Plain fields, safe on either side. */
public final class ClientManaState {
    public static volatile float mana;
    public static volatile float max = 100F;

    private ClientManaState() {}
}
