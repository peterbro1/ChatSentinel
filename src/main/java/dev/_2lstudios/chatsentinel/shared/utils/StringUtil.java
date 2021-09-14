package dev._2lstudios.chatsentinel.shared.utils;

import java.text.Normalizer;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class StringUtil {

    public static String removeAccents(String message) {
		/*
		 * Removes accents Credit: https://stackoverflow.com/users/636009/david-conrad
		 */
		final char[] out = new char[message.length()];

		message = Normalizer.normalize(message, Normalizer.Form.NFD);

		for (int j = 0, i = 0, n = message.length(); i < n; ++i) {
			final char c = message.charAt(i);

			if (c <= '\u007F') {
				out[j++] = c;
			}
		}

		return new String(out).replace("(punto)", ".").replace("(dot)", ".").trim();
	}
	public static HashMap<Pattern,String> getPairs(String[] messages){
    	HashMap<Pattern,String> _map = new HashMap<>();
    	for (String message : messages) {
			String[] pairs = message.split("===");
			try{
				_map.put(Pattern.compile(pairs[0]),pairs[1]);
			}catch (Exception e){
				System.out.println("[ChatSentinel ERROR] " + e.getMessage());
				continue;
			}
		}
    	return _map;

	}

}
