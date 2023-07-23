package org.teacon.checkin.network.protocol.game;

import net.minecraft.network.chat.Component;

public class SanitizeException extends Exception {
    private final Component msg;
    public SanitizeException(Component msg) {
        super(msg.getString());
        this.msg = msg;
    }

    public Component getMsg() {
        return msg;
    }
}
