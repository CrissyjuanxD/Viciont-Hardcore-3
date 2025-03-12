    package Dificultades.Features;

    import org.bukkit.Bukkit;
    import org.bukkit.GameMode;
    import org.bukkit.Location;
    import org.bukkit.World;
    import org.bukkit.entity.*;
    import org.bukkit.event.EventHandler;
    import org.bukkit.event.HandlerList;
    import org.bukkit.event.Listener;
    import org.bukkit.event.entity.EntityDamageByEntityEvent;
    import org.bukkit.plugin.Plugin;
    import org.bukkit.scheduler.BukkitRunnable;
    import org.bukkit.util.Vector;

    public class PassiveMobAggression implements Listener {

        private final Plugin plugin;
        private boolean isApplied = false;

        public PassiveMobAggression(Plugin plugin) {
            this.plugin = plugin;
        }

        public void apply(){
            if (!isApplied) {
                Bukkit.getPluginManager().registerEvents(this, plugin);
                startPassiveMobAggression();
                isApplied = true;
            }
        }

        public void revert(){
            if (isApplied) {
                isApplied = false;
                HandlerList.unregisterAll(this);
            }
        }

        public void startPassiveMobAggression() {
            // Ejecutar la tarea cada 2 ticks para un movimiento más fluido
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!isApplied) {
                        cancel();
                        return;
                    }

                    for (World world : Bukkit.getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            // Verificar si es un mob pacífico (Creature pero no Monster)
                            if (entity instanceof Creature && !(entity instanceof Monster)) {

                                if (entity.getType() == EntityType.BEE || entity.getType() == EntityType.DOLPHIN) {
                                    continue; // Saltar esta entidad
                                }

                                Creature creature = (Creature) entity;

                                // Buscar el jugador más cercano dentro de un radio de 10 bloques
                                Player nearest = null;
                                double nearestDistance = 10; // Radio de 10 bloques
                                for (Player player : world.getPlayers()) {
                                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                                        continue;
                                    }
                                    double distance = creature.getLocation().distance(player.getLocation());
                                    if (distance < nearestDistance) {
                                        nearest = player;
                                        nearestDistance = distance;
                                    }
                                }

                                // Si hay un jugador cercano, seguir y atacar
                                if (nearest != null) {
                                    followAndAttackPlayer(creature, nearest);
                                } else {
                                    // Si no hay jugadores cercanos, detener el seguimiento
                                    creature.setTarget(null);
                                }
                            }
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 2L); // Ejecutar cada 2 ticks
        }

        private void followAndAttackPlayer(Creature creature, Player player) {
            // Asignar el jugador como objetivo (esto activa la IA de Pathfinder)
            creature.setTarget(player);

            // Obtener ubicaciones actuales
            Location mobLoc = creature.getLocation();
            Location playerLoc = player.getLocation();

            // Calcular el vector dirección horizontal (ignorando la diferencia vertical)
            Vector direction = playerLoc.toVector().subtract(mobLoc.toVector());
            direction.setY(0);

            if (direction.lengthSquared() == 0) {
                return;
            }

            direction.normalize();

            // Suavizar la aceleración: interpolar entre la velocidad actual y la dirección deseada
            double acceleration = 0.1; // Ajusta este valor para definir la rapidez con la que cambia la velocidad
            Vector currentVel = creature.getVelocity();
            Vector newVel = currentVel.multiply(0.9).add(direction.multiply(acceleration));
            creature.setVelocity(newVel);

            // (Opcional) Actualizar la orientación del mob: calculamos el yaw deseado
            double yaw = Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
            // Si la diferencia es significativa, podemos actualizar la orientación
            if (Math.abs(mobLoc.getYaw() - (float) yaw) > 5) {
                Location newLoc = mobLoc.clone();
                newLoc.setYaw((float) yaw);
                // Hacemos un teleport muy ligero: solo para actualizar la dirección sin mover la posición
                creature.teleport(newLoc);
            }

            // Si el mob está muy cerca del jugador, inflige daño
            if (mobLoc.distance(playerLoc) <= 1.5) {
                player.damage(8.0, creature); // 8.0 equivale a 4 corazones de daño
            }
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            if (event.getDamager() instanceof Creature && !(event.getDamager() instanceof Monster)) {
                // Excluir abejas y delfines
                Entity damager = event.getDamager();
                if (damager.getType() == EntityType.BEE || damager.getType() == EntityType.DOLPHIN) {
                    return; // No aplicar daño
                }
                event.setDamage(8.0);
            }
        }
    }