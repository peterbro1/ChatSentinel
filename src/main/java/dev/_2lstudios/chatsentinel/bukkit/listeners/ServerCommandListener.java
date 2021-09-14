package dev._2lstudios.chatsentinel.bukkit.listeners;

import java.util.UUID;
import java.util.regex.Pattern;

import dev._2lstudios.chatsentinel.shared.modules.*;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import dev._2lstudios.chatsentinel.bukkit.modules.ModuleManager;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayerManager;
import dev._2lstudios.chatsentinel.shared.interfaces.Module;
import dev._2lstudios.chatsentinel.shared.utils.StringUtil;
import dev._2lstudios.chatsentinel.shared.utils.VersionUtil;

public class ServerCommandListener implements Listener {
	private final Plugin plugin;
	private final ModuleManager moduleManager;
	private final ChatPlayerManager chatPlayerManager;

	public ServerCommandListener(final Plugin plugin, final ModuleManager moduleManager,
			final ChatPlayerManager chatPlayerManager) {
		this.plugin = plugin;
		this.moduleManager = moduleManager;
		this.chatPlayerManager = chatPlayerManager;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onServerCommand(final PlayerCommandPreprocessEvent event) {
		final Player player = event.getPlayer();

		if (!player.hasPermission("chatsentinel.bypass")) {
			final UUID uuid = player.getUniqueId();
			final GeneralModule generalModule = moduleManager.getGeneralModule();
			final WhitelistModule whitelistModule = moduleManager.getWhitelistModule();
			final ChatPlayer chatPlayer = chatPlayerManager.getPlayer(uuid);
			final String originalMessage = event.getMessage().trim();
			final boolean isNormalCommand = generalModule.isCommand(originalMessage);
			String modifiedMessage;

			modifiedMessage = StringUtil.removeAccents(originalMessage);
			if (originalMessage.contains(" ")) {
				/*if (isNormalCommand)
					modifiedMessage = modifiedMessage.replace("/", "");*/

			} else {
				modifiedMessage = "/";
			}

			if (whitelistModule.isEnabled()) {
				final Pattern whitelistPattern = whitelistModule.getPattern();

				modifiedMessage = whitelistPattern.matcher(modifiedMessage).replaceAll("");
			}

			final MessagesModule messagesModule = moduleManager.getMessagesModule();
			final Server server = plugin.getServer();
			final String playerName = player.getName();
			final String lang = VersionUtil.getLocale(player);
			for (final Module module : moduleManager.getModules()) {
					if (!player.hasPermission("chatsentinel.bypass." + module.getName())
						&& (module instanceof CooldownModule || module instanceof SyntaxModule || isNormalCommand)
						&& module.meetsCondition(chatPlayer, modifiedMessage)) {
					final int warns = chatPlayer.addWarn(module), maxWarns = module.getMaxWarns();
					final String[][] placeholders = {
							{ "%player%", "%message%", "%warns%", "%maxwarns%", "%cooldown%" },
							{ playerName, originalMessage, String.valueOf(warns), String.valueOf(module.getMaxWarns()),
									String.valueOf(0) } };


					if (module instanceof BlacklistModule) {
						final BlacklistModule blacklistModule = (BlacklistModule) module;
						if (blacklistModule.isHideWords()) {
							event.setMessage(blacklistModule.getPattern().matcher(originalMessage).replaceAll(blacklistModule.getRandomReplaceWord()));

						} else
							event.setCancelled(true);

					} else if (module instanceof CapsModule) {
						final CapsModule capsModule = (CapsModule) module;

						if (capsModule.isReplace())
							event.setMessage(originalMessage.toLowerCase());
						else
							event.setCancelled(true);
					} else if (module instanceof CooldownModule) {
						placeholders[1][4] = String
								.valueOf(((CooldownModule) module).getRemainingTime(chatPlayer, modifiedMessage));

						event.setCancelled(true);
					} else if (module instanceof FloodModule) {
						final FloodModule floodModule = (FloodModule) module;

						if (floodModule.isReplace()) {
							final String replacedString = floodModule.replace(originalMessage);

							if (!replacedString.isEmpty())
								event.setMessage(replacedString);
							else
								event.setCancelled(true);
						} else
							event.setCancelled(true);
					} else if (module instanceof PurpleModule){
						continue;
					}else
						event.setCancelled(true);

					final String notificationMessage = module.getWarnNotification(placeholders);
					final String warnMessage = messagesModule.getWarnMessage(placeholders, lang, module.getName());

					if (warnMessage != null && !warnMessage.isEmpty())
						player.sendMessage(warnMessage);

					if (notificationMessage != null && !notificationMessage.isEmpty()) {
						for (final Player player1 : server.getOnlinePlayers()) {
							if (player1.hasPermission("chatsentinel.notify"))
								player1.sendMessage(notificationMessage);
						}

						server.getConsoleSender().sendMessage(notificationMessage);
					}

					if (warns >= maxWarns && maxWarns > 0) {
						server.getScheduler().runTask(plugin, () -> {
							for (final String command : module.getCommands(placeholders)) {
								server.dispatchCommand(server.getConsoleSender(), command);
							}
						});

						chatPlayer.clearWarns();

						if (event.isCancelled()) {
							break;
						}
					}
				}
			}

			if (!event.isCancelled()) {
				chatPlayer.addLastMessage(modifiedMessage, System.currentTimeMillis());
			}
		}
	}
}
