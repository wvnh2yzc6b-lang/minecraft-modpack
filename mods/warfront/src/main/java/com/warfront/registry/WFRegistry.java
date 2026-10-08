package com.warfront.registry;

import com.mojang.serialization.Codec;
import com.warfront.Warfront;
import com.warfront.block.*;
import com.warfront.entity.SoldierEntity;
import com.warfront.item.CommanderBatonItem;
import com.warfront.item.ManaCrystalItem;
import com.warfront.item.ManaShardItem;
import com.warfront.item.HealingStaffItem;
import com.warfront.item.WarHornItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;


public final class WFRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Warfront.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Warfront.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Warfront.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Warfront.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Warfront.MODID);
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Warfront.MODID);
    public static final DeferredHolder<net.minecraft.world.effect.MobEffect, com.warfront.combat.FrenzyEffect> FRENZY =
            MOB_EFFECTS.register("frenzy", () -> new com.warfront.combat.FrenzyEffect());
    public static final DeferredHolder<net.minecraft.world.effect.MobEffect, com.warfront.combat.OathOfStoneEffect> OATH_OF_STONE =
            MOB_EFFECTS.register("oath_of_stone", () -> new com.warfront.combat.OathOfStoneEffect());
    public static final DeferredHolder<net.minecraft.world.effect.MobEffect, com.warfront.upkeep.WellFedEffect> WELL_FED =
            MOB_EFFECTS.register("well_fed", () -> new com.warfront.upkeep.WellFedEffect());
    /** Trophy banner auras, one per faction (the Fallen Host's banner gives mana instead). */
    public static final java.util.Map<com.warfront.faction.NpcFaction,
            DeferredHolder<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffect>> TROPHY_AURAS = trophyAuras();

    private static java.util.Map<com.warfront.faction.NpcFaction, DeferredHolder<net.minecraft.world.effect.MobEffect,
            net.minecraft.world.effect.MobEffect>> trophyAuras() {
        var map = new java.util.EnumMap<com.warfront.faction.NpcFaction, DeferredHolder<net.minecraft.world.effect.MobEffect,
                net.minecraft.world.effect.MobEffect>>(com.warfront.faction.NpcFaction.class);
        for (var f : com.warfront.faction.NpcFaction.values()) {
            if (f == com.warfront.faction.NpcFaction.FALLEN_HOST) continue;
            map.put(f, MOB_EFFECTS.register("trophy_" + f.name().toLowerCase(java.util.Locale.ROOT),
                    () -> com.warfront.fortress.TrophyBanners.aura(f)));
        }
        return map;
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Warfront.MODID);

    // ---- player data ----
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> RACE = ATTACHMENTS.register("race",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> ARMY_ORDER = ATTACHMENTS.register(
            "army_order", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> ARMY_FORMATION = ATTACHMENTS.register(
            "army_formation", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> STARTER_KIT = ATTACHMENTS.register(
            "starter_kit", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    /** Test mode: unlocks the Test Panel (F8) and /wftest for this player. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> TEST_MODE = ATTACHMENTS.register(
            "test_mode", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());
    /** Test mode god mode: invulnerable with creative-style flight. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> GOD_MODE = ATTACHMENTS.register(
            "god_mode", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    /** The advisor's quest step (Advisor.Step ordinal), a counter for the current step, and his entity id. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> QUEST_STEP = ATTACHMENTS.register(
            "quest_step", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> QUEST_COUNT = ATTACHMENTS.register(
            "quest_count", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> ADVISOR = ATTACHMENTS.register(
            "advisor", () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).copyOnDeath().build());

    /** Campaign: war meter per NPC faction, and which warlords the player has beaten (a bit per faction). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.minecraft.nbt.CompoundTag>> WAR_METERS = ATTACHMENTS.register(
            "war_meters", () -> AttachmentType.builder(() -> new net.minecraft.nbt.CompoundTag())
                    .serialize(net.minecraft.nbt.CompoundTag.CODEC).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> WARLORDS_BEATEN = ATTACHMENTS.register(
            "warlords_beaten", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());
    /** All seven warlords beaten: the player carries the Warlord title. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> WARLORD_TITLE = ATTACHMENTS.register(
            "warlord_title", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    // ---- entities ----
    public static final DeferredHolder<EntityType<?>, EntityType<com.warfront.advisor.AdvisorEntity>> ADVISOR_ENTITY =
            ENTITIES.register("advisor", () -> EntityType.Builder.<com.warfront.advisor.AdvisorEntity>of(
                            com.warfront.advisor.AdvisorEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).build("advisor"));
    public static final DeferredHolder<EntityType<?>, EntityType<SoldierEntity>> SOLDIER = ENTITIES.register("soldier",
            () -> EntityType.Builder.<SoldierEntity>of(SoldierEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .build("soldier"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.warfront.merchant.MerchantEntity>> MERCHANT =
            ENTITIES.register("merchant", () -> EntityType.Builder.<com.warfront.merchant.MerchantEntity>of(
                            com.warfront.merchant.MerchantEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("merchant"));

    // ---- blocks ----
    public static final DeferredBlock<TowerBlock> ARROW_TOWER = BLOCKS.register("arrow_tower",
            () -> new TowerBlock(TowerType.ARROW, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<TowerBlock> ARCANE_SPIRE = BLOCKS.register("arcane_spire",
            () -> new TowerBlock(TowerType.ARCANE, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(s -> 7)));
    public static final DeferredBlock<TowerBlock> HEALING_SHRINE = BLOCKS.register("healing_shrine",
            () -> new TowerBlock(TowerType.HEALING, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(s -> 10)));
    public static final DeferredBlock<WarStandardBlock> WAR_STANDARD = BLOCKS.register("war_standard",
            () -> new WarStandardBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 1200.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<DropExperienceBlock> MANA_ORE = BLOCKS.register("mana_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F).requiresCorrectToolForDrops().lightLevel(s -> 4)));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_MANA_ORE = BLOCKS.register("deepslate_mana_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE).lightLevel(s -> 4)));
    public static final DeferredBlock<ManabloomBlock> MANABLOOM = BLOCKS.register("manabloom",
            () -> new ManabloomBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollission()
                    .randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)
                    .lightLevel(s -> s.getValue(CropBlock.AGE) >= CropBlock.MAX_AGE ? 7 : 2)));

    public static final DeferredBlock<ManaWellBlock> MANA_WELL = BLOCKS.register("mana_well",
            () -> new ManaWellBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(s -> s.getValue(ManaWellBlock.FILL) * 3)));
    public static final DeferredBlock<ManaPylonBlock> MANA_PYLON = BLOCKS.register("mana_pylon",
            () -> new ManaPylonBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion().lightLevel(s -> 6)));
    public static final DeferredBlock<ManaBrazierBlock> MANA_BRAZIER = BLOCKS.register("mana_brazier",
            () -> new ManaBrazierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.LANTERN).noOcclusion().lightLevel(s -> 12)));
    public static final DeferredBlock<SummoningAltarBlock> SUMMONING_ALTAR = BLOCKS.register("summoning_altar",
            () -> new SummoningAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(4.0F, 12.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion().lightLevel(s -> 9)));

    public static final DeferredBlock<com.warfront.upkeep.MessHallBlock> MESS_HALL = BLOCKS.register("mess_hall",
            () -> new com.warfront.upkeep.MessHallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.5F).sound(SoundType.WOOD)));

    public static final DeferredBlock<com.warfront.outpost.RaidChestBlock> RAID_CHEST = BLOCKS.register("raid_chest",
            () -> new com.warfront.outpost.RaidChestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(3.0F, 1200.0F).sound(SoundType.WOOD)));

    public static final DeferredBlock<com.warfront.fortress.FortressCoreBlock> FORTRESS_CORE = BLOCKS.register("fortress_core",
            () -> new com.warfront.fortress.FortressCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F).noLootTable().lightLevel(s -> 10)));
    public static final DeferredBlock<com.warfront.fortress.TrophyBannerBlock> TROPHY_BANNER = BLOCKS.register("trophy_banner",
            () -> new com.warfront.fortress.TrophyBannerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.0F, 1200.0F).sound(SoundType.WOOD).noOcclusion()));

    // Dwarf tunnels.
    public static final DeferredBlock<Block> RUNE_STONE = BLOCKS.register("rune_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(12.0F, 1500.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)));
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> RUNE_STONE_STAIRS = BLOCKS.register("rune_stone_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(RUNE_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(RUNE_STONE.get())));
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> RUNE_STONE_SLAB = BLOCKS.register("rune_stone_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.ofFullCopy(RUNE_STONE.get())));
    public static final DeferredBlock<net.minecraft.world.level.block.WallBlock> RUNE_STONE_WALL = BLOCKS.register("rune_stone_wall",
            () -> new net.minecraft.world.level.block.WallBlock(BlockBehaviour.Properties.ofFullCopy(RUNE_STONE.get()).forceSolidOn()));
    public static final DeferredBlock<com.warfront.tunnel.SpikeFloorBlock> SPIKE_FLOOR = BLOCKS.register("spike_floor",
            () -> new com.warfront.tunnel.SpikeFloorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3.0F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<com.warfront.tunnel.RuneMineBlock> RUNE_MINE = BLOCKS.register("rune_mine",
            () -> new com.warfront.tunnel.RuneMineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
                    .strength(2.0F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<com.warfront.tunnel.FlameVentBlock> FLAME_VENT = BLOCKS.register("flame_vent",
            () -> new com.warfront.tunnel.FlameVentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.NETHER_BRICKS).lightLevel(s -> 3)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.warfront.tunnel.TrapBlockEntity>> TRAP_BE =
            BLOCK_ENTITIES.register("trap", () -> BlockEntityType.Builder.of(com.warfront.tunnel.TrapBlockEntity::new,
                    SPIKE_FLOOR.get(), RUNE_MINE.get(), FLAME_VENT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TowerBlockEntity>> TOWER_BE =
            BLOCK_ENTITIES.register("tower", () -> BlockEntityType.Builder.of(TowerBlockEntity::new,
                    ARROW_TOWER.get(), ARCANE_SPIRE.get(), HEALING_SHRINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarStandardBlockEntity>> WAR_STANDARD_BE =
            BLOCK_ENTITIES.register("war_standard", () -> BlockEntityType.Builder.of(WarStandardBlockEntity::new,
                    WAR_STANDARD.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManaWellBlockEntity>> MANA_WELL_BE =
            BLOCK_ENTITIES.register("mana_well", () -> BlockEntityType.Builder.of(ManaWellBlockEntity::new,
                    MANA_WELL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManaPylonBlockEntity>> MANA_PYLON_BE =
            BLOCK_ENTITIES.register("mana_pylon", () -> BlockEntityType.Builder.of(ManaPylonBlockEntity::new,
                    MANA_PYLON.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SummoningAltarBlockEntity>> SUMMONING_ALTAR_BE =
            BLOCK_ENTITIES.register("summoning_altar", () -> BlockEntityType.Builder.of(SummoningAltarBlockEntity::new,
                    SUMMONING_ALTAR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.warfront.upkeep.MessHallBlockEntity>> MESS_HALL_BE =
            BLOCK_ENTITIES.register("mess_hall", () -> BlockEntityType.Builder.of(com.warfront.upkeep.MessHallBlockEntity::new,
                    MESS_HALL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.warfront.outpost.RaidChestBlockEntity>> RAID_CHEST_BE =
            BLOCK_ENTITIES.register("raid_chest", () -> BlockEntityType.Builder.of(com.warfront.outpost.RaidChestBlockEntity::new,
                    RAID_CHEST.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.warfront.fortress.FortressCoreBlockEntity>> FORTRESS_CORE_BE =
            BLOCK_ENTITIES.register("fortress_core", () -> BlockEntityType.Builder.of(com.warfront.fortress.FortressCoreBlockEntity::new,
                    FORTRESS_CORE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.warfront.fortress.TrophyBannerBlockEntity>> TROPHY_BANNER_BE =
            BLOCK_ENTITIES.register("trophy_banner", () -> BlockEntityType.Builder.of(com.warfront.fortress.TrophyBannerBlockEntity::new,
                    TROPHY_BANNER.get()).build(null));

    // ---- items ----
    public static final DeferredItem<BlockItem> ARROW_TOWER_ITEM = ITEMS.registerSimpleBlockItem(ARROW_TOWER);
    public static final DeferredItem<BlockItem> ARCANE_SPIRE_ITEM = ITEMS.registerSimpleBlockItem(ARCANE_SPIRE);
    public static final DeferredItem<BlockItem> HEALING_SHRINE_ITEM = ITEMS.registerSimpleBlockItem(HEALING_SHRINE);
    public static final DeferredItem<BlockItem> WAR_STANDARD_ITEM = ITEMS.registerSimpleBlockItem(WAR_STANDARD);
    public static final DeferredItem<BlockItem> MANA_WELL_ITEM = ITEMS.registerSimpleBlockItem(MANA_WELL);
    public static final DeferredItem<BlockItem> MANA_PYLON_ITEM = ITEMS.registerSimpleBlockItem(MANA_PYLON);
    public static final DeferredItem<BlockItem> MANA_BRAZIER_ITEM = ITEMS.registerSimpleBlockItem(MANA_BRAZIER);
    public static final DeferredItem<BlockItem> SUMMONING_ALTAR_ITEM = ITEMS.registerSimpleBlockItem(SUMMONING_ALTAR);
    public static final DeferredItem<BlockItem> MESS_HALL_ITEM = ITEMS.registerSimpleBlockItem(MESS_HALL);
    public static final DeferredItem<BlockItem> RUNE_STONE_ITEM = ITEMS.registerSimpleBlockItem(RUNE_STONE);
    public static final DeferredItem<BlockItem> RUNE_STONE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(RUNE_STONE_STAIRS);
    public static final DeferredItem<BlockItem> RUNE_STONE_SLAB_ITEM = ITEMS.registerSimpleBlockItem(RUNE_STONE_SLAB);
    public static final DeferredItem<BlockItem> RUNE_STONE_WALL_ITEM = ITEMS.registerSimpleBlockItem(RUNE_STONE_WALL);
    public static final DeferredItem<BlockItem> SPIKE_FLOOR_ITEM = ITEMS.registerSimpleBlockItem(SPIKE_FLOOR);
    public static final DeferredItem<BlockItem> RUNE_MINE_ITEM = ITEMS.registerSimpleBlockItem(RUNE_MINE);
    public static final DeferredItem<BlockItem> FLAME_VENT_ITEM = ITEMS.registerSimpleBlockItem(FLAME_VENT);
    public static final DeferredItem<com.warfront.tunnel.RuneDrillItem> RUNE_DRILL = ITEMS.register("rune_drill",
            () -> new com.warfront.tunnel.RuneDrillItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> TROPHY_BANNER_ITEM = ITEMS.register("trophy_banner",
            () -> new BlockItem(TROPHY_BANNER.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    // The warlords' gear.
    public static final DeferredItem<Item> SKULLSPLITTER = ITEMS.register("skullsplitter", com.warfront.fortress.WarlordGear.Skullsplitter::new);
    public static final DeferredItem<Item> LEGION_WARPLATE = ITEMS.register("legion_warplate", com.warfront.fortress.WarlordGear.LegionWarplate::new);
    public static final DeferredItem<Item> EMBERBRAND = ITEMS.register("emberbrand", com.warfront.fortress.WarlordGear.Emberbrand::new);
    public static final DeferredItem<Item> BROODFANG = ITEMS.register("broodfang", com.warfront.fortress.WarlordGear.Broodfang::new);
    public static final DeferredItem<Item> THORNBOW = ITEMS.register("thornbow", com.warfront.fortress.WarlordGear.Thornbow::new);
    public static final DeferredItem<Item> RUNEHAMMER = ITEMS.register("runehammer", com.warfront.fortress.WarlordGear.Runehammer::new);
    public static final DeferredItem<Item> FALLEN_HALO = ITEMS.register("fallen_halo", com.warfront.fortress.WarlordGear.FallenHalo::new);
    /** Dropped when the seventh warlord falls: the clue to the hidden warlord. */
    public static final DeferredItem<Item> SEAL_OF_SEVEN = ITEMS.register("seal_of_seven",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext ctx, java.util.List<Component> tooltip,
                                            net.minecraft.world.item.TooltipFlag flag) {
                    tooltip.add(Component.literal("Seven War Maps, fitted together. They point to a battlefield out of time.")
                            .withStyle(net.minecraft.ChatFormatting.GOLD));
                }
            });

    public static final DeferredItem<BlockItem> MANA_ORE_ITEM = ITEMS.registerSimpleBlockItem(MANA_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_MANA_ORE_ITEM = ITEMS.registerSimpleBlockItem(DEEPSLATE_MANA_ORE);
    public static final DeferredItem<ManaShardItem> MANA_SHARD = ITEMS.register("mana_shard",
            () -> new ManaShardItem(new Item.Properties()));
    public static final DeferredItem<ManaCrystalItem> MANA_CRYSTAL = ITEMS.register("mana_crystal",
            () -> new ManaCrystalItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<ItemNameBlockItem> MANABLOOM_SEEDS = ITEMS.register("manabloom_seeds",
            () -> new ItemNameBlockItem(MANABLOOM.get(), new Item.Properties()));

    public static final DeferredItem<CommanderBatonItem> COMMANDER_BATON = ITEMS.register("commander_baton",
            () -> new CommanderBatonItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<WarHornItem> WAR_HORN = ITEMS.register("war_horn",
            () -> new WarHornItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<HealingStaffItem> HEALING_STAFF = ITEMS.register("healing_staff",
            () -> new HealingStaffItem(new Item.Properties().durability(128)));
    public static final DeferredItem<Item> WAR_MARK = ITEMS.registerSimpleItem("war_mark");
    public static final DeferredItem<com.warfront.war.WarMapItem> WAR_MAP = ITEMS.register("war_map",
            () -> new com.warfront.war.WarMapItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<com.warfront.flight.ManaGliderItem> MANA_GLIDER = ITEMS.register("mana_glider",
            () -> new com.warfront.flight.ManaGliderItem(new Item.Properties().durability(320).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<com.warfront.flight.RocketPackItem> ROCKET_PACK = ITEMS.register("rocket_pack",
            () -> new com.warfront.flight.RocketPackItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<com.warfront.flight.AngelWingsItem> ANGEL_WINGS = ITEMS.register("angel_wings",
            () -> new com.warfront.flight.AngelWingsItem(new Item.Properties().durability(432).rarity(Rarity.RARE)));
    public static final DeferredItem<com.warfront.flight.WindCharmItem> WIND_CHARM = ITEMS.register("wind_charm",
            () -> new com.warfront.flight.WindCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** Carried by builders; a display tool with no use of its own. */
    public static final DeferredItem<Item> MASON_HAMMER = ITEMS.register("mason_hammer",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DeferredSpawnEggItem> SOLDIER_SPAWN_EGG = ITEMS.register("soldier_spawn_egg",
            () -> new DeferredSpawnEggItem(SOLDIER, 0x7A1F1F, 0x2B2B2B, new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.warfront"))
                    .icon(() -> new ItemStack(COMMANDER_BATON.get()))
                    .displayItems((params, out) -> ITEMS.getEntries().forEach(item -> out.accept(item.get())))
                    .build());

    private WFRegistry() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
        ATTACHMENTS.register(bus);
        MOB_EFFECTS.register(bus);
    }
}
