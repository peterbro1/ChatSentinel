package dev._2lstudios.chatsentinel.shared.modules;

import java.util.Collection;

public class GeneralModule {
	private Collection<String> commands;
	private boolean msg_enabled;
	public void loadData(final Collection<String> commands, boolean msgs) {
		this.commands = commands;
		msg_enabled = msgs;
	}

	public boolean isCommand(final String message) {
		for (final String command : commands) {
			if (message.toLowerCase().startsWith(command + ' '))
				return true;
		}

		return false;
	}

	public boolean msgEnabled(){
		return this.msg_enabled;
	}
}
