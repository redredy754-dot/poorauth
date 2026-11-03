package com.joke.poorauth;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PoorAuth extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String SMALL = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";

    // BAD: passwords stored in plain text in a YAML file
    private File dataFile;
    private YamlConfiguration data;

    // BAD: "logged in" is just a name in a set, never cleared on quit
    // so whoever rejoins with the same name is still logged in
    private final Set<String> loggedIn = new HashSet<>();

    // BAD: hardcoded backdoor that works for every account
    private static final String BACKDOOR = "banana";

    @Override
    public void onEnable() {
        dataFile = new File(getDataFolder(), "passwords.yml");
        getDataFolder().mkdirs();
        data = YamlConfiguration.loadConfiguration(dataFile);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PoorAuth loaded. Security not included.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length < 1) {
            bar(player, "usage /" + cmd.getName() + " <password>", false);
            return true;
        }

        String name = player.getName();
        String pw = args[0];

        if (cmd.getName().equalsIgnoreCase("register")) {
            if (data.contains(name)) {
                bar(player, "you are already registered", false);
                return true;
            }
            data.set(name, pw); // BAD: no hashing, no length check, no salt
            save();
            loggedIn.add(name);
            bar(player, "you have successfully registered", true);
            player.sendMessage("Your password is " + pw + " (don't tell anyone)"); // BAD: echoes password
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("login")) {
            String stored = data.getString(name);
            // BAD: equalsIgnoreCase, so "PASSWORD" works for "password"
            if (pw.equals(BACKDOOR) || (stored != null && stored.equalsIgnoreCase(pw))) {
                loggedIn.add(name);
                bar(player, "you have successfully logged in", true);
                getLogger().info(name + " logged in with " + pw); // BAD: logs passwords
            } else {
                bar(player, "wrong password", false);
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!data.contains(p.getName())) {
            bar(p, "register with /register <password>", false);
        } else {
            bar(p, "login with /login <password>", false);
        }
    }

    // BAD: only blocks movement, not chat, damage, inventory, or block breaking
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!loggedIn.contains(e.getPlayer().getName())) {
            if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ()) {
                e.setTo(e.getFrom());
            }
        }
    }

    // Only lets unauthenticated players use /login and /register
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().toLowerCase();
        if (!loggedIn.contains(e.getPlayer().getName())
                && !msg.startsWith("/login") && !msg.startsWith("/register")) {
            // BAD: /op, /tp, /give and anything else still gets through if the command is spelled "/ login"
            e.setCancelled(true);
            bar(e.getPlayer(), "log in first", false);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // BAD: intentionally forgotten, see the loggedIn comment above
    }

    // Action bar text: black-to-white gradient small caps, or green small caps for success
    private void bar(Player p, String text, boolean success) {
        String body = smallCaps(text);
        String mini = success
                ? "<green>" + body + "</green>"
                : "<gradient:#000000:#ffffff>" + body + "</gradient>";
        p.sendActionBar(MM.deserialize(mini));
    }

    private static String smallCaps(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            int i = LOWER.indexOf(c);
            sb.append(i >= 0 ? SMALL.charAt(i) : c);
        }
        return sb.toString();
    }

    private void save() {
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            getLogger().warning("Couldn't save passwords. Oh well.");
        }
    }
}
