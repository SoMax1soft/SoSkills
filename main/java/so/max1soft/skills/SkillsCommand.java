package so.max1soft.skills;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillsCommand implements CommandExecutor {

    private final Main plugin;

    public SkillsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /soskills <activate|disable> [skill]");
            return true;
        }

        String action = args[0];
        SkillsListener listener = plugin.getSkillsListener();

        if (action.equalsIgnoreCase("activate")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /soskills activate <skill>");
                return true;
            }
            String skillName = args[1];
            if (player.hasPermission("skills.use." + skillName.toLowerCase())) {
                listener.activateSkill(player, skillName);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this skill.");
            }
        } else if (action.equalsIgnoreCase("disable")) {
            listener.disableSkills(player);
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /soskills <activate|disable> [skill]");
        }

        return true;
    }
}
