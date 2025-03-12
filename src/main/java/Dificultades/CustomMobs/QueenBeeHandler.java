package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class QueenBeeHandler implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Boolean> canBeShot = new HashMap<>();
    private final Map<UUID, Boolean> isRegenerating = new HashMap<>();
    private final Map<UUID, Boolean> isAttacking = new HashMap<>();
    private final Random random = new Random();
    private final Map<UUID, String> deathCauseMap = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeBehaviors = new HashMap<>();
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private final NamespacedKey queenbeekey;

    public QueenBeeHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.queenbeekey = new NamespacedKey(plugin, "queenbee");
    }

    // Spawnea la Abeja Reina
    public Bee spawnQueenBee(Location location) {
        Bee bee = (Bee) Objects.requireNonNull(location.getWorld()).spawnEntity(location, EntityType.BEE);
        applyQueenBeeAttributes(bee);
        return bee;
    }

    public void transformToQueenBee(Bee bee){
        applyQueenBeeAttributes(bee);
    }

    private void applyQueenBeeAttributes(Bee bee) {
        bee.setCustomName(ChatColor.GOLD + "Abeja Reina");
        bee.setCustomNameVisible(true);
        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(500.0);
        bee.setHealth(500.0);
        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.35);
        bee.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        bee.getPersistentDataContainer().set(queenbeekey, PersistentDataType.BYTE, (byte) 1);
        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(3);
        bee.setSilent(true);
        bee.setRemoveWhenFarAway(false);
        bee.setAI(true);

        // Abeja siempre enojada
        bee.setAnger(999999); // Duración exagerada para mantenerse enojada
        bee.setCannotEnterHiveTicks(Integer.MAX_VALUE); // No puede entrar en colmenas
        bee.setHasStung(false); // Asegura que siempre tenga aguijón
        bee.setTarget(getRandomPlayer(bee)); // Ataca a un jugador inmediatamente

        // Crear BossBar
        BossBar bossBar = Bukkit.createBossBar(ChatColor.YELLOW + "Abeja Reina", BarColor.YELLOW, BarStyle.SOLID);
        bossBar.setVisible(true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        bossBars.put(bee.getUniqueId(), bossBar);
        canBeShot.put(bee.getUniqueId(), false);
        isRegenerating.put(bee.getUniqueId(), false);
        isAttacking.put(bee.getUniqueId(), false);

        startBossBehavior(bee);
    }

    private void updateBossBar(Bee bee) {
        UUID beeId = bee.getUniqueId();
        BossBar bossBar = bossBars.get(beeId);

        if (bossBar != null && !bee.isDead()) {
            double healthPercentage = Math.max(0.0, bee.getHealth() / 500.0);
            bossBar.setProgress(healthPercentage);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Bee bee && isQueenBee(bee)) {
            Bukkit.getScheduler().runTask(plugin, () -> updateBossBar(bee));
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Bee bee && isQueenBee(bee)) {
            Bukkit.getScheduler().runTask(plugin, () -> updateBossBar(bee));
        }
    }


    @EventHandler
    public void onTargetChange(EntityTargetEvent event) {
        if (event.getEntity() instanceof Bee bee) {
            if (isQueenBee(bee)) {
                bee.setAnger(999999);
            }
        }
    }

    @EventHandler
    public void onBeeAttack(EntityDamageByEntityEvent event) {
        // Verifica si el atacante es una abeja reina personalizada
        if (event.getDamager() instanceof Bee bee && isQueenBee(bee)) {
            event.setCancelled(true);
            // Actualiza el tiempo de actividad
            lastAttackTime.put(bee.getUniqueId(), System.currentTimeMillis());

            // Aplica daño manualmente
            if (event.getEntity() instanceof Player player) {
                player.damage(4); // Daño personalizado
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                bee.playEffect(EntityEffect.ENTITY_POOF);
                bee.getWorld().spawnParticle(Particle.SMOKE, bee.getLocation(), 10, 0.5, 1.0, 0.5, 0.1);
                bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_STING, 1f, 0.1f);
                deathCauseMap.put(player.getUniqueId(), "Abeja Reina");

                // Empuja al jugador ligeramente hacia atrás
                Vector knockback = player.getLocation().toVector()
                        .subtract(bee.getLocation().toVector())
                        .normalize().multiply(0.4);

                if (isVectorFinite(knockback)) {
                    player.setVelocity(knockback);
                } else {
                    player.setVelocity(new Vector(0, 0, 0)); // No aplicar empuje si el vector no es válido
                }

                // Reestablece el estado de ataque para continuar atacando
                isAttacking.put(bee.getUniqueId(), false);
            }

            Vector direction = bee.getLocation().toVector()
                    .subtract(event.getEntity().getLocation().toVector());

            if (direction.length() == 0) {
                direction = new Vector(0, 0, 0); // Si la longitud es cero, no generamos un vector inválido
            } else {
                direction = direction.normalize().multiply(1.2); // Normalizamos y escalamos la dirección
            }

            if (isVectorFinite(direction)) {
                bee.setVelocity(direction); // Solo aplicar la velocidad si el vector es válido
            } else {
                bee.setVelocity(new Vector(0, 0, 0));// No mover si el vector no es válido
            }

            // Tarea repetitiva para cambiar de objetivo si hay alguien más cerca
            new BukkitRunnable() {
                int ticks = 0; // Contador para los 5 segundos

                @Override
                public void run() {
                    ticks += 10; // Incrementa el tiempo en 10 ticks (0.5 segundos)

                    // Busca al jugador más cercano
                    Player closestPlayer = getClosestPlayer(event.getEntity(), 20);

                    // Si hay un jugador más cercano, cambia el objetivo
                    if (closestPlayer != null) {
                        bee.setTarget(closestPlayer);
                    }

                    // Después de 5 segundos (100 ticks), detiene la tarea
                    if (ticks >= 100) {
                        this.cancel(); // Detiene la repetición
                    }
                }
            }.runTaskTimer(plugin, 0L, 10L); // Se ejecuta cada 10 ticks (0.5 segundos)
        }
    }

    // verificar si el vector es finito
    private boolean isVectorFinite(Vector vector) {
        return !Double.isNaN(vector.getX()) && !Double.isNaN(vector.getY()) && !Double.isNaN(vector.getZ())
                && !Double.isInfinite(vector.getX()) && !Double.isInfinite(vector.getY()) && !Double.isInfinite(vector.getZ());
    }
    // Verifica si una ubicación es finita
    private boolean isLocationFinite(Location location) {
        return Double.isFinite(location.getX()) && Double.isFinite(location.getY()) && Double.isFinite(location.getZ());
    }
    private boolean isQueenBee(Bee bee) {
        return bee.getCustomName() != null && bee.getCustomName().contains("Abeja Reina");
    }

    private void startBossBehavior(Bee bee) {
        if (activeBehaviors.containsKey(bee.getUniqueId())) {
            return; // Evita tareas duplicadas si ya hay una activa
        }

        BukkitRunnable behaviorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (bee.isDead()) {
                    activeBehaviors.remove(bee.getUniqueId());
                    cancel();
                    return;
                }

                // Si ya está atacando, no permite nuevas acciones
                if (isAttacking.getOrDefault(bee.getUniqueId(), false)) {
                    return;
                }

                // Verifica la salud actual de la abeja (ataque de recuperacion)
                double health = bee.getHealth();
                if (health < 250.0) {
                    double chance = random.nextDouble();
                    if (chance <= 0.24) {
                        regenerateBee(bee);
                        return; // Sal de la ejecución actual para evitar otras acciones
                    }
                }

                // Selecciona y ejecuta una acción aleatoria (ataques principales)
                int action = random.nextInt(6);
                switch (action) {
                    case 0 -> launchSpikes(bee);
                    case 1 -> launchExplosiveAttack(bee);
                    case 2 -> poisonCircle(bee);
                    case 3 -> summonAngryBees(bee);
                    case 4 -> meleeAttack(bee);
                    case 5 -> executeFlightAttack(bee);
                }
            }
        };

        behaviorTask.runTaskTimer(plugin, 0L, 50L);
        activeBehaviors.put(bee.getUniqueId(), behaviorTask); // Añade la tarea activa
    }

    private void launchSpikes(Bee bee) {
        isAttacking.put(bee.getUniqueId(), true);
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 5.0f, 0.1f);

        Location beeLocation = bee.getLocation();
        List<Vector> directions = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(((double) 360 / 8) * i);
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            directions.add(new Vector(x, -0.3, z).normalize());
        }

        for (Vector direction : directions) {
            BlockDisplay spike = bee.getWorld().spawn(beeLocation, BlockDisplay.class);
            spike.setBlock(Bukkit.createBlockData(Material.POINTED_DRIPSTONE));
            spike.setGravity(false);

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks++ > 40 || spike.isDead()) {
                        spike.remove();
                        cancel();
                        return;
                    }

                    Location newLocation = spike.getLocation().add(direction.clone().multiply(0.3));
                    if (!isLocationFinite(newLocation) || newLocation.getBlock().getType().isSolid()) {
                        // Si choca, intenta continuar o reiniciar
                        spike.remove();
                        cancel();
                        return;
                    }

                    spike.teleport(newLocation);

                    for (Entity entity : spike.getWorld().getNearbyEntities(spike.getLocation(), 1, 1, 1)) {
                        if (entity instanceof Player player) {
                            player.damage(8);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 500, 1));
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.1f);
                            deathCauseMap.put(player.getUniqueId(), "Aguijón de Abeja Reina");
                            spike.remove();
                            cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        // Restablecer el estado de ataque después de 3 segundos
        new BukkitRunnable() {
            @Override
            public void run() {
                isAttacking.put(bee.getUniqueId(), false);
            }
        }.runTaskLater(plugin, 60L);
    }


    private void launchExplosiveAttack(Bee bee) {
        isAttacking.put(bee.getUniqueId(), true);
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 5.0f, 0.1f);

        Location beeLocation = bee.getLocation();
        List<Vector> directions = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(((double) 360 / 8) * i);
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            directions.add(new Vector(x, -0.3, z).normalize());
        }

        for (Vector direction : directions) {
            BlockDisplay blockDisplay = bee.getWorld().spawn(beeLocation, BlockDisplay.class);
            blockDisplay.setBlock(Bukkit.createBlockData(Material.POINTED_DRIPSTONE));
            blockDisplay.setGravity(false);
            blockDisplay.setGlowing(true);
            blockDisplay.setGlowColorOverride(Color.RED);

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks++ > 40 || blockDisplay.isDead()) {
                        blockDisplay.remove();
                        cancel();
                        return;
                    }

                    Location newLocation = blockDisplay.getLocation().add(direction.clone().multiply(0.3));
                    if (!isLocationFinite(newLocation) || newLocation.getBlock().getType().isSolid()) {
                        explode(blockDisplay);
                        blockDisplay.remove();
                        cancel();
                        return;
                    }

                    for (Entity entity : blockDisplay.getWorld().getNearbyEntities(blockDisplay.getLocation(), 1, 1, 1)) {
                        if (entity instanceof Player player) {
                            explode(blockDisplay);
                            player.damage(3);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 2));
                            deathCauseMap.put(player.getUniqueId(), "Aguijón Explosivos de Abeja Reina");
                            blockDisplay.remove();
                            cancel();
                            return;
                        }
                    }

                    blockDisplay.teleport(newLocation);
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isAttacking.put(bee.getUniqueId(), false);
            }
        }.runTaskLater(plugin, 60L);
    }


    private void explode(Entity explosive) {
        explosive.getWorld().spawnParticle(Particle.EXPLOSION, explosive.getLocation(), 1); // Partícula de explosión
        explosive.getWorld().playSound(explosive.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 1.2f); // Sonido de explosión
        //efecto de pocion por si te da la explocion
        for (Entity entity : explosive.getWorld().getNearbyEntities(explosive.getLocation(), 5, 5, 5)) {
            if (entity instanceof Player player) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 150, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 2));
            }
        }

        // Daño en área (si hay alguna entidad dentro de un radio de 5 bloques)
        for (Entity nearbyEntity : explosive.getWorld().getNearbyEntities(explosive.getLocation(), 5, 5, 5)) {
            if (nearbyEntity instanceof LivingEntity livingEntity) {
                livingEntity.damage(6); // Daño fijo de 3 corazones
            }
        }
    }

    private void executeFlightAttack(Bee bee) {
        isAttacking.put(bee.getUniqueId(), true);

        // **Fase 1: Subir**
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks < 20) { // Sube durante 1 segundo (20 ticks)
                    bee.setVelocity(new Vector(0, 0.5, 0)); // Velocidad de subida
                } else {
                    bee.setVelocity(new Vector(0, 0, 0)); // Detener vuelo
                    startDescent(bee); // Pasar a la siguiente fase
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Se ejecuta cada tick

        // Efectos visuales y de sonido para animar el vuelo
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.0f, 0.8f);
        bee.getWorld().spawnParticle(Particle.CLOUD, bee.getLocation(), 10, 0.5, 1.0, 0.5, 0.1);
    }

    // **Fase 2: Bajar**
    private void startDescent(Bee bee) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks < 20) { // Baja durante 1 segundo
                    bee.setVelocity(new Vector(0, -0.5, 0)); // Velocidad de bajada
                } else {
                    bee.setVelocity(new Vector(0, 0, 0)); // Detener bajada
                    performGroundAttack(bee); // Iniciar el ataque en el suelo
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 1L); // Espera 20 ticks antes de ejecutar
    }

    // **Fase 3: Ataque en el suelo**
    private void performGroundAttack(Bee bee) {
        // **Ataque 1: Pinchos explosivos**
        launchSpikes(bee);

        new BukkitRunnable() {
            @Override
            public void run() {
                // **Ataque 2: Explosión**
                launchExplosiveAttack(bee);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // **Ataque 3: Círculo de veneno**
                        poisonCircle(bee);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                // **Ataque 4: Invocar abejas**
                                summonAngryBees(bee);

                                // **Fase 4: Subir después del ataque**
                                startFlightFinish(bee);
                            }
                        }.runTaskLater(plugin, 20L); // Espera 1 segundo entre ataques
                    }
                }.runTaskLater(plugin, 20L); // Espera 1 segundo
            }
        }.runTaskLater(plugin, 20L); // Espera 1 segundo
    }

    // **Fase 4: Subida final**
    private void startFlightFinish(Bee bee) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks < 20) { // Sube durante 1 segundo
                    bee.setVelocity(new Vector(0, 0.5, 0)); // Velocidad de subida
                } else {
                    bee.setVelocity(new Vector(0, 0, 0)); // Detener vuelo
                    finishFlightAttack(bee); // Terminar ataque
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 1L); // Retraso después del ataque
    }

    // **Fase 5: Finalización del ataque**
    private void finishFlightAttack(Bee bee) {
        isAttacking.put(bee.getUniqueId(), false); // Restablecer el estado

        // Efectos para finalizar
        bee.getWorld().spawnParticle(Particle.CLOUD, bee.getLocation(), 10, 0.5, 1.0, 0.5, 0.1);
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1f, 0.5f);
    }


    private void poisonCircle(Bee bee) {
        isAttacking.put(bee.getUniqueId(), true);
        Location center = bee.getLocation();

        // Precalcula las posiciones en círculo
        List<Vector> offsets = new ArrayList<>();
        for (double angle = 0; angle < 360; angle += 10) {
            double radians = Math.toRadians(angle);
            offsets.add(new Vector(Math.cos(radians), 0, Math.sin(radians)));
        }

        new BukkitRunnable() {
            double radius = 0.5;

            @Override
            public void run() {
                if (radius > 15) { // Radio máximo
                    isAttacking.put(bee.getUniqueId(), false);
                    cancel();
                    return;
                }

                bee.getWorld().playSound(center, Sound.ENTITY_BEE_LOOP, 5f, 0.1f);

                for (Vector offset : offsets) {
                    Location particleLoc = center.clone().add(offset.clone().multiply(radius));
                    bee.getWorld().spawnParticle(Particle.WITCH, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);

                    // Usamos getNearbyEntities para obtener las entidades cercanas
                    for (Entity entity : bee.getWorld().getNearbyEntities(particleLoc, 1, 1, 1)) {
                        if (entity instanceof Player player) { // Verifica si la entidad es un jugador
                            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 4));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1));
                        }
                    }
                }

                radius += 0.5; // Expande el círculo
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }


    // 3. Invoca 5 abejas enojadas
    private void summonAngryBees(Bee bee) {
        isAttacking.put(bee.getUniqueId(), true);
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 5.0f, 0.1f);

        for (int i = 0; i < 4; i++) {
            Bee angryBee = (Bee) bee.getWorld().spawnEntity(bee.getLocation(), EntityType.BEE);
            angryBee.setAnger(2200); // 60 segundos enojada

            // Evitar que entre en el Bee Nest
            angryBee.setCannotEnterHiveTicks(Integer.MAX_VALUE);

            // Asigna un nombre personalizado con color dorado usando concatenación
            angryBee.setCustomName(ChatColor.GOLD + "Abeja Guardiana " + (i + 1));
            angryBee.setCustomNameVisible(true); // Opcional, para mostrar el nombre

            // Selecciona un jugador aleatorio como objetivo
            Player target = getRandomPlayer(bee);
            if (target != null) {
                angryBee.setTarget(target);
            }

            // Matar la abeja después de 1 minuto (1200 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    angryBee.remove();
                }
            }.runTaskLater(plugin, 1200L); // 1 minuto en ticks (20 ticks = 1 segundo)
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isAttacking.put(bee.getUniqueId(), false);
            }
        }.runTaskLater(plugin, 100L);
    }

    // 4. Ataque cuerpo a cuerpo
    private void meleeAttack(Bee bee) {
        if (isAttacking.getOrDefault(bee.getUniqueId(), false)) return; // Ya está atacando
        isAttacking.put(bee.getUniqueId(), true);

        Player target = getRandomPlayer(bee);
        if (target != null) {
            bee.setTarget(target);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (bee.getLocation().distance(target.getLocation()) <= 1.5) {
                        target.damage(4); // 2 corazones de daño
                    }

                    bee.setTarget(null);
                    isAttacking.put(bee.getUniqueId(), false);
                }
            }.runTaskLater(plugin, 40L); // 2 segundos de ataque
        }
    }


    // 5. Regeneración sobre el bee nest
    private void regenerateBee(Bee bee) {
        if (isAttacking.getOrDefault(bee.getUniqueId(), false)) return; // Ya está atacando
        // Verificar si la abeja ya está regenerándose
        if (isRegenerating.getOrDefault(bee.getUniqueId(), false)) {
            return; // Evitar regeneraciones duplicadas
        }

        // Marcar como regenerando
        isRegenerating.put(bee.getUniqueId(), true);

        // Crear un nuevo runnable para manejar partículas y sonido
        BukkitRunnable particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bee.isValid() || bee.isDead()) {
                    this.cancel(); // Cancelar si la abeja ya no es válida
                    return;
                }

                // Generar partículas alrededor de la abeja
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double x = Math.cos(angle) * 0.5;
                    double z = Math.sin(angle) * 0.5;
                    Location particleLoc = bee.getLocation().clone().add(x, 0, z);
                    bee.getWorld().spawnParticle(Particle.GLOW, particleLoc, 0, 0, 0, 0, 1);
                }

                // Sonido de regeneración
                bee.getWorld().playSound(bee.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 5.0f, 2f);
            }
        };

        // Ejecutar partículas cada 10 ticks (0.5 segundos)
        particleTask.runTaskTimer(plugin, 0L, 10L);

        // Regenerar la salud después de 5 segundos
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!bee.isValid() || bee.isDead()) {
                    particleTask.cancel(); // Cancelar las partículas si la abeja ya no es válida
                    isRegenerating.put(bee.getUniqueId(), false);
                    return;
                }

                double maxHealth = Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
                double newHealth = Math.min(bee.getHealth() + 50, maxHealth);
                bee.setHealth(newHealth);

                // Finalizar la regeneración
                isRegenerating.put(bee.getUniqueId(), false);

                // Cancelar partículas y sonido
                particleTask.cancel(); // Detener el efecto visual y sonoro
            }
        }.runTaskLater(plugin, 100L); // 5 segundos
    }

    @EventHandler
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Bee bee) {
            // Verificamos si la abeja es la "Abeja Reina" usando PersistentDataContainer
            if (bee.getPersistentDataContainer().has(queenbeekey, PersistentDataType.BYTE)) {
                if (event.getDamager() instanceof Projectile) {
                    double originalDamage = event.getDamage();
                    double reducedDamage = originalDamage * 0.5; // 50% menos daño
                    event.setDamage(reducedDamage);
                }
            }
        }
    }



    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Bee bee && bee.getCustomName() != null && bee.getCustomName().equals(ChatColor.GOLD + "Abeja Reina")) {
            // Si la abeja no tiene un objetivo o perdió el objetivo, busca uno nuevo
            if (event.getTarget() == null || !(event.getTarget() instanceof Player)) {
                Player target = getRandomPlayer(bee); // Busca un jugador aleatorio como objetivo
                if (target != null) {
                    bee.setTarget(target);
                }
            }
        }
    }

    private Player getRandomPlayer(Bee bee) {
        // Obtiene todos los jugadores cercanos en un rango de 20 bloques
        List<Player> nearbyPlayers = Objects.requireNonNull(bee.getLocation()
                        .getWorld())
                .getNearbyEntities(bee.getLocation(), 20, 20, 20) // Rango de 20 bloques en todas las direcciones
                .stream()
                .filter(entity -> entity instanceof Player) // Filtra solo jugadores
                .map(entity -> (Player) entity) // Convierte a Player
                .collect(Collectors.toList());

        // Si no hay jugadores cerca, devuelve null
        if (nearbyPlayers.isEmpty()) {
            return null;
        }

        // Devuelve un jugador aleatorio de la lista
        return nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()));
    }

    private Player getClosestPlayer(Entity entity, double range) {
        double closestDistance = Double.MAX_VALUE;
        Player closestPlayer = null;

        for (Player player : entity.getWorld().getPlayers()) {
            double distance = entity.getLocation().distanceSquared(player.getLocation());
            if (distance <= range * range && distance < closestDistance) { // Comparar solo dentro del rango
                closestDistance = distance;
                closestPlayer = player;
            }
        }
        return closestPlayer;
    }

    @EventHandler
    public void onQueenBeeDeath(EntityDeathEvent event) {
        // Verifica si la entidad es un jugador
        if (event.getEntity() instanceof Player player) {
            String cause = deathCauseMap.get(player.getUniqueId());
            if (cause != null) {
                // Define los mensajes según la causa de la muerte
                String deathMessage = "";

                switch (cause) {
                    case "Abeja Reina":
                        deathMessage = player.getName() + " ha muerto por la " + ChatColor.YELLOW + ChatColor.BOLD + "Abeja Reina";
                        break;
                    case "Aguijón de Abeja Reina":
                        deathMessage = player.getName() + " ha muerto por el aguijón de la " + ChatColor.YELLOW + ChatColor.BOLD + "Abeja Reina";
                        break;
                    case "Aguijón Explosivos de Abeja Reina":
                        deathMessage = player.getName() + " ha muerto por el aguijón explosivo de la " + ChatColor.YELLOW + ChatColor.BOLD + "Abeja Reina";
                        break;
                }

                // Establece el mensaje de muerte personalizado
                event.getEntity().getServer().broadcastMessage(deathMessage);

                // Limpia la causa de muerte después de que haya sido procesada
                deathCauseMap.remove(player.getUniqueId());
            }
            return; // Evita procesar lógica de la abeja reina si es un jugador
        }

        // Verifica si la entidad es la Abeja Reina
        if (event.getEntity() instanceof Bee bee && bee.getCustomName() != null
                && bee.getCustomName().equals(ChatColor.GOLD + "Abeja Reina")) {

            // Drop personalizado al morir
            ItemStack sting = new ItemStack(Material.YELLOW_DYE);
            ItemMeta meta = sting.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.GOLD + "Aguijón de la Abeja Reina");
            meta.setCustomModelData(1);
            sting.setItemMeta(meta);
            bee.getWorld().dropItem(bee.getLocation(), sting);

            // Eliminar BossBar asociada
            UUID beeId = bee.getUniqueId();
            if (bossBars.containsKey(beeId)) {
                BossBar bossBar = bossBars.remove(beeId);
                if (bossBar != null) {
                    bossBar.removeAll();
                }
            }
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();

        // Obtiene solo las abejas en el mundo
        for (Bee bee : world.getEntitiesByClass(Bee.class)) {
            // Si la abeja tiene el nombre personalizado de "Abeja Reina"
            if (bee.getCustomName() != null && bee.getCustomName().equals(ChatColor.GOLD + "Abeja Reina")) {
                // Elimina el PersistentDataContainer
                bee.getPersistentDataContainer().remove(queenbeekey);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Verificar si el jugador realmente se movió (no solo giró la cámara)
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Location playerLocation = player.getLocation();
        double maxDistanceSquared = 30 * 30; // 30 bloques al cuadrado

        // Obtiene entidades cercanas y filtra solo arañas sin PersistentDataKey
        for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
            if (entity instanceof Bee bee &&
                    bee.getCustomName() != null &&
                    bee.getCustomName().equals(ChatColor.GOLD + "Abeja Reina") &&
                    !bee.getPersistentDataContainer().has(queenbeekey, PersistentDataType.BYTE)) {

                // Usa distanceSquared para evitar la raíz cuadrada
                if (playerLocation.distanceSquared(bee.getLocation()) <= maxDistanceSquared) {
                    transformToQueenBee(bee);
                }
            }
        }
    }

}
