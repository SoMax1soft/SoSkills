package so.max1soft.skills;

import com.Zrips.CMI.CMI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private SkillsListener skillsListener;
    public static Main inst;
    private CMI cmi;


    @Override
    public void onEnable() {
            inst = this;
        saveDefaultConfig();
        getLogger().info("");
        getLogger().info("§fПлагин: §aЗапущен");
        getLogger().info("§fСоздатель: §b@max1soft");
        getLogger().info("§fВерсия: §c1.5 (CMI)");
        getLogger().info("");
        getLogger().info("§fИнформация: §dMax1soft.pw");
        getLogger().info("");
        skillsListener = new SkillsListener(this);

        getServer().getPluginManager().registerEvents(skillsListener, this);
        this.cmi = (CMI) Bukkit.getPluginManager().getPlugin("CMI");
        this.getCommand("soskills").setExecutor(new SkillsCommand(this));
    }
    public CMI getCMI() {
        return this.cmi;
    }
    public SkillsListener getSkillsListener() {
        return skillsListener;
    }
}
