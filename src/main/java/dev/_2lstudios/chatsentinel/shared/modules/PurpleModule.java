package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.interfaces.Module;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class PurpleModule implements Module {

    private HashMap<Pattern,String> entries;
    private boolean enabled;


    final public void loadData(final boolean enabled, HashMap<Pattern,String> entries) {
        this.enabled = enabled;
        this.entries = new HashMap<>();
        this.entries = entries;
    }

    @Override
    public boolean meetsCondition(ChatPlayer chatPlayer, String message) {
        for (Pattern p : entries.keySet())
            if (p.matcher(message).find())
                return true;

        return false;
    }

    @Override
    public int getMaxWarns() {
        return 0;
    }

    public String getReturnString(String message){
        for (Pattern p : entries.keySet())
            if (p.matcher(message).find())
                return ChatColor.translateAlternateColorCodes('&',entries.get(p));
        return "";
    }

    @Override
    public String[] getCommands(String[][] placeholders) {
        return new String[0];
    }

    @Override
    public String getName() {
        return "Purple";
    }

    @Override
    public String getWarnNotification(String[][] placeholders) {
        return null;
    }
}
