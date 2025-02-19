package com.zhuangxv.bot.injector.support;

import com.zhuangxv.bot.core.Bot;
import com.zhuangxv.bot.event.BaseEvent;
import com.zhuangxv.bot.event.message.GroupMessageEvent;
import com.zhuangxv.bot.event.message.GroupRecallEvent;
import com.zhuangxv.bot.event.message.PrivateMessageEvent;
import com.zhuangxv.bot.injector.ObjectInjector;
import com.zhuangxv.bot.message.CacheMessage;
import com.zhuangxv.bot.message.MessageChain;
import com.zhuangxv.bot.message.MessageTypeHandle;

import java.util.List;

public class MessageStringInjector implements ObjectInjector<String> {
    @Override
    public Class<String> getClassType() {
        return String.class;
    }

    @Override
    public String[] getType() {
        return new String[]{"message"};
    }

    @Override
    public String getObject(BaseEvent event, Bot bot) {
        MessageChain messageChain = null;
        if (event instanceof GroupMessageEvent) {
            messageChain = new MessageChain();
            GroupMessageEvent groupMessageEvent = (GroupMessageEvent) event;
            for (int i = 0; i < groupMessageEvent.getMessage().size(); i++) {
                messageChain.add(MessageTypeHandle.getMessage(groupMessageEvent.getMessage().getJSONObject(i)));
            }
        }
        if (event instanceof PrivateMessageEvent) {
            PrivateMessageEvent privateMessageEvent = (PrivateMessageEvent) event;
            messageChain = new MessageChain();
            for (int i = 0; i < privateMessageEvent.getMessage().size(); i++) {
                messageChain.add(MessageTypeHandle.getMessage(privateMessageEvent.getMessage().getJSONObject(i)));
            }
        }
        if (event instanceof GroupRecallEvent) {
            GroupRecallEvent groupRecallEvent = (GroupRecallEvent) event;
            List<CacheMessage> groupCacheMessageChain = bot.getGroupCacheMessageChain(groupRecallEvent.getGroupId(), groupRecallEvent.getMessageId(), 1);
            if (groupCacheMessageChain.isEmpty()) {
                return null;
            }
            messageChain = groupCacheMessageChain.get(0).getMessageChain();
        }
        return messageChain == null ? null : messageChain.toString();
    }
}
