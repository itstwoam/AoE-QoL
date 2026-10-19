package com.itstwoam.aoeqol.jec;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.TextComponentString;

/** Client-only: shows a line in the player's chat. Kept separate so server code never loads client classes. */
final class ClientChat {
    private ClientChat() {}

    static void show(String s) {
        ICommandSender player = Minecraft.getMinecraft().thePlayer;
        if (player != null) player.addChatMessage(new TextComponentString(s));
    }
}
