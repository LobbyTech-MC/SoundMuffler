package dev.j3fftw.soundmuffler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.Nonnull;

@EnableAsync
public class SoundMufflerListener extends PacketAdapter implements Listener, EnergyNetComponent {

    public SoundMufflerListener(Plugin plugin) {
        super(plugin, ListenerPriority.NORMAL,
            PacketType.Play.Server.NAMED_SOUND_EFFECT, PacketType.Play.Server.ENTITY_SOUND
        );
    }

    @Override
    @Async
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT
            || event.getPacketType() == PacketType.Play.Server.ENTITY_SOUND
        ) {
            Location loc;
            if (event.getPacket().getIntegers().getFields().size() < 3) {
            	return;
            }
            Integer a = event.getPacket().getIntegers().readSafely(0);
            Integer b = event.getPacket().getIntegers().readSafely(1);
            Integer c = event.getPacket().getIntegers().readSafely(2);
            if (a != null && b != null && c != null && event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                int x = a >> 3;
                int y = b >> 3;
                int z = c >> 3;
                loc = new Location(event.getPlayer().getWorld(), x, y, z);
            } else if (b != null && event.getPacketType() == PacketType.Play.Server.ENTITY_SOUND) {
                loc = event.getPlayer().getWorld().getEntities().stream()
                    .filter(e -> e.getEntityId() == b)
                    .map(Entity::getLocation)
                    .findAny().orElse(null);
            } else return;

            if (loc == null)
                return;

            final Block soundMuff = findSoundMuffler(loc);
            if (soundMuff != null
                && BlockStorage.getLocationInfo(soundMuff.getLocation(), "enabled") != null
                && BlockStorage.getLocationInfo(soundMuff.getLocation(), "enabled").equals("true")
                && getCharge(soundMuff.getLocation()) > 8
            ) {
                int volume = Integer.parseInt(BlockStorage.getLocationInfo(soundMuff.getLocation(), "volume"));
                if (volume == 0) {
                    event.setCancelled(true);
                } else {
                    event.getPacket().getFloat().write(0, (float) volume / 100.0f);
                }
            }
        }
    }

    @Async
    private Block findSoundMuffler(Location loc) {
        final int dis = SoundMufflerMachine.DISTANCE;
        for (int x = loc.getBlockX() - dis; x < loc.getBlockX() + dis; x++) {
            for (int y = loc.getBlockY() - dis; y < loc.getBlockY() + dis; y++) {
                for (int z = loc.getBlockZ() - dis; z < loc.getBlockZ() + dis; z++) {
                    if (!loc.getWorld().isChunkLoaded(x >> 4, z >> 4))
                        continue;
                    Block b = loc.getWorld().getBlockAt(x, y, z);
                    if (b.getType() == Material.WHITE_CONCRETE && BlockStorage.hasBlockInfo(b)) {
                        SlimefunItem item = BlockStorage.check(b);
                        if (item.getId().equals("SOUND_MUFFLER")) {
                            return b;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Async
    public void start() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Nonnull
    @Override
    @Async
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    @Async
    public int getCapacity() {
        return 352;
    }

    @Nonnull
    @Override
    @Async
    public String getId() {
        return "SOUND_MUFFLER";
    }
}
