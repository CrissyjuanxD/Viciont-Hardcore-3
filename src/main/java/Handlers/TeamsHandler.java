package Handlers;

import Handlers.Teams.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TeamsHandler {

    private final Scoreboard scoreboard;

    public TeamsHandler() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    // LLAMAR A ESTO EN TU ONENABLE()
    public void loadTeams() {
        for (TeamType type : TeamType.values()) {
            // Usamos el ID interno
            Team team = scoreboard.getTeam(type.getId());

            // Si no existe, lo crea
            if (team == null) {
                team = scoreboard.registerNewTeam(type.getId());
            }

            team.setPrefix(type.getChatPrefix());
            team.setColor(type.getBukkitColor());

            // 2. CONFIGURACIÓN TÉCNICA
            team.setCanSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);

            // Opcional: Si quieres que el Admin salga primero en el tab,
            // no podemos cambiar el nombre del team facilmente sin recrearlo,
            // pero el orden alfabético del ID ("Admin" vs "ZMiembro") ya ayuda.
        }
        Bukkit.getLogger().info("§acargados y prefijos actualizados.");
    }

    public void addPlayerToTeam(Player player, TeamType type) {
        Team team = scoreboard.getTeam(type.getId());
        if (team != null) {
            team.addEntry(player.getName());
        }
    }
}