package com.itstwoam.aoeqol.misc;

import com.google.common.util.concurrent.Futures;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;

/**
 * "Save and Quit to Title" can freeze the game for good (singleplayer and LAN host). When the world closes, the
 * game hands the built-in server one last job (log out every player still connected) and waits for it without a
 * time limit. Your disconnect, sent a moment earlier, also makes the server shut down. If the server's shutdown
 * starts in the split second between the job being handed over and it being picked up, the server stops without
 * running it, and the game waits forever. (Minecraft's own rule for a job handed to a server that has already
 * stopped is to run it right away on the caller's side; the freeze is the moment in between.)
 *
 * Fix: the game still waits while the server is running. Once the server has fully stopped, the job is run right
 * here, exactly as Minecraft does when the timing goes the other way. By then every player has been logged out
 * and saved by the disconnect and the server's shutdown, so the job finds an empty list and finishes instantly.
 *
 * Replaces the Futures.getUnchecked call in IntegratedServer.initiateShutdown().
 */
public final class QuitFix {
    private QuitFix() {}

    public static Object await(Future<?> job) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return job.get(50, TimeUnit.MILLISECONDS);
                } catch (TimeoutException stillWaiting) {
                    MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                    if (server != null && !server.isServerStopped()) continue;
                    if (!job.isDone() && job instanceof Runnable) {
                        LogManager.getLogger("AoE-QoL").info("The built-in server stopped before its last logout job ran; "
                                + "running it here, as Minecraft does after the server has stopped (prevents the quit-to-title freeze)");
                        ((Runnable) job).run();
                    }
                    try {
                        return Futures.getUnchecked(job);
                    } catch (RuntimeException failed) {
                        LogManager.getLogger("AoE-QoL").warn("The built-in server's last logout job failed", failed);
                        return null;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;          // Futures.getUnchecked waits uninterruptibly too
                } catch (ExecutionException e) {
                    return Futures.getUnchecked(job); // rethrows the way the original call does
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
}
