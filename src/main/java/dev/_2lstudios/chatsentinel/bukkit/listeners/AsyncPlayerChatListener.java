package dev._2lstudios.chatsentinel.bukkit.listeners;

import java.awt.*;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

import dev._2lstudios.chatsentinel.bukkit.ChatSentinel;
import dev._2lstudios.chatsentinel.shared.modules.*;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.BaseComponentSerializer;
import net.md_5.bungee.chat.TextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import dev._2lstudios.chatsentinel.bukkit.modules.ModuleManager;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayerManager;
import dev._2lstudios.chatsentinel.shared.interfaces.Module;
import dev._2lstudios.chatsentinel.shared.utils.StringUtil;
import dev._2lstudios.chatsentinel.shared.utils.VersionUtil;

public class AsyncPlayerChatListener implements Listener {
	private final Plugin plugin;
	private final ModuleManager moduleManager;
	private final ChatPlayerManager chatPlayerManager;

	public AsyncPlayerChatListener(final Plugin plugin, final ModuleManager moduleManager,
			final ChatPlayerManager chatPlayerManager) {
		this.plugin = plugin;
		this.moduleManager = moduleManager;
		this.chatPlayerManager = chatPlayerManager;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {
		final Player player = event.getPlayer();

		if (player.hasPermission("chatsentinel.bypass"))
			return;


			final UUID uuid = player.getUniqueId();
			final ChatPlayer chatPlayer = chatPlayerManager.getPlayer(uuid);
			final String message = event.getMessage().trim();
			final String modifiedMessage;
			final MessagesModule messagesModule = moduleManager.getMessagesModule();
			final WhitelistModule whitelistModule = moduleManager.getWhitelistModule();
			final Server server = plugin.getServer();
			final String playerName = player.getName();
			final String lang = VersionUtil.getLocale(player);

			if (whitelistModule.isEnabled()) {
				final Pattern whitelistPattern = whitelistModule.getPattern();

				modifiedMessage = whitelistPattern.matcher(StringUtil.removeAccents(message))
						.replaceAll("").trim();
			} else {
				modifiedMessage = StringUtil.removeAccents(message);
			}

			for (final Module module : moduleManager.getModules()) {
				if (!player.hasPermission("chatsentinel.bypass." + module.getName())
						&& module.meetsCondition(chatPlayer, modifiedMessage)) {
					final Collection<Player> recipients = event.getRecipients();
					final int warns = chatPlayer.addWarn(module), maxWarns = module.getMaxWarns();
					final String[][] placeholders = {
							{ "%player%", "%message%", "%warns%", "%maxwarns%", "%cooldown%" }, { playerName, message,
									String.valueOf(warns), String.valueOf(module.getMaxWarns()), String.valueOf(0) } };




					if (module instanceof BlacklistModule) {
						final BlacklistModule blacklistModule = (BlacklistModule) module;
						chatPlayer.addLastMessage(modifiedMessage,System.currentTimeMillis());
						if (blacklistModule.isFakeMessage()) {
							recipients.removeIf(player1 -> player1 != player);
						} else if (blacklistModule.isHideWords()) {
							event.setMessage(blacklistModule.getPattern().matcher(modifiedMessage).replaceAll(blacklistModule.getRandomReplaceWord()));
						} else {
							event.setCancelled(true);
						}
					} else if (module instanceof PurpleModule){
						final PurpleModule pmod = (PurpleModule) module;
						Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin,
								() -> {player.sendMessage(pmod.getReturnString(message));},
								1);
						break;
					}else if (module instanceof CapsModule) {
						final CapsModule capsModule = (CapsModule) module;

						if (capsModule.isReplace()) {
							event.setMessage(message.toLowerCase());
						} else {
							event.setCancelled(true);
						}
					} else if (module instanceof CooldownModule) {
						placeholders[1][4] = String
								.valueOf(((CooldownModule) module).getRemainingTime(chatPlayer, message));

						event.setCancelled(true);
					} else if (module instanceof FloodModule) {
						final FloodModule floodModule = (FloodModule) module;

						if (floodModule.isReplace()) {
							final String replacedString = floodModule.replace(message);

							if (!replacedString.isEmpty()) {
								event.setMessage(replacedString);
							} else {
								event.setCancelled(true);
							}
						} else {
							event.setCancelled(true);
						}
					} else {
						event.setCancelled(true);
					}

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
