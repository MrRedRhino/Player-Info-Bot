package org.pipeman.player_info_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import org.bukkit.Statistic;
import org.pipeman.player_info_bot.PlayerInformation;
import org.pipeman.player_info_bot.Utils;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

public class CommandPlayerinfo {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    public static void handle(SlashCommandInteractionEvent event) {
        OptionMapping playerOption = event.getOption("playername");
        String playerName = playerOption == null ? null : playerOption.getAsString();

        if (playerName == null || playerName.isEmpty()) return;
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(playerName);
        embedBuilder.setColor(new Color(59, 152, 0));

        event.deferReply().queue();
        Optional<PlayerInformation> information = PlayerInformation.of(playerName);

        embedBuilder.addField(
                "Last Seen",
                information.map(inf -> {
                    if (inf.online()) {
                        return "Currently online";
                    } else {
                        long lastSeen = inf.lastSeen();
                        int daysAgo = (int) Math.floorDiv(System.currentTimeMillis() - lastSeen, 86_400_000);
                        String time = DATE_FORMAT.format(new Date(lastSeen));
                        return "On " + time + " (" + daysAgo + " days ago)";
                    }
                }).orElse("Not found. Check spelling and use suggestions provided when typing the command."),
                false
        );

        information.ifPresent(inf -> {
            embedBuilder.addField(
                    "Playtime",
                    inf.playtime() / 3_600 + " hours",
                    false
            );
            embedBuilder.addField("Rank", Utils.ordinal(inf.rank()), false);

            inf.totalClaimBlocks().ifPresent(blocks ->
                    embedBuilder.addField(
                            "Total claimblocks",
                            blocks + " blocks",
                            false
                    )
            );

            Function<Stat<Identifier>, Integer> statisticSupplier = inf.statisticSupplier();
            embedBuilder.addField(
                    "Other statistics",
                    String.format("%.1f km walked, %d deaths, %d mobs killed",
                            statisticSupplier.apply(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM)) / 100_000d,
                            statisticSupplier.apply(Stats.CUSTOM.getOrCreateStat(Stats.DEATHS)),
                            statisticSupplier.apply(Stats.CUSTOM.getOrCreateStat(Stats.MOB_KILLS))
                    ),
                    false
            );
        });


        String filename = playerName.hashCode() + ".png";
        embedBuilder.setThumbnail("attachment://" + filename);
        event.getHook().sendFiles(FileUpload.fromData(Utils.getSkin(playerName), filename))
                .setEmbeds(embedBuilder.build())
                .queue();
    }
}
