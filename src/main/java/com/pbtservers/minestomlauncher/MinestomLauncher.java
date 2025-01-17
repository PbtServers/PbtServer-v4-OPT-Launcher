package com.pbtservers.minestomlauncher;

import com.pbtservers.minestomlauncher.util.InstanceGenUtil;
import com.pbtservers.minestomlauncher.util.ObjectTriple;
import com.pbtservers.minestomlauncher.command.ShutdownCommand;
import com.pbtservers.minestomlauncher.util.FileResult;
import com.pbtservers.minestomlauncher.util.FileUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.lan.OpenToLAN;
import net.minestom.server.extras.optifine.OptifineSupport;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Duration;

import static com.pbtservers.minestomlauncher.util.MessageUtil.*;
import static net.kyori.adventure.text.Component.text;

public final class MinestomLauncher {

    private static final ComponentLogger LOGGER = ComponentLogger.logger(MinestomLauncher.class);

    public static final String LAUNCHER_VERSION_NAME = "1.0.0";
    public static final String CONFIG_LOCATION = System.getProperty("config.location", "server.json");

    public MinestomLauncher() {}

    public void init() {
        LOGGER.info(LAUNCH_MESSAGE);

        loadConfiguration();
    }

    private void loadConfiguration() {
        long startMillis = System.currentTimeMillis();
        ObjectTriple<FileResult, MinestomData, Exception> triple = FileUtil.loadMinestomData(Path.of(CONFIG_LOCATION));

        switch(triple.left()) {
            case CREATED, EXISTING -> start(startMillis, triple.left(), triple.mid());
            case MALFORMED         -> shutdown();
        }
    }

    private void start(long startMillis, @NotNull FileResult result, @NotNull MinestomData minestomData) {
        MinestomData.Network networkData = minestomData.networkData();
        MinestomData.Proxy proxyData = minestomData.proxyData();
        MinestomData.Server serverData = minestomData.serverData();
        MinestomData.Instance instanceData = minestomData.instanceData();

        System.setProperty("minestom.tps", String.valueOf(serverData.ticksPerSecond()));
        System.setProperty("minestom.chunk-view-distance", String.valueOf(serverData.chunkViewDistance()));
        System.setProperty("minestom.entity-view-distance", String.valueOf(serverData.entityViewDistance()));

        MinecraftServer minecraftServer = MinecraftServer.init();

        if (networkData.openToLan()) {
            OpenToLAN.open();
        }

        if (serverData.optifineSupport()) {
            OptifineSupport.enable();
        }

        if (proxyData.enabled()) {
            String proxyType = proxyData.type();

            if (proxyType.equalsIgnoreCase("velocity")) {
                VelocityProxy.enable(proxyData.secret());
            } else if (proxyType.equalsIgnoreCase("bungeecord")) {
                BungeeCordProxy.enable();
            }
        } else if (serverData.onlineMode()) {
            MojangAuth.init();
        }

        if (serverData.benchmark()) {
            MinecraftServer.getBenchmarkManager().enable(Duration.of(10, TimeUnit.SECOND));
        }

        if (instanceData.enabled()) {
            String instanceTypeName = instanceData.typeName();
            InstanceManager instanceManager = MinecraftServer.getInstanceManager();
            InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

            InstanceGenUtil.loadInstance(instanceTypeName, instanceContainer);
            instanceManager.registerInstance(instanceContainer);

            MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent.class, event -> {
                Player player = event.getPlayer();

                event.setSpawningInstance(instanceContainer);
                player.setRespawnPoint(new Pos(0, 62, 0));
            });
        }

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new ShutdownCommand());
        //commandManager.register(new MinestomDefaultCommand());
        commandManager.setUnknownCommandCallback((sender, command) -> sender.sendMessage(text("Comando Invalido.", NamedTextColor.RED)));

        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> System.out.println("Launcher de Minestom Apagado Correctamente."));

        minecraftServer.start(networkData.ip(), networkData.port());

        LOGGER.info(configMessage(startMillis, result, networkData, serverData, proxyData));
    }

    private void shutdown() {
        LOGGER.info(malformedConfigMessage());

        try {
            Thread.sleep(5000);
        } catch (IllegalArgumentException | InterruptedException exc) {
            throw new RuntimeException(exc);
        }
    }

}