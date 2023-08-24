package com.pbtservers.minestomlauncher.command;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

public class ShutdownCommand extends Command {

	public ShutdownCommand() {
		super("shutdown", "stop", "end");

		addSyntax(this::execute);
	}

	private void execute(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
		if (commandSender instanceof Player player) {
			player.sendMessage(text("Este comando es solo de terminal!", NamedTextColor.RED));
			return;
		}

		MinecraftServer.stopCleanly();
	}

}