package com.warfront.block;

import com.warfront.army.Formation;
import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.army.UnitNames;
import com.warfront.config.WFConfig;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.FactionData;
import com.warfront.faction.Factions;
import com.warfront.faction.Race;
import com.warfront.faction.Relation;
import com.warfront.mana.ManaNetwork;
import com.warfront.registry.WFRegistry;
import com.warfront.world.BaseLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The core of a summoning altar. The full altar is the core on a 3×3 floor of brick or stone-brick blocks, with a
 * Mana Brazier two blocks out on each diagonal. Summoning a unit draws its cost from the mana network in reach.
 */
public class SummoningAltarBlockEntity extends BlockEntity {
    /** Blocks that count as altar floor. */
    public static final TagKey<Block> ALTAR_BASE = TagKey.create(Registries.BLOCK, com.warfront.Warfront.id("altar_base"));

    @Nullable private UUID owner;

    public SummoningAltarBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.SUMMONING_ALTAR_BE.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public String factionKey(@Nullable MinecraftServer server) {
        if (owner == null) return Factions.WILD;
        return server == null ? "p:" + owner : FactionData.get(server).keyOf(owner);
    }

    /** What is missing from the altar, in words; empty when it is complete. */
    public static List<String> missing(Level level, BlockPos core) {
        List<String> out = new ArrayList<>();
        int floor = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getBlockState(core.offset(dx, -1, dz)).is(ALTAR_BASE)) floor++;
            }
        }
        if (floor < 9) out.add("a 3×3 floor of bricks or stone bricks under the altar (" + floor + "/9)");
        int braziers = 0;
        for (int dx = -2; dx <= 2; dx += 4) {
            for (int dz = -2; dz <= 2; dz += 4) {
                if (level.getBlockState(core.offset(dx, 0, dz)).is(WFRegistry.MANA_BRAZIER.get())) braziers++;
            }
        }
        if (braziers < 4) out.add("a Mana Brazier two blocks out on each diagonal corner (" + braziers + "/4)");
        return out;
    }

    public float availableMana() {
        return level == null ? 0F : ManaNetwork.available(level, worldPosition, factionKey(level.getServer()));
    }

    /** Whether this player may use the altar: its owner or an ally. An unclaimed altar is claimed by its first user. */
    public boolean mayUse(Player player) {
        if (owner == null) {
            setOwner(player.getUUID());
            return true;
        }
        MinecraftServer server = player.getServer();
        return Factions.relation(server, factionKey(server), Factions.keyOf(server, player)) == Relation.ALLY;
    }

    public static Race raceOf(Player player) {
        Race race = Race.byId(player.getData(WFRegistry.RACE));
        return race == null ? Race.HUMAN : race;
    }

    /** The outcome of a summon: a message for the player and whether a unit appeared. */
    public record Result(boolean ok, String message) {}

    public Result summon(Player player, SoldierRole role) {
        if (!(level instanceof ServerLevel server)) return new Result(false, "");
        if (!mayUse(player)) return new Result(false, "This altar belongs to another faction.");
        List<String> missing = missing(level, worldPosition);
        if (!missing.isEmpty()) return new Result(false, "The altar is incomplete. It needs " + String.join(" and ", missing) + ".");

        int max = WFConfig.MAX_ARMY_SIZE.get();
        int count = level.getEntitiesOfClass(SoldierEntity.class, new AABB(player.blockPosition()).inflate(256),
                s -> s.isAlive() && s.isOwnedBy(player)).size();
        if (count >= max) return new Result(false, "Your army is at full strength (" + max + ").");

        Race race = raceOf(player);
        int needed = BaseLevel.requiredLevel(role);
        if (role == SoldierRole.BEAST && !UnitNames.hasBeast(race)) return new Result(false, "Your race has no war beast yet.");
        if (role.retired()) return new Result(false, "Guards are no longer summoned: sneak + right-click any battle unit to put it on guard or patrol duty.");
        BaseLevel.Status base = needed > 1 || role == SoldierRole.BEAST
                ? BaseLevel.of(level, worldPosition, factionKey(server.getServer())) : new BaseLevel.Status(0, 0);
        int baseLevel = base.level();
        if (baseLevel < needed) {
            return new Result(false, "A " + UnitNames.of(race, role) + " needs a level " + needed + " base; this one is level "
                    + baseLevel + ". To get there, " + base.missingFor(needed) + ".");
        }
        if (role == SoldierRole.BEAST) {
            int beasts = level.getEntitiesOfClass(SoldierEntity.class, new AABB(player.blockPosition()).inflate(256),
                    s -> s.isAlive() && s.isOwnedBy(player) && s.getRole() == SoldierRole.BEAST).size();
            int cap = BaseLevel.beastCap(baseLevel);
            if (beasts >= cap) {
                String next = baseLevel < BaseLevel.MAX_LEVEL && cap < WFConfig.BEAST_LIMIT.get()
                        ? " To raise it, " + base.missingFor(baseLevel + 1) + "." : "";
                return new Result(false, "You already command " + beasts + " war beasts, the most you can field (" + cap
                        + ") from a level " + baseLevel + " base." + next);
            }
        }
        int cost = role.manaCost(race);
        String key = factionKey(server.getServer());
        if (!player.getAbilities().instabuild && !ManaNetwork.draw(level, worldPosition, key, cost)) {
            return new Result(false, "Not enough mana in reach: a " + UnitNames.of(race, role) + " costs " + cost
                    + ", the linked wells hold " + (int) availableMana() + ".");
        }

        SoldierEntity soldier = WFRegistry.SOLDIER.get().create(level);
        if (soldier == null) return new Result(false, "The summoning failed.");
        float yaw = player.getYRot() + 180F;
        soldier.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, yaw, 0F);
        soldier.setupAsRecruit(player, role, race);
        boolean posted = role.posted();
        if (posted) {
            level.addFreshEntity(soldier);   // join the level first so a builder can survey around it
            soldier.assignPost(soldier.position(), yaw);
            if (role == SoldierRole.FARMER) soldier.getWorkItems().addItem(new ItemStack(Items.WHEAT_SEEDS, 8));
        } else {
            Order order = Order.byOrdinal(player.getData(WFRegistry.ARMY_ORDER));
            Formation formation = Formation.byOrdinal(player.getData(WFRegistry.ARMY_FORMATION));
            if (order == Order.FOLLOW) soldier.command(order, formation, null, player.getYRot());
            else soldier.command(Order.HOLD, formation, soldier.position(), player.getYRot());
            level.addFreshEntity(soldier);
        }

        com.warfront.advisor.Advisor.complete(player, com.warfront.advisor.Advisor.Step.SUMMON);
        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                worldPosition.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.04);
        level.playSound(null, worldPosition, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 0.9F, 1.1F);
        return new Result(true, "A " + soldier.getUnitName() + " answers your summons. (" + (count + 1) + "/" + max
                + ", -" + cost + " mana)" + (posted ? " It works here; sneak + right-click it to move its post." : ""));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
