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

package net.kodehawa.mantarobot.commands.action;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.NoArgsCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;

import java.awt.*;
import java.util.List;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;

public class TextActionCmd extends NoArgsCommand {
    private final Color color;
    private final String desc;
    private final String format;
    private final String name;
    private final List<String> strings;

    public TextActionCmd(String name, String desc, Color color, String format, List<String> strings) {
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.color = color;
        this.format = format;
        this.strings = strings;
    }

    @Override
    protected void call(GuildMessageReceivedEvent event, String content) {
        event.getChannel().sendMessage(String.format(format, random(strings))).queue();
    }

    @Override
    public MessageEmbed help(GuildMessageReceivedEvent event) {
        return helpEmbed(event, name)
                .setDescription(desc)
                .setColor(color)
                .build();
    }
}
