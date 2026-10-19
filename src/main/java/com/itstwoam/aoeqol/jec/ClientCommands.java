package com.itstwoam.aoeqol.jec;


import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.ClientCommandHandler;

/** Client-side commands (work without cheats and on servers). Client only.
 *  /jectags [clear]  - items JEC failed to match because of a tag or damage value
 *  /jecunify         - apply all saved ore dictionary preferences to every saved recipe */
public class ClientCommands extends CommandBase {
    private final String name;

    private ClientCommands(String name) { this.name = name; }

    /** Prepended to ProxyClient.init(). */
    public static void register() {
        try {
            ClientCommandHandler.instance.registerCommand(new ClientCommands("jectags"));
            ClientCommandHandler.instance.registerCommand(new ClientCommands("jecunify"));
        } catch (Throwable t) {
            System.out.println("[AoE-QoL] Could not register commands: " + t);
        }
    }

    public String getCommandName() { return name; }

    public String getCommandUsage(ICommandSender sender) {
        return name.equals("jecunify")
            ? "/jecunify - replace items in all saved recipes with your ore dictionary preferences"
            : "/jectags [clear] - items JEC failed to match because of a tag or damage value";
    }

    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (name.equals("jecunify")) OreUnify.applyAll(sender);
        else NbtCompat.handleCommand(sender, args);
    }

    public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return true; }

    public int getRequiredPermissionLevel() { return 0; }
}
