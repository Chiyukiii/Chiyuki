/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.Option;
import net.kodehawa.mantarobot.options.OptionType;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static java.util.Map.Entry;
import static net.kodehawa.mantarobot.utils.Utils.mapObjects;

@Module
public class OptsCmd {
    public static Command optsCmd;

    public static void onHelp(GuildMessageReceivedEvent event) {
        event.getChannel().sendMessage("Hey, if you're lost, check <https://github.com/Mantaro/MantaroBot/wiki/Configuration> for a guide on how to use opts.").queue();
    }

    public static SimpleCommand getOpts() {
        return (SimpleCommand) optsCmd;
    }

    @Subscribe
    public void register(CommandRegistry registry) {
        registry.register("opts", optsCmd = new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(args.length == 1 && args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("ls")) {
                    StringBuilder builder = new StringBuilder();

                    for(String s : Option.getAvaliableOptions()) {
                        builder.append(s).append("\n");
                    }

                    List<String> m = DiscordUtils.divideString(builder);
                    List<String> messages = new LinkedList<>();
                    boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
                    for(String s1 : m) {
                        messages.add("**Mantaro's Options List**\n" + (hasReactionPerms ? "Use the arrow reactions to change pages. " :
                                "Use &page >> and &page << to change pages and &cancel to end") +
                                "*All options must be prefixed with `~>opts` when running them*\n" + String.format("```prolog\n%s```", s1));
                    }

                    if(hasReactionPerms) {
                        DiscordUtils.list(event, 45, false, messages);
                    } else {
                        DiscordUtils.listText(event, 45, false, messages);
                    }

                    return;
                }

                if(args.length < 2) {
                    event.getChannel().sendMessage(help(event)).queue();
                    return;
                }

                StringBuilder name = new StringBuilder();

                if(args[0].equalsIgnoreCase("help")) {
                    for(int i = 1; i < args.length; i++) {
                        String s = args[i];
                        if(name.length() > 0) name.append(":");
                        name.append(s);
                        Option option = Option.getOptionMap().get(name.toString());

                        if(option != null) {
                            try {
                                EmbedBuilder builder = new EmbedBuilder()
                                        .setAuthor(option.getOptionName(), null, event.getAuthor().getEffectiveAvatarUrl())
                                        .setDescription(option.getDescription())
                                        .setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
                                        .addField("Type", option.getType().toString(), false);
                                event.getChannel().sendMessage(builder.build()).queue();
                            } catch(IndexOutOfBoundsException ignored) {
                            }
                            return;
                        }
                    }
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid option help name.").queue(
                            message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
                    );

                    return;
                }

                for(int i = 0; i < args.length; i++) {
                    String s = args[i];
                    if(name.length() > 0) name.append(":");
                    name.append(s);
                    Option option = Option.getOptionMap().get(name.toString());

                    if(option != null) {
                        BiConsumer<GuildMessageReceivedEvent, String[]> callable = Option.getOptionMap().get(name.toString()).getEventConsumer();
                        try {
                            String[] a;
                            if(++i < args.length) a = Arrays.copyOfRange(args, i, args.length);
                            else a = new String[0];
                            callable.accept(event, a);
                            Player p = MantaroData.db().getPlayer(event.getAuthor());
                            if(p.getData().addBadgeIfAbsent(Badge.DID_THIS_WORK)) {
                                p.saveAsync();
                            }
                        } catch(IndexOutOfBoundsException ignored) {
                        }
                        return;
                    }
                }

                event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid option or arguments.").queue(
                        message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
                );
                event.getChannel().sendMessage(help(event)).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Options and Configurations Command")
                        .setDescription("**This command allows you to change Mantaro settings for this server.**\n" +
                                "All values set are local rather than global, meaning that they will only effect this server.")
                        .addField("Usage", "The command is so big that we moved the description to the wiki. [Click here](https://github.com/Mantaro/MantaroBot/wiki/Configuration) to go to the Wiki Article.", false)
                        .build();
            }
        }).addOption("check:data", new Option("Data check.",
                "Checks the data values you have set on this server. **THIS IS NOT USER-FRIENDLY**", OptionType.GENERAL)
                .setAction(event -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    //Map as follows: name, value
                    Map<String, Object> fieldMap = mapObjects(guildData);

                    if(fieldMap == null) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot retrieve values. Weird thing...").queue();
                        return;
                    }

                    StringBuilder show = new StringBuilder();
                    show.append("Options set for server **")
                            .append(event.getGuild().getName())
                            .append("**\n\n");

                    AtomicInteger ai = new AtomicInteger();

                    for(Entry e : fieldMap.entrySet()) {
                        if(e.getKey().equals("localPlayerExperience")) {
                            continue;
                        }

                        show.append(ai.incrementAndGet())
                                .append(".- `")
                                .append(e.getKey())
                                .append("`");

                        if(e.getValue() == null) {
                            show.append(" **is not set to anything.")
                                    .append("**\n");
                        } else {
                            show.append(" is set to: **")
                                    .append(e.getValue())
                                    .append("**\n");
                        }
                    }

                    List<String> toSend = DiscordUtils.divideString(1600, show);
                    toSend.forEach(message -> event.getChannel().sendMessage(message).queue());
                }).setShortDescription("Checks the data values you have set on this server.")
        ).addOption("reset:all", new Option("Options reset.",
                "Resets all options set on this server.", OptionType.GENERAL)
                .setAction(event -> {
                    //Temporary stuff.
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData temp = MantaroData.db().getGuild(event.getGuild()).getData();

                    //The persistent data we wish to maintain.
                    String premiumKey = temp.getPremiumKey();
                    long quoteLastId = temp.getQuoteLastId();
                    long ranPolls = temp.getQuoteLastId();
                    String gameTimeoutExpectedAt = temp.getGameTimeoutExpectedAt();
                    long cases = temp.getCases();

                    //Assign everything all over again
                    DBGuild newDbGuild = DBGuild.of(dbGuild.getId(), dbGuild.getPremiumUntil());
                    GuildData newTmp = newDbGuild.getData();
                    newTmp.setGameTimeoutExpectedAt(gameTimeoutExpectedAt);
                    newTmp.setRanPolls(ranPolls);
                    newTmp.setCases(cases);
                    newTmp.setPremiumKey(premiumKey);
                    newTmp.setQuoteLastId(quoteLastId);

                    //weee
                    newDbGuild.saveAsync();

                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Correctly reset your options!").queue();
                }));
    }
}
